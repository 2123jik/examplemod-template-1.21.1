package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net.minecraft.world.effect.RegenerationMobEffect")
public abstract class RegenerationMobEffectMixin extends MobEffect {

    protected RegenerationMobEffectMixin(MobEffectCategory p_296242_, int p_294288_) {
        super(p_296242_, p_294288_);
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity p_295924_, int p_296417_) {
        if (p_295924_.getHealth() < p_295924_.getMaxHealth()) {
            p_295924_.heal(p_295924_.getMaxHealth()*0.05f);
        }

        return true;
    }
}
