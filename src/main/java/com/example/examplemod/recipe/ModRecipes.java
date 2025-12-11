package com.example.examplemod.recipe; // 确保包名正确

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 配方注册类 (ModRecipes)
 * 用于注册自定义的配方类型或序列化器。
 */
public class ModRecipes {
    // 创建一个 DeferredRegister（延迟注册器），用于注册 RecipeSerializer（配方序列化器）。
    // Registries.RECIPE_SERIALIZER: 指定注册表类型为配方序列化器。
    // ExampleMod.MODID: 指定模组 ID。
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ExampleMod.MODID);

    // 注册一个名为 "apply_scroll_bonus" 的配方序列化器。
    // 这意味着在 JSON 配方文件中，你可以使用 "type": "examplemod:apply_scroll_bonus"。
    //
    // 注意：这里传递的构造函数是 SmithingTransformRecipe.Serializer::new
    // 这表示该配方的结构和逻辑完全复用了原版“锻造台升级配方”（例如下界合金升级）。
    // 它通常包含 template(模板), base(底材), addition(添加物) 和 result(结果)。
    public static final DeferredHolder<RecipeSerializer<?>, SmithingTransformRecipe.Serializer> APPLY_SCROLL_BONUS_SERIALIZER =
            SERIALIZERS.register("apply_scroll_bonus", SmithingTransformRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<FoodPotionRecipe>> FOOD_POTION_SERIALIZER =
            SERIALIZERS.register("food_potion_crafting",
                    () -> new SimpleCraftingRecipeSerializer<>(FoodPotionRecipe::new));

    // 注册方法
    // 必须在主类 (ExampleMod.java) 的构造函数中调用此方法：ModRecipes.register(modEventBus);
    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}