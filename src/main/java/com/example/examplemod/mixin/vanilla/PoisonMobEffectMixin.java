package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets ="net.minecraft.world.effect.PoisonMobEffect")
public abstract class PoisonMobEffectMixin extends MobEffect {
    protected PoisonMobEffectMixin(MobEffectCategory category, int color) {
        super(category, color);
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity p_296276_, int p_296233_) {
        if (p_296276_.getHealth() > 1.0F) {
            // Neo: Replace DamageSources#magic() with neoforge:poison to allow differentiating poison damage.
            // Fallback to minecraft:magic in client code when connecting to a vanilla server.
            // LivingEntity#hurt(DamageSource) will no-op in client code immediately, but the holder is resolved before the no-op.
            var dTypeReg = p_296276_.damageSources().damageTypes;
            var dType = dTypeReg.getHolder(net.neoforged.neoforge.common.NeoForgeMod.POISON_DAMAGE).orElse(dTypeReg.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC));
            p_296276_.hurt(new net.minecraft.world.damagesource.DamageSource(dType),p_296276_.getMaxHealth()*0.02F );
        }

        return true;
    }
}
