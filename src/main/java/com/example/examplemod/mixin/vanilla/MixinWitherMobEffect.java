package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.effect.WitherMobEffect")
public class MixinWitherMobEffect {

    @Redirect(
        method = "applyEffectTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    private boolean modifyWitherDamage(LivingEntity entity, DamageSource source, float amount) {
        // RPG数值：凋零通常比中毒更疼，设定为 3% 或更高
        float newAmount = entity.getMaxHealth() * 0.03F;
        
        return entity.hurt(source, Math.max(newAmount, 1.0F));
    }
}