package com.example.examplemod.client.time;

import net.minecraft.core.particles.ParticleOptions;

public record SnapshotParticle(
    ParticleOptions type, // 粒子的类型 (火焰、烟雾、模组法术特效)
    double relX, double relY, double relZ, // 相对玩家的位置偏移
    double xSpeed, double ySpeed, double zSpeed // 粒子的速度
) {}