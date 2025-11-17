package com.example.examplemod.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vectorwing.farmersdelight.common.effect.ComfortEffect;

@Mixin(ComfortEffect.class)
public abstract class ComfortEffectMixin extends MobEffect {
    protected ComfortEffectMixin(MobEffectCategory category, int color) {
        super(category, color);
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.hasEffect(MobEffects.REGENERATION)) {
            return true;
        } else {
            if (entity instanceof Player) {
                Player player = (Player)entity;
                if ((double)player.getFoodData().getSaturationLevel() > (double)0.0F) {
                    return true;
                }
            }

            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(entity.getMaxHealth()*0.05f);
            }

            return true;
        }
    }
}
