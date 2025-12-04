package com.example.examplemod.client.particle;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, ExampleMod.MODID);

    public static final Supplier<SimpleParticleType> BLACK_HOLE_MATTER = PARTICLES.register("black_hole_matter",
            () -> new SimpleParticleType(false));
}