package com.example.examplemod.mixin;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.mob_effect.BleedingEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static io.redspace.ironsspellbooks.damage.ISSDamageTypes.BLOOD_MAGIC;

@Mixin(BleedingEffect.class)
public class BleedingEffectMixin {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.level().damageSources().source(BLOOD_MAGIC, entity.getLastAttacker()), 1.0F + (float)amplifier);
        return true;
    }
}

