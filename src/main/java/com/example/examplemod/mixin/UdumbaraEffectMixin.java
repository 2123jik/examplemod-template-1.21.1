package com.example.examplemod.mixin;

import dev.xkmc.youkaishomecoming.content.effect.UdumbaraEffect;
import dev.xkmc.youkaishomecoming.init.data.YHModConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static dev.xkmc.youkaishomecoming.content.effect.UdumbaraEffect.hasLantern;

@Mixin(UdumbaraEffect.class)
public abstract
class UdumbaraEffectMixin
extends MobEffect {
    protected UdumbaraEffectMixin(MobEffectCategory category, int color) {
        super(category, color);
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity e, int lv) {
        if (e.getY() < (double)e.level().getMinBuildHeight()) {
            e.moveTo(e.getX(), (double)e.level().getMaxBuildHeight(), e.getZ());
            e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 6000));
        }

        if (e.tickCount % (Integer) YHModConfig.SERVER.udumbaraHealingPeriod.get() == 0 && e.level().isNight() && (e.level().canSeeSky(e.blockPosition().above()) || hasLantern(e))) {
            e.heal((float)((e.getMaxHealth()*0.02f) *(1<< lv)));
        }

        return true;
    }
}
