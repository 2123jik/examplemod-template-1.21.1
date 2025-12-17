package com.example.examplemod.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Matrix4f;

import java.util.Map;

public class SmartCooldownRenderer {

    /**
     * 计算并更新效果的最大持续时间缓存，返回当前冷却比例 (0.0 - 1.0)
     */
    public static float calculateRatio(MobEffectInstance effectInstance, Map<MobEffect, Integer> durationCache) {
        MobEffect effectType = effectInstance.getEffect().value();
        int currentDuration = effectInstance.getDuration();
        
        // 获取缓存中的最大值
        int maxDuration = durationCache.getOrDefault(effectType, 0);

        // 如果当前时间比缓存大（刚喝药/刷新），更新缓存
        if (currentDuration > maxDuration) {
            maxDuration = currentDuration;
            durationCache.put(effectType, maxDuration);
        }

        // 防止除以0
        if (maxDuration <= 0) maxDuration = currentDuration;
        if (maxDuration == 0) maxDuration = 1;

        return 1.0f - (currentDuration / (float) maxDuration);
    }

    /**
     * 绘制冷却遮罩
     * @param graphics GuiGraphics
     * @param x 起始X
     * @param y 起始Y
     * @param size 图标大小 (通常是18)
     * @param ratio 冷却比例 (0.0 - 1.0)
     * @param zOffset Z轴偏移 (Jade可能需要0.01f，HUD可能需要0或更高)
     */
    public static void renderCooldown(GuiGraphics graphics, float x, float y, float size, float ratio, float zOffset) {
        if (ratio <= 0) return;

        // 如果满了，直接画个全黑方块
        if (ratio >= 1.0f) {
            // 使用 fill 会自动处理当前 PoseStack 的位置，但我们需要手动控制 Z
            // 这里为了简单，直接推入Pose
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zOffset);
            graphics.fill((int) x, (int) y, (int) (x + size), (int) (y + size), 0x80000000);
            graphics.pose().popPose();
            return;
        }

        // --- 核心渲染逻辑 ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest(); // 暂时禁用深度测试以覆盖在图标上
        RenderSystem.disableCull();      // 禁用面剔除
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        float r = size / 2.0f;

        float red = 0.0f; float green = 0.0f; float blue = 0.0f; float alpha = 0.6f;

        float step = ratio * 4.0f;
        int fullQuadrants = (int) step;
        float currentQuadProgress = step - fullQuadrants;

        for (int i = 0; i < fullQuadrants; i++) {
            drawFixedQuadrant(buffer, matrix, cx, cy, r, i, red, green, blue, alpha, zOffset);
        }

        double angleRad = currentQuadProgress * (Math.PI / 2.0);
        drawPartialQuadrant(buffer, matrix, cx, cy, r, fullQuadrants, angleRad, red, green, blue, alpha, zOffset);

        try {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } catch (Exception e) {
            // ignore
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // --- 以下是私有辅助方法 ---

    private static void drawFixedQuadrant(BufferBuilder buffer, Matrix4f mat, float cx, float cy, float r, int qIdx, float red, float g, float b, float a, float z) {
        float x2, y2, x3, y3;
        switch (qIdx) {
            case 0 -> { x2 = cx + r; y2 = cy - r; x3 = cx + r; y3 = cy; addQuad(buffer, mat, cx, cy - r, x2, y2, x3, y3, cx, cy, red, g, b, a, z); }
            case 1 -> { x2 = cx + r; y2 = cy + r; x3 = cx; y3 = cy + r; addQuad(buffer, mat, cx + r, cy, x2, y2, x3, y3, cx, cy, red, g, b, a, z); }
            case 2 -> { x2 = cx - r; y2 = cy + r; x3 = cx - r; y3 = cy; addQuad(buffer, mat, cx, cy + r, x2, y2, x3, y3, cx, cy, red, g, b, a, z); }
            case 3 -> { x2 = cx - r; y2 = cy - r; x3 = cx; y3 = cy - r; addQuad(buffer, mat, cx - r, cy, x2, y2, x3, y3, cx, cy, red, g, b, a, z); }
        }
    }

    private static void drawPartialQuadrant(BufferBuilder buffer, Matrix4f mat, float cx, float cy, float r, int qIdx, double angleRad, float red, float g, float b, float a, float z) {
        boolean isPast45 = angleRad > (Math.PI / 4.0);
        float startX = 0, startY = 0, cornerX = 0, cornerY = 0, endX = 0, endY = 0;

        switch (qIdx) {
            case 0 -> { startX = cx; startY = cy - r; cornerX = cx + r; cornerY = cy - r; endX = cx + r; endY = cy; }
            case 1 -> { startX = cx + r; startY = cy; cornerX = cx + r; cornerY = cy + r; endX = cx; endY = cy + r; }
            case 2 -> { startX = cx; startY = cy + r; cornerX = cx - r; cornerY = cy + r; endX = cx - r; endY = cy; }
            case 3 -> { startX = cx - r; startY = cy; cornerX = cx - r; cornerY = cy - r; endX = cx; endY = cy - r; }
        }

        if (!isPast45) {
            double offset = r * Math.tan(angleRad);
            float moveX = 0, moveY = 0;
            switch (qIdx) {
                case 0 -> { moveX = cx + (float) offset; moveY = startY; }
                case 1 -> { moveX = startX; moveY = cy + (float) offset; }
                case 2 -> { moveX = cx - (float) offset; moveY = startY; }
                case 3 -> { moveX = startX; moveY = cy - (float) offset; }
            }
            addTriangle(buffer, mat, cx, cy, startX, startY, moveX, moveY, red, g, b, a, z);
        } else {
            addTriangle(buffer, mat, cx, cy, startX, startY, cornerX, cornerY, red, g, b, a, z);
            double offset2 = r * Math.tan(angleRad - (Math.PI / 4.0));
            float moveX = 0, moveY = 0;
            switch (qIdx) {
                case 0 -> { moveX = endX; moveY = (cy - r) + (float) offset2; }
                case 1 -> { moveX = (cx + r) - (float) offset2; moveY = endY; }
                case 2 -> { moveX = endX; moveY = (cy + r) - (float) offset2; }
                case 3 -> { moveX = (cx - r) + (float) offset2; moveY = endY; }
            }
            addTriangle(buffer, mat, cx, cy, cornerX, cornerY, moveX, moveY, red, g, b, a, z);
        }
    }

    private static void addTriangle(BufferBuilder buffer, Matrix4f mat, float x1, float y1, float x2, float y2, float x3, float y3, float r, float g, float b, float a, float z) {
        buffer.addVertex(mat, x1, y1, z).setColor(r, g, b, a);
        buffer.addVertex(mat, x2, y2, z).setColor(r, g, b, a);
        buffer.addVertex(mat, x3, y3, z).setColor(r, g, b, a);
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f mat, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r, float g, float b, float a, float z) {
        addTriangle(buffer, mat, x1, y1, x2, y2, x4, y4, r, g, b, a, z);
        addTriangle(buffer, mat, x2, y2, x3, y3, x4, y4, r, g, b, a, z);
    }
}