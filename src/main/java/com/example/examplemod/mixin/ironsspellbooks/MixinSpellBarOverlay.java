package com.example.examplemod.mixin.ironsspellbooks;

import io.redspace.ironsspellbooks.gui.overlays.SpellBarOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpellBarOverlay.class)
public class MixinSpellBarOverlay {

    // 在方法最开始直接取消，原版代码一行都不会执行
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void cancelOriginalRender(GuiGraphics guiHelper, DeltaTracker deltaTracker, CallbackInfo ci) {
        ci.cancel();
    }
}