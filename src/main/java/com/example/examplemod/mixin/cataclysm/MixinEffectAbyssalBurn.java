package com.example.examplemod.mixin.cataclysm;

import com.github.L_Ender.cataclysm.effects.EffectAbyssal_Burn;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EffectAbyssal_Burn.class)
public class MixinEffectAbyssalBurn {

    /**
     * 目标：修改 applyEffectTick 方法中的 hurt 调用
     * 原版逻辑：LivingEntityIn.hurt(source, 1.0F)
     * 修改逻辑：LivingEntityIn.hurt(source, 最大生命值 * 百分比)
     */
    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            remap = false // 因为目标类 EffectAbyssal_Burn 是第三方模组的类，通常不需要混淆映射，视你的开发环境而定
    )
    private boolean redirectHurt(LivingEntity entity, DamageSource source, float originalAmount) {
        // --- RPG 数值修改区域 ---
        
        // 设定比例：例如每次跳伤害造成最大生命值的 5% (0.05)
        // 这个Buff原本大约2秒跳一次 (40 ticks)，5% 意味着40秒左右能烧死满血玩家，比较合理
        float percentage = 0.05F; 
        
        // 计算新伤害：最大生命值 * 比例
        float newAmount = entity.getMaxHealth() * percentage;

        // 保底机制：如果算出来的伤害小于原版的 1.0F (针对血量极低的生物)，则维持 1.0F，防止完全不扣血
        // 如果你的包里所有生物血量都很高，可以去掉 Math.max
        float finalDamage = Math.max(newAmount, 1.0F);

        // 调用原本的 hurt 方法，但传入新的伤害值
        return entity.hurt(source, finalDamage);
    }
}