package com.example.examplemod.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class FoodPotionRecipe extends CustomRecipe {

    public FoodPotionRecipe(CraftingBookCategory category) {
        super(category);
    }

    // 判定配方是否匹配：网格中必须正好有一个带食物组件的物品和一个带药水内容的物品
    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasFood = false;
        boolean hasPotion = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            // 检查是否有食物组件
            if (stack.has(DataComponents.FOOD)) {
                if (hasFood) return false; // 只能有一个食物
                hasFood = true;
            }
            // 检查是否有药水内容组件 (且不是空瓶子)
            else if (stack.has(DataComponents.POTION_CONTENTS)) {
                if (hasPotion) return false; // 只能有一个药水
                hasPotion = true;
            } else {
                return false; // 不允许其他杂物
            }
        }

        return hasFood && hasPotion;
    }

    // 核心逻辑：组装物品
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack foodStack = ItemStack.EMPTY;
        ItemStack potionStack = ItemStack.EMPTY;

        // 1. 再次遍历找到对应的物品
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.has(DataComponents.FOOD)) {
                foodStack = stack;
            } else if (stack.has(DataComponents.POTION_CONTENTS)) {
                potionStack = stack;
            }
        }

        if (foodStack.isEmpty() || potionStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 2. 获取组件数据
        FoodProperties oldFoodProp = foodStack.get(DataComponents.FOOD);
        PotionContents potionContents = potionStack.get(DataComponents.POTION_CONTENTS);

        if (oldFoodProp == null || potionContents == null) return ItemStack.EMPTY;

        // 3. 构建新的效果列表
        // 注意：FoodProperties 是 record，不可变，所以要创建新的 List
        List<FoodProperties.PossibleEffect> newEffects = new ArrayList<>(oldFoodProp.effects());

        // 遍历药水中的所有效果 (包括原版药水效果和自定义NBT效果)
        for (MobEffectInstance effect : potionContents.getAllEffects()) {
            // 创建 PossibleEffect，概率设为 1.0 (100%)
            // 使用 Supplier 包装 effect 的副本
            newEffects.add(new FoodProperties.PossibleEffect(() -> new MobEffectInstance(effect), 1.0F));
        }
        
        // 4. 创建新的 FoodProperties
        // 依照源码中的 record 构造函数：nutrition, saturation, canAlwaysEat, eatSeconds, usingConvertsTo, effects
        FoodProperties newFoodProp = new FoodProperties(
                oldFoodProp.nutrition(),
                oldFoodProp.saturation(),
                oldFoodProp.canAlwaysEat(),
                oldFoodProp.eatSeconds(),
                oldFoodProp.usingConvertsTo(), // 保持原有的返回物品 (例如 迷之炖菜吃完给碗)
                newEffects
        );

        // 5. 复制食物并应用新组件
        ItemStack resultStack = foodStack.copy();
        resultStack.setCount(1); // 合成结果通常是1个
        resultStack.set(DataComponents.FOOD, newFoodProp);

        // 可选：你可能想改个名字，比如 "注魔的 [原名]"
        // resultStack.set(DataComponents.CUSTOM_NAME, Component.literal("注魔 ").append(resultStack.getHoverName()));

        return resultStack;
    }
    
    // (可选) 如果你希望合成后返还药水瓶，需要重写 getRemainingItems
    // 默认的合成逻辑会消耗所有物品。如果想保留玻璃瓶：
    @Override
    public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
         net.minecraft.core.NonNullList<ItemStack> remaining = net.minecraft.core.NonNullList.withSize(input.size(), ItemStack.EMPTY);
         for (int i = 0; i < remaining.size(); ++i) {
            ItemStack stack = input.getItem(i);
            if (stack.has(DataComponents.POTION_CONTENTS)) {
                // 如果是药水，返还玻璃瓶
//                remaining.set(i, new ItemStack(Items.GLASS_BOTTLE));
            } else {
                // 其他物品按原版逻辑（比如如果有容器物品则返还）
                remaining.set(i, stack.getCraftingRemainingItem());
            }
         }
         return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FOOD_POTION_SERIALIZER.get();
    }
}