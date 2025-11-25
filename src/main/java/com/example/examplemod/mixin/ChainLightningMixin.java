package com.example.examplemod.mixin;

import io.redspace.ironsspellbooks.entity.spells.ChainLightning;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChainLightning.class)
public abstract class ChainLightningMixin extends Projectile {

    // 影子字段：映射原类中的字段，以便在 Mixin 中访问和修改
    @Shadow public float range;
    @Shadow public int maxConnections;

    // 必须调用父类构造函数（Mixin 规范）
    protected ChainLightningMixin(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    // 用于记录本次 tick 是否触发了水下增强，防止重复计算或错误还原
    @Unique
    private boolean isUnderwaterBuffed = false;

    /**
     * 在 tick 方法开始执行逻辑之前拦截。
     * 如果实体在水中，将范围和最大连接数翻 3 倍。
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        // 检查实体是否在水中 (调用 Minecraft 原版 Entity 的 isInWater 方法)
        if (this.isInWater()) {
            this.range *= 3.0F;
            this.maxConnections *= 3;
            this.isUnderwaterBuffed = true;
        }
    }

    /**
     * 在 tick 方法执行完毕后拦截。
     * 如果刚才进行了增强，这里必须还原数值，否则下一 tick 会基于翻倍后的值再次翻倍（指数爆炸）。
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickReturn(CallbackInfo ci) {
        if (this.isUnderwaterBuffed) {
            this.range /= 3.0F;
            this.maxConnections /= 3;
            this.isUnderwaterBuffed = false;
        }
    }
}