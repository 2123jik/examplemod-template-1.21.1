package com.example.examplemod.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"))
    private void onAddParticle(Particle particle, CallbackInfo ci) {
        if (particle == null) return;

        ParticleAccessor accessor = (ParticleAccessor) particle;

        // 使用修改后的新方法名
        int originalLife = accessor.getRawLifetime();

        int newLife = Math.max(1, originalLife / 3);

        // 使用修改后的新方法名
        accessor.setRawLifetime(newLife);
    }
}