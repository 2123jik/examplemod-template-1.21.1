package com.example.examplemod.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Collection;

public class EffectOrbitRenderer {

    // 配置参数
    private static final float ORBIT_RADIUS = 1.0f; // 公转半径
    private static final float ROTATION_SPEED = 2.0f; // 公转速度
    private static final float ICON_SCALE = 0.5f; // 图标大小
    private static final float ITEM_THICKNESS = 0.05f; // 模拟厚度
    private static final int LAYERS = 16; // 堆叠层数 (越高越平滑，但性能开销略增，16是原版物品标准)

    /**
     * 主渲染方法：在世界渲染事件中调用此方法
     */
    public static void renderOrbitingEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        Collection<MobEffectInstance> effects = entity.getActiveEffects();
        if (effects.isEmpty()) return;

        // 获取相机旋转，用于 Billboarding (让图标始终朝向相机)
        Quaternionf cameraRot = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        
        // 计算时间变量用于旋转
        double time = (entity.tickCount + partialTick) * (ROTATION_SPEED * 0.05);
        int count = effects.size();
        int index = 0;

        for (MobEffectInstance effect : effects) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect.getEffect());

            poseStack.pushPose();

            // 1. 计算公转位置
            // 将圆周平分
            double angle = time + (2 * Math.PI * index / count);
            
            // 计算偏移坐标 (x, z 旋转，y 浮动在头顶)
            float offsetX = (float) Math.cos(angle) * ORBIT_RADIUS;
            float offsetZ = (float) Math.sin(angle) * ORBIT_RADIUS;
            float offsetY = entity.getBbHeight() + 0.6f; // 头顶上方 0.6 格

            // 移动到目标位置
            poseStack.translate(offsetX, offsetY, offsetZ);

            // 2. Billboarding (核心：抵消相机的旋转，使平面始终正对屏幕)
            poseStack.mulPose(cameraRot);

            // 3. 缩放
            poseStack.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);

            // 4. 绘制伪 3D 图标
            // 实体上方的光照通常取实体本身的光照，或者最高亮度 15 (0xF000F0)
            int packedLight = Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick);
            render3DIcon(poseStack, buffer, sprite, packedLight);

            poseStack.popPose();
            index++;
        }
    }

    /**
     * 手写 3D 渲染器：通过堆叠图层模拟厚度
     */
    private static void render3DIcon(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, int packedLight) {
        // 使用 CUTOUT 渲染类型，支持透明度
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(sprite.atlasLocation()));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        // 基础参数
        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        // 居中偏移 (-0.5 ~ 0.5)
        float size = 1.0f;
        float halfSize = size / 2.0f;
        
        // -------------------------------------------------------
        // 步骤 A: 绘制“侧面” (通过层叠模拟)
        // -------------------------------------------------------
        // 我们从后向前画 LAYERS 层，每层 Z 轴稍微偏移
        // 这种方法比复杂的像素扫描快得多，且在动态公转时视觉效果几乎一样
        float step = ITEM_THICKNESS / LAYERS;
        
        for (int i = 0; i < LAYERS; i++) {
            // 计算当前层的 Z 深度 (-thickness/2 到 +thickness/2)
            float z = (i * step) - (ITEM_THICKNESS / 2.0f);
            
            // 为了模拟阴影，中间的层可以稍微暗一点 (可选，这里用纯白)
            // 如果觉得侧面太亮，可以把 color 改成 230, 230, 230
            addQuad(consumer, matrix, packedLight, 
                    -halfSize, -halfSize, size, size, z, 
                    minU, maxU, minV, maxV, 
                    255, 255, 255);
        }

        // -------------------------------------------------------
        // 步骤 B: 绘制 正面 和 背面 (封顶，防止看到层叠的裂纹)
        // -------------------------------------------------------
        
        // 正面 (Z = +thickness/2)
        addQuad(consumer, matrix, packedLight,
                -halfSize, -halfSize, size, size, ITEM_THICKNESS / 2.0f,
                minU, maxU, minV, maxV, 
                255, 255, 255);

        // 背面 (Z = -thickness/2) - 注意：为了背面看正常，通常需要翻转 U 坐标，这里简化处理直接画
        addQuad(consumer, matrix, packedLight,
                -halfSize, -halfSize, size, size, -ITEM_THICKNESS / 2.0f,
                minU, maxU, minV, maxV, 
                255, 255, 255);
    }

    /**
     * 底层方法：向显存推入一个四边形 (Quad)
     */
    private static void addQuad(VertexConsumer consumer, Matrix4f matrix, int light, 
                                float x, float y, float w, float h, float z, 
                                float minU, float maxU, float minV, float maxV,
                                int r, int g, int b) {
        
        // 左上 -> 左下 -> 右下 -> 右上 (逆时针顺序)
        // 注意：法线 (Normal) 对于光照很重要，这里简单设为 (0, 1, 0) 或者根据朝向设定
        
        // 顶点 1: 左上
        consumer.addVertex(matrix, x, y + h, z)
                .setColor(r, g, b, 255)
                .setUv(minU, maxV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0); // 简化法线

        // 顶点 2: 左下
        consumer.addVertex(matrix, x + w, y + h, z)
                .setColor(r, g, b, 255)
                .setUv(maxU, maxV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);

        // 顶点 3: 右下
        consumer.addVertex(matrix, x + w, y, z)
                .setColor(r, g, b, 255)
                .setUv(maxU, minV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);

        // 顶点 4: 右上
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(minU, minV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);
    }
}