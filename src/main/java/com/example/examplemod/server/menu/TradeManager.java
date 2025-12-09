package com.example.examplemod.server.menu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager {
    // 记录请求：TargetUUID -> SourceUUID (谁被请求了 -> 谁发起的)
    // 实际项目中建议封装一个 Request 对象包含时间戳，以便清理过期请求
    private static final Map<UUID, UUID> activeRequests = new HashMap<>();

    public static void requestTrade(ServerPlayer source, ServerPlayer target) {
        UUID sourceId = source.getUUID();
        UUID targetId = target.getUUID();

        // 1. 检查是否对方已经请求过我了 (双向确认)
        if (activeRequests.containsKey(sourceId) && activeRequests.get(sourceId).equals(targetId)) {
            activeRequests.remove(sourceId); // 移除请求
            startTrade(source, target); // 开始交易
            return;
        }

        // 2. 发送请求
        activeRequests.put(targetId, sourceId);

        // 给发起者反馈
        source.displayClientMessage(Component.literal("已向 " + target.getName().getString() + " 发送交易请求..."), true);

        // 给目标发送可点击的消息
        Component acceptButton = Component.literal("[点击接受]")
                .setStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trade_accept_internal " + sourceId.toString())));

        target.sendSystemMessage(Component.literal(source.getName().getString() + " 请求与你交易 ")
                .append(acceptButton));
    }

    // 由命令调用
    public static void acceptTrade(ServerPlayer target, String sourceUUIDStr) {
        try {
            UUID sourceId = UUID.fromString(sourceUUIDStr);
            // 验证请求是否有效
            if (activeRequests.containsKey(target.getUUID()) && activeRequests.get(target.getUUID()).equals(sourceId)) {
                ServerPlayer source = target.server.getPlayerList().getPlayer(sourceId);
                if (source != null && source.isAlive()) {
                    activeRequests.remove(target.getUUID());
                    startTrade(source, target);
                } else {
                    target.sendSystemMessage(Component.literal("对方已离线。"));
                }
            } else {
                target.sendSystemMessage(Component.literal("请求已过期或不存在。"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startTrade(ServerPlayer host, ServerPlayer guest) {
        // 创建共享数据
        SimpleContainer sharedInv = new SimpleContainer(18);
        ContainerData sharedData = new SimpleContainerData(2);

        Runnable onComplete = () -> {
            // --- 修复：Inventory.add 逻辑 ---
            transferItems(sharedInv, 0, 9, guest); // Host 的物品给 Guest
            transferItems(sharedInv, 9, 18, host); // Guest 的物品给 Host

            host.closeContainer();
            guest.closeContainer();

            host.level().playSound(null, host.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        };

        // 打开 Host
        host.openMenu(new SimpleMenuProvider((id, inv, p) -> {
            TradeMenu menu = new TradeMenu(id, inv, sharedInv, sharedData, true);
            menu.completeCallback = onComplete;
            return menu;
        }, Component.literal("Trading with " + guest.getName().getString())), (buf) -> buf.writeBoolean(true));

        // 打开 Guest
        guest.openMenu(new SimpleMenuProvider((id, inv, p) -> {
            TradeMenu menu = new TradeMenu(id, inv, sharedInv, sharedData, false);
            menu.completeCallback = onComplete;
            return menu;
        }, Component.literal("Trading with " + host.getName().getString())), (buf) -> buf.writeBoolean(false));
    }

    // --- 修复：正确的物品给予逻辑 ---
    private static void transferItems(Container source, int start, int end, Player target) {
        for (int i = start; i < end; i++) {
            ItemStack stack = source.getItem(i);
            if (!stack.isEmpty()) {
                // copy 一份，因为 add() 会修改原 stack
                ItemStack toGive = stack.copy();

                // 尝试添加到背包
                // add() 会返回 boolean，但最重要的是它会修改 toGive 的 count
                if (target.getInventory().add(toGive)) {
                    // 如果成功添加了一部分或全部
                    // 此时 toGive.getCount() 会变少，或者变成 0
                }

                // 如果还有剩余 (toGive 没空)，说明背包满了
                if (!toGive.isEmpty()) {
                    target.drop(toGive, false); // 掉落在脚下
                }

                // 清空交易框
                source.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}