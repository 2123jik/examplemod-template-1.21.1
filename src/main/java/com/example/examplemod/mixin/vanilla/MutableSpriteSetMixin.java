package com.example.examplemod.mixin.vanilla;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

// 使用 targets 来定位内部类
@Mixin(targets = "net.minecraft.client.particle.ParticleEngine$MutableSpriteSet")
public class MutableSpriteSetMixin {

    @Shadow
    private List<TextureAtlasSprite> sprites;

    /**
     * @author TimeMod
     * @reason 修复粒子寿命缩短后，age > maxAge 导致的数组越界崩溃
     */
    @Overwrite
    public TextureAtlasSprite get(int particleAge, int particleMaxAge) {
        // 1. 确保分母不为 0
        int safeMaxAge = Math.max(1, particleMaxAge);

        // 2. 核心修复：将 age 限制在安全范围内
        // 如果 age 超出了 maxAge，强制按 maxAge 计算（即显示最后一帧）
        int safeAge = Math.min(particleAge, safeMaxAge);
        
        // 防御性编程：防止负数
        safeAge = Math.max(0, safeAge);

        // 原始逻辑：particleAge * (size - 1) / particleMaxAge
        // 现在使用安全的参数，即使 age 很大，计算结果也永远不会超过 size - 1
        return this.sprites.get(safeAge * (this.sprites.size() - 1) / safeMaxAge);
    }
}