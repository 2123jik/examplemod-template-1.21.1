package com.example.examplemod.mixin.vanilla;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class HealOrHarmMobEffectMixin {
    @Shadow @Final private boolean isHarm;

    @Inject(
        method = "applyInstantenousEffect(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void applyHealingAttributeToInvertedInstant(@Nullable Entity source, @Nullable Entity indirectSource, LivingEntity livingEntity, int amplifier, double health, CallbackInfo ci) {
        if (livingEntity.isInvertedHealAndHarm()) {

            float healingFactor = (float) livingEntity.getAttributeValue(ALObjects.Attributes.HEALING_RECEIVED);

            if (this.isHarm) {
                float baseHeal = (float)((int)(health * (double)(4 << amplifier) + 0.5));
                float finalHeal = baseHeal * healingFactor;
                if (finalHeal > 0) {
                    livingEntity.heal(finalHeal);
                }
            } else {
                float baseDamage = (float)((int)(health * (double)(6 << amplifier) + 0.5));
                float finalDamage = baseDamage * healingFactor;
                if (finalDamage > 0) {
                    if (source == null) {
                        livingEntity.hurt(livingEntity.damageSources().magic(), finalDamage);
                    } else {
                        livingEntity.hurt(livingEntity.damageSources().indirectMagic(source, indirectSource), finalDamage);
                    }
                }
            }

            ci.cancel();
        }
    }
    @Inject(
        method = "applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void applyHealingAttributeToInvertedTick(LivingEntity livingEntity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if (livingEntity.isInvertedHealAndHarm()) {

            float healingFactor = (float) livingEntity.getAttributeValue(ALObjects.Attributes.HEALING_RECEIVED);

            if (this.isHarm) {
                float baseHeal = (float)Math.max(4 << amplifier, 0);
                float finalHeal = baseHeal * healingFactor;
                if (finalHeal > 0) {
                    livingEntity.heal(finalHeal);
                }
            } else {
                float baseDamage = (float)(6 << amplifier);
                float finalDamage = baseDamage * healingFactor;
                if (finalDamage > 0) {
                    livingEntity.hurt(livingEntity.damageSources().magic(), finalDamage);
                }
            }
            cir.cancel();
        }
    }
}