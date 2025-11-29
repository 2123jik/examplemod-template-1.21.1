package com.example.examplemod.mixin.cataclysm;

import com.github.L_Ender.cataclysm.effects.EffectMonstrous;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EffectMonstrous.class)
public abstract class EffectMonstrousMixin extends MobEffect {
    protected EffectMonstrousMixin(MobEffectCategory category, int color) {
        super(category, color);

    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity LivingEntityIn, int amplifier) {
        if (LivingEntityIn.getHealth() < LivingEntityIn.getMaxHealth() / 2.0F) {
            LivingEntityIn.heal(LivingEntityIn.getHealth()*0.05f);
        }

        return true;
    }

}
