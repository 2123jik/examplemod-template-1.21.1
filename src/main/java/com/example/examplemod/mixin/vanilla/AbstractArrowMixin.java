package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
//删除暴击箭粒子//
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin   {
    @Redirect(
        method = "tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;isCritArrow()Z"
        )
    )
    private boolean worldflipper$SuppressCritParticles(AbstractArrow instance) {
        return false;
    }
}
