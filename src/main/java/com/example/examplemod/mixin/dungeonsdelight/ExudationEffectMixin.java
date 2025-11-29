package com.example.examplemod.mixin.dungeonsdelight;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.yirmiri.dungeonsdelight.common.effect.ExudationEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ExudationEffect.class)
public class ExudationEffectMixin extends MobEffect {
    protected ExudationEffectMixin(MobEffectCategory category, int color) {
        super(category, color);
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void onEffectStarted(LivingEntity living, int amplifier) {
        living.setAbsorptionAmount(Math.max(living.getAbsorptionAmount(), (float)(4 * (1 + amplifier))/20f*living.getMaxHealth()
                )
        );
    }
}