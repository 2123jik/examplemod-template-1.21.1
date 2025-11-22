package com.example.examplemod.client.entity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.SwordProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

public class SwordProjectileRenderer extends EntityRenderer<SwordProjectileEntity> {
    private final ItemRenderer itemRenderer;

    // 复用末地烛的纹理，它是一个很好的白色渐变光束，适合做拖尾
    private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,"textures/entity/white.png");

    public SwordProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SwordProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 1. 旋转处理 (保持你原有的逻辑)
        // 注意：通常 projectile 的 forward 是 Z 轴，你需要确认你的模型朝向
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));


        // 3. 渲染剑本体 (你原有的逻辑)
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));

        float scale = 1.0f;
        poseStack.scale(scale, scale, scale);
        ItemStack sword = entity.getSwordItem();

        this.itemRenderer.renderStatic(
                sword,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                FULL_BRIGHT, // 剑本身也是全亮
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose(); // 结束剑的变换

        poseStack.popPose(); // 结束整个实体的变换
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordProjectileEntity entity) {
        // 这个方法对于 renderStatic 渲染的实体通常不重要，但最好返回一个默认值
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}