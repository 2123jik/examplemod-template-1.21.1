package com.example.examplemod.server.events;

import dev.shadowsoffire.placebo.events.AnvilLandEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.bobmowzie.mowziesmobs.server.item.ItemHandler.WROUGHT_AXE;
import static com.bobmowzie.mowziesmobs.server.item.ItemHandler.WROUGHT_HELMET;
import static com.example.examplemod.ExampleMod.MODID;
import static com.example.examplemod.component.ModDataComponents.MAKEN_ARMOR;
import static com.example.examplemod.component.ModDataComponents.MAKEN_SWORD;
import static twilightforest.init.TFItems.GIANT_SWORD;

@EventBusSubscriber(modid = MODID)
public class WorldEventHandler {

    @SubscribeEvent
    public static void onAnvilLand(AnvilLandEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockPos pos = event.getPos();
        List<ItemEntity> availableItems = new ArrayList<>(event.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0)));

        boolean crafted;
        do {
            crafted = false;
            if (tryVectorCraft(availableItems, item -> item.is(WROUGHT_AXE.get()), item -> item.has(MAKEN_SWORD.get()))) {
                crafted = true;
            } else if (tryVectorCraft(availableItems, item -> item.is(WROUGHT_HELMET.get()), item -> item.has(MAKEN_ARMOR.get()))) {
                crafted = true;
            }
        } while (crafted);
    }

    private static boolean tryVectorCraft(List<ItemEntity> availableItems, Predicate<ItemStack> toolMatcher, Predicate<ItemStack> targetMatcher) {
        Optional<ItemEntity> toolEntityOpt = availableItems.stream().filter(e -> toolMatcher.test(e.getItem())).findFirst();
        Optional<ItemEntity> targetEntityOpt = availableItems.stream().filter(e -> targetMatcher.test(e.getItem())).findFirst();

        if (toolEntityOpt.isPresent() && targetEntityOpt.isPresent()) {
            ItemEntity toolEntity = toolEntityOpt.get();
            ItemEntity targetEntity = targetEntityOpt.get();

            targetEntity.getItem().set(DataComponents.UNBREAKABLE, new Unbreakable(true));
            toolEntity.getItem().shrink(1);
            if (toolEntity.getItem().isEmpty()) {
                toolEntity.discard();
            }

            availableItems.remove(toolEntity);
            availableItems.remove(targetEntity);
            return true;
        }
        return false;
    }
}