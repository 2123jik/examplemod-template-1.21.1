package com.example.examplemod.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin   {
    @Shadow
    protected boolean inGround;

    @Shadow
    protected int inGroundTime;

    @Redirect(
        // 目标方法是 tick()
        method = "tick()V",
        // 注入点（At）是一个方法调用（INVOKE）
        at = @At(
            value = "INVOKE",
            // 目标调用的具体方法签名，格式为 "Lowner/class/Name;methodName(Larguments/class;)Lreturn/type;"
            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;isCritArrow()Z"
        )
    )
    private boolean worldflipper$SuppressCritParticles(AbstractArrow instance) {
        return false;
    }
}
