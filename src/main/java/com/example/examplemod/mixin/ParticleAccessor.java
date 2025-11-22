package com.example.examplemod.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    // 原版已经有 getLifetime() 了，所以我们改名叫 getRawLifetime
    // 注解里的 "lifetime" 告诉 Mixin 我们要访问的是 lifetime 字段
    @Accessor("lifetime")
    int getRawLifetime();

    // 原版没有 setLifetime，但为了保持风格统一，我们也改名
    @Accessor("lifetime")
    void setRawLifetime(int lifetime);
}