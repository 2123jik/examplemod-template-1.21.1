package com.example.examplemod.mixin.apothic;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.shadowsoffire.apotheosis.client.AffixItemEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AffixItemEffectRenderer.class)
public class AffixItemEffectRendererMixin {

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/loot/RarityRenderData;beamHeight()F"
        ),
        remap = false
    )
    private static float modifyBeamHeight(float original) {
        return original * 10.0F;
    }
}