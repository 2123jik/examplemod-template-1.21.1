package com.example.examplemod.client.entity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GoldenGateEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class GoldenGateRenderer extends EntityRenderer<GoldenGateEntity> {
    // 贴图路径，确保 resources/assets/examplemod/textures/entity/golden_gate.png 存在
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/entity/golden_gate.png");

    public GoldenGateRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GoldenGateEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.pushPose();

        // --- 1. 旋转处理 ---
        // 计算插值后的旋转角度，保证画面流畅
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // 应用旋转
        // 注意：Minecraft 的 Y 旋转通常需要取负值，且可能需要 +180 度修正，具体取决于贴图方向
        // 这里的逻辑对应你在 Entity 中 calculate 的 lookAt 逻辑
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // --- 2. 绘制设置 ---
        // entityCutoutNoCull: 支持透明通道（波纹通常是透明的），且双面渲染（NoCull），这样从后面看也能看到
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        // --- 3. 绘制一个正方形平面 (Quad) ---
        // 假设我们画一个 1x1 的平面 (半径 0.5)
        float size = 0.5f;

        // 手动添加 4 个顶点 (Vertex)
        // 参数顺序: 矩阵, x, y, z, 颜色(R,G,B,A), UV坐标(u,v), 覆盖层UV, 光照, 法线矩阵, 法线向量(nx, ny, nz)

        // 左上点
        addVertex(vertexConsumer, matrix, pose, packedLight, -size, size, 0, 0, 0);
        // 左下点
        addVertex(vertexConsumer, matrix, pose, packedLight, -size, -size, 0, 0, 1);
        // 右下点
        addVertex(vertexConsumer, matrix, pose, packedLight, size, -size, 0, 1, 1);
        // 右上点
        addVertex(vertexConsumer, matrix, pose, packedLight, size, size, 0, 1, 0);

        poseStack.popPose();
    }

    // 辅助方法：简化顶点添加代码
    private void addVertex(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose normal, int light, float x, float y, float z, float u, float v) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, 255) // 白色，不改变贴图颜色
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normal, 0, 0, 1) // 法线指向正前方
        ; // 新版 Minecraft 可能不需要 .endVertex()，或者链式调用直接结束
    }

    @Override
    public ResourceLocation getTextureLocation(GoldenGateEntity entity) {
        return TEXTURE;
    }
}