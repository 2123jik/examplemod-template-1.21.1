package com.example.examplemod.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel {

    @Inject(
        method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;ZLnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/level/Explosion;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cf$explode(Entity entity, DamageSource damageSource, ExplosionDamageCalculator damageCalculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction explosionInteraction, boolean animate, ParticleOptions smallParticles, ParticleOptions largeParticles, Holder<SoundEvent> soundEvent, CallbackInfoReturnable<Explosion> cir) {
        if (entity instanceof Creeper creeper) {
            if (creeper.getCommandSenderWorld().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                Level level = (Level) (Object) this;
                if (true)
                {
                    Explosion explosion = new Explosion(level, entity, damageSource, damageCalculator, x, y, z, radius, fire, Explosion.BlockInteraction.KEEP, smallParticles, largeParticles, soundEvent);

                    explosion.explode();
                    explosion.finalizeExplosion(animate);

                    cir.setReturnValue(explosion);
                }
            }
        }
    }
}