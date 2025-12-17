package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin to modify the FoodData class for dynamic natural regeneration.
 * This makes the healing amount scale with the player's max health.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    // 定义回血量为最大生命值的百分比。这里是5%，和原版 (1.0 / 20.0) 保持一致。
    // 你可以根据需要调整这个值。
    private static final float HEAL_PERCENTAGE_OF_MAX_HEALTH = 0.05F;

    /**
     * 重定向基于饱和度的快速治疗调用 (foodLevel >= 20)。
     * 原版代码: player.heal(f / 6.0F)，其中 f 最大为 6.0F，所以最多治疗 1.0F。
     * 我们将其修改为按比例缩放。
     * @param player 玩家实例
     * @param originalAmount 原始计算出的治疗量 (f / 6.0F)
     */
    @Redirect(
        method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;heal(F)V",
            ordinal = 0 // 第一个 heal() 调用
        )
    )
    private void redirectSaturationHeal(Player player, float originalAmount) {
        // 计算基于最大生命值的等效治疗量
        // originalAmount 是一个 0 到 1 的系数，我们用它乘以新的基础治疗量
        float scaledHealAmount = (player.getMaxHealth() * HEAL_PERCENTAGE_OF_MAX_HEALTH) * originalAmount;

        // 调用原始的 heal 方法，传入我们计算出的新值。
        // 这将正确触发治疗事件，让“活力迸发”等效果能够生效。
        player.heal(scaledHealAmount);
    }

    @Redirect(
        method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;heal(F)V",
            ordinal = 1 // 第二个 heal() 调用
        )
    )
    private void redirectNaturalRegenHeal(Player player, float originalAmount) {
        // 计算基于玩家最大生命值的治疗量
        float scaledHealAmount = player.getMaxHealth() * HEAL_PERCENTAGE_OF_MAX_HEALTH;

        // 调用原始的 heal 方法，传入我们计算出的新值。
        // 同样，这将确保“活力迸发”等效果能正确应用。
        player.heal(scaledHealAmount);
    }

    // 这里定义默认的新上限，你可以将其连接到你的 Config 文件
    @Unique
    private int maxFoodLevel = 40;

    /**
     * 1. 修改 add 方法中的 clamp 上限
     * 原代码: this.foodLevel = Mth.clamp(foodLevel + this.foodLevel, 0, 20);
     */
    @ModifyConstant(method = "add", constant = @Constant(intValue = 20))
    private int modifyMaxFoodInAdd(int constant) {
        return this.maxFoodLevel;
    }

    /**
     * 2. 修改 needsFood 方法的判断条件
     * 原代码: return this.foodLevel < 20;
     */
    @ModifyConstant(method = "needsFood", constant = @Constant(intValue = 20))
    private int modifyMaxFoodInNeedsFood(int constant) {
        return this.maxFoodLevel;
    }

    /**
     * 3. 修改 tick 方法中的饱和度回血判断
     * 原代码: if (flag && this.saturationLevel > 0.0F && player.isHurt() && this.foodLevel >= 20)
     * 只有当饥饿值达到满值时，饱和度才会快速回血。我们将其修改为新的最大值。
     */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
    private int modifyMaxFoodInTick(int constant) {
        return this.maxFoodLevel;
    }

    /**
     * 可选：修改普通回血的阈值 (原版是 >= 18)
     * 如果你希望玩家只有在"接近满食"时才回血，应该保留 18 或修改为 Max - 2。
     * 如果你希望玩家在"半饱"状态也能回血，可以不修改。
     *
     * 这里演示如何保持"满值-2"的逻辑（例如上限40，则38以上才回血）。
     */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 18))
    private int modifyRegenThreshold(int constant) {
        // 保持原版逻辑：最大值 - 2
        return this.maxFoodLevel-4;
    }
}