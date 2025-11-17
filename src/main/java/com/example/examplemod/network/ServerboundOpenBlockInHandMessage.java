package com.example.examplemod.network;

import fuzs.puzzleslib.api.network.v3.ServerMessageListener;
import fuzs.puzzleslib.api.network.v3.ServerboundMessage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record ServerboundOpenBlockInHandMessage(ResourceLocation blockId) implements ServerboundMessage<ServerboundOpenBlockInHandMessage> {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOpenBlockInHandMessage> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ServerboundOpenBlockInHandMessage::blockId,
            ServerboundOpenBlockInHandMessage::new
    );

    @Override
    public ServerMessageListener<ServerboundOpenBlockInHandMessage> getHandler() {
        return new ServerMessageListener<>() {
            @Override
            public void handle(ServerboundOpenBlockInHandMessage message, MinecraftServer server, ServerGamePacketListenerImpl handler, ServerPlayer player, ServerLevel level) {
                server.execute(() -> openContainerForPlayer(player, message.blockId()));
            }
        };
    }
    public static void openContainerForPlayer(ServerPlayer player, ResourceLocation blockId) {
        MenuProvider menuProvider = null;
        Item item = BuiltInRegistries.ITEM.get(blockId);

        if (item.equals(Items.CRAFTING_TABLE)) {
            menuProvider = new SimpleMenuProvider(
                    (id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())),
                    Component.translatable("container.crafting")
            );
            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        } else if (item.equals(Items.SMITHING_TABLE)) {
            menuProvider = new SimpleMenuProvider(
                    (id, inv, p) -> new SmithingMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())),
                    Component.translatable("container.upgrade")
            );
            player.awardStat(Stats.INTERACT_WITH_SMITHING_TABLE);
        } else if (item.equals(Items.STONECUTTER)) {
            menuProvider = new SimpleMenuProvider(
                    (id, inv, p) -> new StonecutterMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())),
                    Component.translatable("container.stonecutter")
            );
            player.awardStat(Stats.INTERACT_WITH_STONECUTTER);
        } else if (item.equals(Items.LOOM)) {
            menuProvider = new SimpleMenuProvider(
                    (id, inv, p) -> new LoomMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())),
                    Component.translatable("container.loom")
            );
            player.awardStat(Stats.INTERACT_WITH_LOOM);
        } else if (item.equals(Items.CARTOGRAPHY_TABLE)) {
            menuProvider = new SimpleMenuProvider(
                    (id, inv, p) -> new CartographyTableMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())),
                    Component.translatable("container.cartography_table")
            );
            player.awardStat(Stats.INTERACT_WITH_CARTOGRAPHY_TABLE);
        }
        // ▼▼▼ 移除了所有关于铁砧和熔炉的 else if 分支 ▼▼▼

        if (menuProvider != null) {
            player.openMenu(menuProvider);
        }
    }
}