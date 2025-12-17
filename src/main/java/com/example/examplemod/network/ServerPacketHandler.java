package com.example.examplemod.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ServerPacketHandler {


    // 处理打开方块界面请求 (彻底重写，移除了 PuzzlesLib 依赖)
    public static void handleOpenBlockInHand(OpenBlockInHandPayload payload, ServerPlayer player) {
        Item item = BuiltInRegistries.ITEM.get(payload.blockId());
        MenuProvider menuProvider = null;

        if (item == Items.CRAFTING_TABLE) {
            menuProvider = new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())), Component.translatable("container.crafting"));
            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        } else if (item == Items.SMITHING_TABLE) {
            menuProvider = new SimpleMenuProvider((id, inv, p) -> new SmithingMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())), Component.translatable("container.upgrade"));
            player.awardStat(Stats.INTERACT_WITH_SMITHING_TABLE);
        } else if (item == Items.STONECUTTER) {
            menuProvider = new SimpleMenuProvider((id, inv, p) -> new StonecutterMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())), Component.translatable("container.stonecutter"));
            player.awardStat(Stats.INTERACT_WITH_STONECUTTER);
        }

        if (menuProvider != null) {
            player.openMenu(menuProvider);
        }
    }
}