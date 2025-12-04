package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.init.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract int getRemainingFireTicks();
    @Shadow public abstract void setRemainingFireTicks(int p_20269_);

    /**
     * 拦截 igniteForTicks 方法。
     * 原版逻辑：取最大值 (Math.max)。
     * 修改逻辑：如果有燃烧咒效果，取和值 (Add)。
     */
    @Inject(method = "igniteForTicks", at = @At("HEAD"), cancellable = true)
    private void stackFireDuration(int newTicks, CallbackInfo ci) {
        // Entity 本身不知道 MobEffect，只有 LivingEntity 知道
        if ((Object) this instanceof LivingEntity living) {
            // 检查是否有燃烧咒效果
            if (living.hasEffect(ModEffects.COMBUSTION_CURSE)) {
                int currentTicks = this.getRemainingFireTicks();

                // 如果当前没有火，currentTicks 可能是负数（防火冷却），归零处理
                if (currentTicks < 0) currentTicks = 0;

                // 堆叠逻辑：当前剩余 + 新增
                this.setRemainingFireTicks(currentTicks + newTicks);

                // 阻止原版逻辑运行，否则原版会再次覆盖
                ci.cancel();
            }
        }
    }
}