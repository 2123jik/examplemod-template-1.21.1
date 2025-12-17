package com.example.examplemod.server.events;

import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.example.examplemod.ExampleMod.MODID;
import static dev.xkmc.l2hostility.init.registrate.LHItems.HOSTILITY_ORB;

@EventBusSubscriber(modid = MODID)
public class DataRegistryHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        final Consumer<DataComponentPatch.Builder> SET_MAX_STACK_64 = builder -> builder.set(DataComponents.MAX_STACK_SIZE, 64);

        event.modifyMatching(item -> item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock, SET_MAX_STACK_64);

        List<Class<? extends Item>> stackableTypes = List.of(
                SaddleItem.class, MinecartItem.class, BoatItem.class, SignItem.class, HangingSignItem.class,
                BucketItem.class, SnowballItem.class, SolidBucketItem.class, MilkBucketItem.class, MobBucketItem.class,
                EggItem.class, BundleItem.class, SpyglassItem.class, BedItem.class, EnderpearlItem.class,
                PotionItem.class, SplashPotionItem.class, LingeringPotionItem.class, EnchantedBookItem.class,
                ArmorStandItem.class, BannerItem.class, BannerPatternItem.class, InstrumentItem.class, HoneyBottleItem.class
        );
        stackableTypes.forEach(type -> event.modifyMatching(type::isInstance, SET_MAX_STACK_64));

        Set<Item> specificItems = Set.of(Items.WRITABLE_BOOK, Items.MUSHROOM_STEW, Items.CAKE, Items.RABBIT_STEW, Items.BEETROOT_SOUP, Items.KNOWLEDGE_BOOK);
        specificItems.forEach(item -> event.modifyMatching(item::equals, SET_MAX_STACK_64));

        event.modifyMatching(item -> item.getDefaultInstance().has(DataComponents.JUKEBOX_PLAYABLE) && item.getDefaultInstance().getRarity() == Rarity.RARE, SET_MAX_STACK_64);


    }
}