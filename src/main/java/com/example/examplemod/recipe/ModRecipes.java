package com.example.examplemod.recipe; // 确保包名正确

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ExampleMod.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SmithingTransformRecipe.Serializer> APPLY_SCROLL_BONUS_SERIALIZER =
            SERIALIZERS.register("apply_scroll_bonus", SmithingTransformRecipe.Serializer::new);


    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}