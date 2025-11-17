package com.example.examplemod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
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
}