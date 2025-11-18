package com.example.examplemod.mixin;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.mob_effect.DetonationEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static io.redspace.ironsspellbooks.damage.ISSDamageTypes.BLOOD_MAGIC;

@Mixin(DetonationEffect.class)
public class DetonationEffectMixin {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity entity, int amp) {
        int ticks = entity.getRemainingFireTicks();
        if (ticks > 0) {
            entity.setRemainingFireTicks(0);
            entity.hurt(entity.level().damageSources().source(BLOOD_MAGIC), (float)((1 + amp) * ticks) / 14.0F);
            ServerLevel level = (ServerLevel)entity.level();
            AABB bb = entity.getBoundingBox();
            level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY(), entity.getZ(), 100, bb.getXsize(), bb.getYsize(), bb.getZsize(), (double)0.25F);
            level.playSound((Player)null, entity, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.2F);
        }

        return true;
    }
}
