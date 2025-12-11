package com.example.examplemod.server.events;

import com.example.examplemod.ExampleMod; // <--- 假设您的主类名
import com.example.examplemod.network.MobEffectStatusPacket;

import com.example.examplemod.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

// 替换 "examplemod" 为您的 Mod ID，bus 为 FORGE
@EventBusSubscriber(modid = "examplemod")
public class MobEffectSyncHandler {

    /**
     * 监听效果添加或更新。
     */
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide() || event.getEffectInstance() == null) return;

        // 获取 MobEffect 的注册 ID
        ResourceLocation effectId = event.getEffectInstance().getEffect().unwrapKey().get().location();

        // 【新增日志】确认事件触发和数据包发送
        ExampleMod.LOGGER.info("Server | MobEffect ADDED: {} to {} (UUID: {})",
                effectId, living.getName().getString(), living.getUUID());

        MobEffectStatusPacket packet = new MobEffectStatusPacket(
                living.getUUID(),
                effectId,
                MobEffectStatusPacket.Action.ADD
        );

        NetworkHandler.sendToTracking(packet, living);
    }

    /**
     * 监听效果移除。
     */
    @SubscribeEvent
    public static void onMobEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) return;

        // 获取被移除效果的 ID
        ResourceLocation effectId = event.getEffect().unwrapKey().get().location();

        // 只有当实体上**不再**拥有任何该 MobEffect 类型的实例时，才发送 REMOVE 信号。
        // 这确保了在效果被刷新或多个同类型效果并存时，图标不会错误地消失。
        if (!living.hasEffect(event.getEffect())) {

            // 【新增日志】确认事件触发和数据包发送
            ExampleMod.LOGGER.info("Server | MobEffect REMOVED: {} from {} (UUID: {}) - No other instances found.",
                    effectId, living.getName().getString(), living.getUUID());

            MobEffectStatusPacket packet = new MobEffectStatusPacket(
                    living.getUUID(),
                    effectId,
                    MobEffectStatusPacket.Action.REMOVE
            );

            NetworkHandler.sendToTracking(packet, living);
        } else {
            ExampleMod.LOGGER.debug("Server | MobEffect Remove Event Fired, but skipped REMOVE packet. Effect {} still exists.", effectId);
        }
    }

    // ⚠️ 注意：MobEffectEvent.Expired 也可能导致效果移除，如果需要完全同步，也应监听该事件
    @SubscribeEvent
    public static void onMobEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide() || event.getEffectInstance() == null) return;

        ResourceLocation effectId = event.getEffectInstance().getEffect().unwrapKey().get().location();

        // 再次检查，确保没有同类型的 MobEffectInstance 留存
        if (!living.hasEffect(event.getEffectInstance().getEffect())) {
            ExampleMod.LOGGER.info("Server | MobEffect EXPIRED: {} from {}...", effectId, living.getName().getString());
            MobEffectStatusPacket packet = new MobEffectStatusPacket(living.getUUID(), effectId, MobEffectStatusPacket.Action.REMOVE);
            NetworkHandler.sendToTracking(packet, living);
        }
    }
}