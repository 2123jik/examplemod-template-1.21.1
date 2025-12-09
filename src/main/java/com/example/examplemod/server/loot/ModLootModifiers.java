package com.example.examplemod.server.loot; // 你的包名

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS;

public class ModLootModifiers {
    // 创建注册器
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(GLOBAL_LOOT_MODIFIER_SERIALIZERS, "examplemod"); // 把 examplemod 换成你的 modid

    // 注册你的 Modifier
    public static final Supplier<MapCodec<RandomizeAllFoodModifier>> BREAD_TO_RANDOM =
            LOOT_MODIFIER_SERIALIZERS.register("bread_to_random", () -> RandomizeAllFoodModifier.CODEC);

    public static final Supplier<MapCodec<ReplaceItemWithTagModifier>> REPLACE_ITEM_WITH_TAG =
            LOOT_MODIFIER_SERIALIZERS.register("replace_item_with_tag", () -> ReplaceItemWithTagModifier.CODEC);
    // 在主类构造函数中调用此方法
    public static void register(IEventBus bus) {
        LOOT_MODIFIER_SERIALIZERS.register(bus);
    }
}