package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.ClientDamageHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
@EventBusSubscriber(modid = ExampleMod.MODID)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        // 使用你的 MODID
        final PayloadRegistrar registrar = event.registrar(ExampleMod.MODID);
        registrar.playToClient(
                DamageIndicatorPayload.TYPE,
                DamageIndicatorPayload.STREAM_CODEC,
                (payload, context) -> {
                    // 客户端处理逻辑
                    context.enqueueWork(() -> ClientDamageHandler.handlePacket(
                            payload.amount(),
                            payload.critType(),
                            payload.layers()
                    ));
                }
        );
        // 注册从客户端发往服务端的包 (Serverbound)
        registrar.playToServer(
                UpdateItemAttributesPayload.TYPE,
                UpdateItemAttributesPayload.STREAM_CODEC,
                NetworkHandler::handleUpdateAttributes
        );
        registrar.playBidirectional(
                TradeActionPayload.TYPE,
                TradeActionPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        TradeActionPayload::handleServer, // 客户端发给服务端
                        (payload, context) -> {}          // 服务端发给客户端(如果有的话，这里不需要)
                )
        );
    }

    /**
     * 发送数据包的方法
     * 必须传入 3 个参数以匹配 DamageIndicatorPayload 的 Record 定义
     *
     * @param player 目标玩家
     * @param amount 伤害数值
     * @param critType 暴击类型 (0:普通, 1:跳劈, 2:L2, 3:神化)
     * @param layers 神化暴击层数 (其他类型传 0)
     */
    public static void sendToPlayer(ServerPlayer player, float amount, int critType, int layers) {
        // 关键修复：这里的 new DamageIndicatorPayload 必须填满 3 个参数
        PacketDistributor.sendToPlayer(player, new DamageIndicatorPayload(amount, critType, layers));
    }

    // 可选：为了方便起见，可以保留一个旧版本的重载方法（默认 layers = 0）
    // 这样你就不用改其他地方调用普通暴击的代码了
    public static void sendToPlayer(ServerPlayer player, float amount, int critType) {
        sendToPlayer(player, amount, critType, 0);
    }


    // 服务端处理逻辑
    private static void handleUpdateAttributes(UpdateItemAttributesPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            // 此时我们在主线程 (Server Thread)
            if (context.player() instanceof ServerPlayer player) {
                // 安全检查 1: 玩家是否在创造模式？(防止作弊)
                if (!player.isCreative()) {
                    // 如果你想允许生存模式改，去掉这个检查
                    return;
                }

                // 安全检查 2: 获取主手物品
                ItemStack stack = player.getMainHandItem();
                if (stack.isEmpty()) return;

                // 核心逻辑: 修改 NBT
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, payload.modifiers());

                // 可选: 播放个音效或者是给个反馈
                // player.displayClientMessage(Component.literal("属性已更新!"), true);
            }
        });
    }
}