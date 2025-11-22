package com.example.examplemod.mixin;

import com.example.examplemod.client.gui.RenderOffsetConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemTipRenderMixin {
    @Unique
    private static final ItemStack EFFECT_ITEM = new ItemStack(Items.NETHER_STAR);
    @Unique
    private static final Vector3f round =new Vector3f(1,1,0).normalize();
    @Unique
    private static final Quaternionf y = new Quaternionf();

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void renderTipEffect(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {

        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft")&&
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("sword")
                && displayContext != ItemDisplayContext.GUI) {
            if (Minecraft.getInstance().level != null) {
                var time=Minecraft.getInstance().level.getGameTime()+Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                poseStack.pushPose();
                poseStack.translate(
                        RenderOffsetConfig.x,
                        RenderOffsetConfig.y,
                        RenderOffsetConfig.z);
                poseStack.scale(0.2f, 0.2f, 0.2f);
                poseStack.rotateAround(y.setAngleAxis(time, round.x, round.y, round.z),0,0,0.01F);
                poseStack.mulPose(Axis.ZP.rotation(-45));
                ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
                renderer.renderStatic(
                        EFFECT_ITEM,
                        ItemDisplayContext.FIXED,
                        combinedLight,
                        combinedOverlay,
                        poseStack,
                        buffer,
                        null,
                        0
                );
                poseStack.popPose();
            }
        }
    }
}