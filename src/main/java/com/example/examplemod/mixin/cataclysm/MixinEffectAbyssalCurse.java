package com.example.examplemod.mixin.cataclysm;

import com.github.L_Ender.cataclysm.effects.EffectAbyssal_Curse;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EffectAbyssal_Curse.class)
public class MixinEffectAbyssalCurse {

    /**
     * 目标：修改 EffectAbyssal_Curse.applyEffectTick 中的 hurt 方法调用
     * 原理：拦截原版的 1.0F 伤害，改为基于最大生命值的百分比伤害
     */
    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            remap = false // 目标类是第三方 Mod，通常不需要混淆映射
    )
    private boolean redirectCurseHurt(LivingEntity entity, DamageSource source, float originalAmount) {
        // --- RPG 数值修改配置 ---
        
        // 设定伤害百分比：0.05F 代表 5% 最大生命值
        // 频率：该效果默认约 2 秒跳一次。5% 意味着 40 秒左右致死（不考虑回血）
        float percentage = 0.05F;
        
        // 额外基础伤害：如果你希望在百分比之外还有个保底伤害（例如防止打不动低血量生物）
        float flatBase = 1.0F;

        // 计算公式
        float newAmount = (entity.getMaxHealth() * percentage) + flatBase;

        // 可选：设置伤害上限（防止对几百万血的 BOSS 造成过于夸张的单次伤害）
        // newAmount = Math.min(newAmount, 1000.0F);

        return entity.hurt(source, newAmount);
    }
}