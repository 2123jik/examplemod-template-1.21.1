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
            ItemStack stack = input.getItem(i); // 这是合成栏里的原始对象
            if (stack.isEmpty()) continue;

            if (stack.has(DataComponents.FOOD)) {
                // ================== 调试引用关键点 ==================
                int originalId = System.identityHashCode(stack);
                System.out.println(">>> 发现食物 (Slot " + i + ")");
                System.out.println("    [1] 原始物品内存ID: " + originalId);

                foodStack = stack.copy(); // 执行复制

                int copyId = System.identityHashCode(foodStack);
                System.out.println("    [2] 复制副本内存ID: " + copyId);

                // 核心验证：如果是 false，说明安全；如果是 true，说明你会改到原来的物品
                System.out.println("    [3] 是否为同一个对象 (危险检查): " + (stack == foodStack));

                if (stack == foodStack) {
                    System.err.println("!!! 严重警告：你正在直接使用原始物品引用，会导致刷物品BUG !!!");
                }
                // ===================================================

            } else if (stack.has(DataComponents.POTION_CONTENTS)) {
                potionStack = stack.copy();
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
        List<FoodProperties.PossibleEffect> newEffects = new ArrayList<>(oldFoodProp.effects());

        for (MobEffectInstance effect : potionContents.getAllEffects()) {
            newEffects.add(new FoodProperties.PossibleEffect(()-> new MobEffectInstance(effect),1.0f));
        }

        // 4. 创建新的 FoodProperties
        FoodProperties newFoodProp = new FoodProperties(
                oldFoodProp.nutrition(),
                oldFoodProp.saturation(),
                oldFoodProp.canAlwaysEat(),
                oldFoodProp.eatSeconds(),
                oldFoodProp.usingConvertsTo(),
                newEffects
        );

        // 5. 复制食物并应用新组件
        // 这里你原本写了第二次 copy
        ItemStack resultStack = foodStack.copy();

        // ================== 调试结果对象 ==================
        int resultId = System.identityHashCode(resultStack);
        System.out.println(">>> 生成结果");
        System.out.println("    [4] 最终结果内存ID: " + resultId);
        System.out.println("    [5] 结果与中间变量是否相同: " + (resultStack == foodStack));
        // ===============================================

        resultStack.setCount(1);
        resultStack.set(DataComponents.FOOD, newFoodProp);

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