package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// 使用 targets 指定包级私有的类
@Mixin(targets = "net.minecraft.world.effect.PoisonMobEffect")
public class MixinPoisonMobEffect {

    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean modifyPoisonDamage(LivingEntity entity, DamageSource source, float amount) {
        // RPG数值：最大生命值的 2%
        // 原版 Poison 的机制是生命值 > 1 才会致死，这里 hurt 依然会被原版的 if 包裹，所以不用担心致死问题
        float newAmount = entity.getMaxHealth() * 0.02F;

        // 建议：保留原版1.0F作为保底，防止打不动低血量生物
        return entity.hurt(source, Math.max(newAmount, 1.0F));
    }
}