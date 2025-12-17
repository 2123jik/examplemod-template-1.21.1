package com.example.examplemod.mixin.ironsspellbooks;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ManaBarOverlay.class)
public abstract class MixinManaBarOverlay {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void cancelOriginalRender(GuiGraphics guiHelper, DeltaTracker deltaTracker, CallbackInfo ci) {
        // 直接取消，把舞台留给我们自己的 Event Handler
        ci.cancel();
    }
}