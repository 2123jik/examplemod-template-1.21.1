package com.example.examplemod.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net.minecraft.world.effect.AbsorptionMobEffect")
public abstract class AbsorptionMobEffectMixin extends MobEffect {

    protected AbsorptionMobEffectMixin(MobEffectCategory p_296242_, int p_294288_) {
        super(p_296242_, p_294288_);
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void onEffectStarted(LivingEntity p_294820_, int p_295222_) {
        super.onEffectStarted(p_294820_, p_295222_);
        p_294820_.setAbsorptionAmount(Math.max(p_294820_.getAbsorptionAmount(), (p_294820_.getMaxHealth()*0.25F * (1 + p_295222_))));
    }
}
