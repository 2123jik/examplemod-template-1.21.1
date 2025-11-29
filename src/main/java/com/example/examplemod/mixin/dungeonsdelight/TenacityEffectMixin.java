package com.example.examplemod.mixin.dungeonsdelight;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.yirmiri.dungeonsdelight.common.effect.TenacityEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.yirmiri.dungeonsdelight.common.effect.TenacityEffect.getInterval;

@Mixin(TenacityEffect.class)
public abstract class TenacityEffectMixin extends MobEffect {
    @Mutable
    @Shadow
    int applyInterval;
    protected TenacityEffectMixin(MobEffectCategory category, int color) {
        super(category, color);
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        if (!living.level().isClientSide && living instanceof Player player) {
            player.heal(1.0F);
            player.getFoodData().tick(player);
            applyInterval = getInterval(player);
        }

        return true;
    }
}
