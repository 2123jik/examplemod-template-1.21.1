package com.example.examplemod.mixin.ironsspellbooks;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.BrewAlchemistCauldronRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(AlchemistCauldronTile.class)
public abstract class AlchemistCauldronMixin {
//    @Inject(method = "tryMeltInput", at = @At("HEAD"), cancellable = true)
//    private void injectFoodInfusion(ItemStack itemStack, CallbackInfo ci) {
//        // 0. 基础检查
//        if (itemStack.isEmpty() || ((AlchemistCauldronTile) (Object) this).getLevel() == null || ((AlchemistCauldronTile) (Object) this).getLevel().isClientSide) {
//            return;
//        }
//
//        // ================================================================
//        // 【兼容性检查】 核心修改：先检查是否会触发原模组的逻辑
//        // ================================================================
//
//        // 1. 检查是否是卷轴 (原模组硬编码逻辑)
//        // 这里的逻辑比较简单，我们假设如果是 scroll 物品就不处理
//        // (如果不确定具体的 ItemRegistry 类，可以通过 registry 检查 id 包含 scroll)
//        // 简单起见，如果原模组逻辑里 check 了 specific item，我们这里先跳过
//
//        // 2. 检查 JSON 配方 (BrewAlchemistCauldronRecipe)
//        // 我们遍历当前锅里的液体，看是否能和物品匹配出原模组的配方
//        for (FluidStack fluid : ((AlchemistCauldronTile)(Object)this).fluidInventory.fluids()) {
//            BrewAlchemistCauldronRecipe.Input input = new BrewAlchemistCauldronRecipe.Input(fluid, itemStack);
//            Optional<RecipeHolder<BrewAlchemistCauldronRecipe>> recipe = ((AlchemistCauldronTile) (Object) this).getLevel().getRecipeManager()
//                    .getRecipeFor((RecipeType<BrewAlchemistCauldronRecipe>) RecipeRegistry.ALCHEMIST_CAULDRON_BREW_TYPE.get(), input, ((AlchemistCauldronTile) (Object) this).getLevel());
//
//            if (recipe.isPresent()) {
//                // 存在官方配方（比如 Apple + Water -> Apple Juice），直接返回，不执行我们的逻辑
//                return;
//            }
//        }
//
//        // 3. 检查原版药水酿造 (PotionBrewing)
//        // 如果这个物品是萤石粉、红石、或者其他药水材料，它应该优先改变药水性质，而不是被注入
//        if (((AlchemistCauldronTile)(Object)this).isBrewable(itemStack)) {
//            // isBrewable 是原类的方法，判断是否是酿造材料
//            // 如果是，直接返回，让原类去处理“改变药水”的逻辑
//            return;
//        }
//
//        // ================================================================
//        // 【自定义逻辑】 只有上面都不匹配，且是食物时，才执行
//        // ================================================================
//
//        if (!itemStack.has(DataComponents.FOOD)) return;
//
//        for (FluidStack fluidStack : ((AlchemistCauldronTile)(Object)this).fluidInventory.fluids()) {
//            ItemStack potionGhostStack = PotionFluid.from(fluidStack);
//
//            if (potionGhostStack.has(DataComponents.POTION_CONTENTS)) {
//                int cost = 250;
//                if (fluidStack.getAmount() < cost) continue;
//
//                // 获取药水组件
//                PotionContents potionContents = potionGhostStack.get(DataComponents.POTION_CONTENTS);
//                // 如果药水没有效果（比如只是水瓶），跳过，防止把食物变成无效果的
//                if (!potionContents.hasEffects()) continue;
//
//                // 防止重复注魔 (可选)：如果食物已经包含了该药水的所有效果，就不再注魔
//                if (isAlreadyInfused(itemStack, potionContents)) continue;
//
//                FoodProperties newFoodProps = createInfusedFoodProps(itemStack.get(DataComponents.FOOD), potionContents);
//
//                if (newFoodProps != null) {
//                    itemStack.set(DataComponents.FOOD, newFoodProps);
//
//                    // 消耗液体
//                    FluidStack toDrain = fluidStack.copyWithAmount(cost);
//                    ((AlchemistCauldronTile)(Object)this).fluidInventory.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
//
//                    // 播放声音
//                    ((AlchemistCauldronTile) (Object) this).getLevel().playSound(null, ((AlchemistCauldronTile)(Object)this).getBlockPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.0f);
//
//                    ((AlchemistCauldronTile)(Object)this).setChanged();
//                    ci.cancel(); // 只有在成功注魔时才取消原方法
//                    return;
//                }
//            }
//        }
//    }
//
//    // 辅助：检查是否已经注魔过，避免死循环消耗药水
//    private boolean isAlreadyInfused(ItemStack food, PotionContents potion) {
//        FoodProperties foodProps = food.get(DataComponents.FOOD);
//        if (foodProps == null) return false;
//
//        // 简单检查：如果食物里的效果数量 < 药水效果数量，肯定没注魔完
//        // 严谨检查需要遍历对比
//        return false; // 简化处理：允许重复注魔（叠加效果）
//    }
//
//    private FoodProperties createInfusedFoodProps(FoodProperties oldFood, PotionContents potion) {
//        // (同之前的代码，创建新的 FoodProperties)
//        List<FoodProperties.PossibleEffect> newEffects = new ArrayList<>(oldFood.effects());
//        for (MobEffectInstance effect : potion.getAllEffects()) {
//            newEffects.add(new FoodProperties.PossibleEffect(() -> new MobEffectInstance(effect), 1.0F));
//        }
//        return new FoodProperties(
//                oldFood.nutrition(),
//                oldFood.saturation(),
//                oldFood.canAlwaysEat(),
//                oldFood.eatSeconds(),
//                oldFood.usingConvertsTo(),
//                newEffects
//        );
//    }
}