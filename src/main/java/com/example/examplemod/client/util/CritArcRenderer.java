package com.example.examplemod.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class CritArcRenderer {

    // 基础常量
    private static final float LAYER_THICKNESS = 2.5f; // 每层厚度
    private static final float INNER_RADIUS_BASE = 8.0f; // 内圈起始半径

    /**
     * 渲染神化暴击弧
     *
     * @param graphics    GuiGraphics 上下文
     * @param x           屏幕 X 坐标
     * @param y           屏幕 Y 坐标
     * @param layers      当前的暴击层数 (Apoth Layers)
     * @param isL2Crit    是否触发 L2 暴击 (金光/橙光)
     * @param baseAlpha   基础透明度
     * @param scale       整体缩放 (推荐 1.5f)
     * @param rotationDeg 旋转角度 (推荐 -45.0f 或 135.0f)
     * @param arcDeg      扇形圆心角 (推荐 15.0f ~ 30.0f)
     */
    public static void render(GuiGraphics graphics, float x, float y, int layers, boolean isL2Crit, float baseAlpha,
                              float scale, float rotationDeg, float arcDeg) {

        // 限制层数显示上限 (视觉上只画9层，防止挡住屏幕，第10层+由 L2 状态表现)
        int renderLayers = Math.min(layers, 9);
        if (renderLayers <= 0 && !isL2Crit) return;

        // --- 1. 计算动画因子 ---
        long time = Util.getMillis();


        // A. 脉冲 (Pulse): 模拟心跳，快速扩张后回缩
        // ((sin(t) + 1) / 2) ^ 4 让波形更尖锐，产生“跳动”感
        float pulseSpeed = time / 150.0f;
        float pulse = (float) Math.pow(Math.sin(pulseSpeed) * 0.5 + 0.5, 4.0);
        float scaleAnim = scale + (pulse * 0.05f * scale); // 随脉冲轻微放大

        // B. 抖动 (Jitter): 高能量时的不稳定偏移
        float jitterX = 0;
        float jitterY = 0;
        if (layers > 5 || isL2Crit) {
            float intensity = isL2Crit ? 1.0f : 0.4f;
            jitterX = (Mth.sin(time * 0.8f) * Mth.cos(time * 0.6f)) * intensity;
            jitterY = (Mth.cos(time * 0.75f) * Mth.sin(time * 0.5f)) * intensity;
        }

        // C. 流光 (Sheen): 亮度带扫过，模拟金属光泽
        // 0.0 -> 1.0 线性周期，每秒一次
        float sheenProgress = (time % 1200) / 1200.0f;

        // --- 2. 准备渲染状态 ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // --- 3. 矩阵变换 ---
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        // 应用位移(含抖动) -> 缩放(含脉冲) -> 旋转
        // 注意：先移到目标位置，再旋转缩放
        poseStack.translate(x + jitterX, y + jitterY, 0);
        poseStack.scale(scaleAnim, scaleAnim, 1.0f);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationDeg));

        Matrix4f matrix = poseStack.last().pose();

        // --- 4. 构建网格 ---
        // 我们需要画 renderLayers 层，如果是 L2 暴击，额外画一层金边
        int totalSteps = renderLayers;
        if (isL2Crit) totalSteps += 1;

        Tesselator tesselator = Tesselator.getInstance();

        // 为了保证颜色从内到外平滑且没有 Strip 连接线问题，我们分层绘制
        // 虽然增加了 DrawCall，但对于 UI 元素来说性能损耗可忽略
        for (int i = 0; i < totalSteps; i++) {
            float rIn = INNER_RADIUS_BASE + (i * LAYER_THICKNESS);
            float rOut = rIn + LAYER_THICKNESS;

            // 如果是 L2 暴击的最后一层，画宽一点，显眼一点
            if (isL2Crit && i == totalSteps - 1) {
                rOut += 1.5f;
            }

            // 获取当前层内圈和外圈的颜色
            // 关键点：当前层的外圈颜色必须等于下一层的内圈颜色，这样才能视觉连续
            int colorIn = getDynamicColor(i, renderLayers, isL2Crit, sheenProgress, baseAlpha);
            int colorOut = getDynamicColor(i + 1, renderLayers, isL2Crit, sheenProgress, baseAlpha);

            drawArcLayer(tesselator, matrix, rIn, rOut, arcDeg, colorIn, colorOut);
        }

        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    /**
     * 绘制单层扇形弧
     * 使用 TRIANGLE_STRIP 绘制一个弯曲的矩形带
     */
    private static void drawArcLayer(Tesselator tesselator, Matrix4f matrix, float rIn, float rOut, float arcDeg, int colorIn, int colorOut) {
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // 分段数：角度越小需要的段数越少，6段对于30度以内足够平滑
        int segments = 6;
        float halfArc = (float) Math.toRadians(arcDeg) / 2.0f;
        float stepRad = (2 * halfArc) / segments;
        float startRad = -halfArc; // 从 -angle/2 到 +angle/2，以 0 度为中心

        for (int s = 0; s <= segments; s++) {
            float theta = startRad + (s * stepRad);
            float cos = Mth.cos(theta);
            float sin = Mth.sin(theta);

            // 顶点顺序：先外后内 (或者先内后外，只要一致即可)
            // Strip 逻辑：Point A(外), Point B(内), Point C(外), Point D(内)... 构成三角形

            // 顶点 1: 外圈点
            buffer.addVertex(matrix, cos * rOut, sin * rOut, 0)
                    .setColor(colorOut);

            // 顶点 2: 内圈点
            buffer.addVertex(matrix, cos * rIn, sin * rIn, 0)
                    .setColor(colorIn);
        }

        // 提交绘制
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /**
     * 计算动态颜色
     * 逻辑：白(内) -> 青 -> 紫 -> 红(外) -> 金(L2)
     * 叠加 Sheen 高光效果
     */
    private static int getDynamicColor(int index, int maxLayers, boolean isL2, float sheenPos, float baseAlpha) {
        // 1. 确定基础色相
        int baseColor;

        // 特殊处理：L2 的最外层是金色
        if (isL2 && index >= maxLayers) {
            baseColor = 0xFFD700; // Gold
        } else if (index == 0) {
            baseColor = 0xFFFFFF; // 核心极亮
        } else {
            // 计算渐变进度 (0.0 ~ 1.0)
            float progress = (float) (index - 1) / Math.max(1, maxLayers - 1);

            // 简单的三段渐变插值
            if (progress < 0.5f) {
                // 前半段: 青 (00FFFF) -> 紫 (FF00FF)
                baseColor = mixColors(0x00FFFF, 0xFF00FF, progress * 2.0f);
            } else {
                // 后半段: 紫 (FF00FF) -> 红 (FF0000)
                baseColor = mixColors(0xFF00FF, 0xFF0000, (progress - 0.5f) * 2.0f);
            }
        }

        // 2. 计算 Sheen (流光高光)
        // 将层数 index 映射到 0~1 的空间，如果 sheenPos 经过这里，就变白
        float normalizedIndex = (float) index / (maxLayers + (isL2 ? 1 : 0));
        float dist = Math.abs(normalizedIndex - sheenPos);

        // 处理循环边界 (可选，让光效看起来是循环的)
        if (dist > 0.5f) dist = 1.0f - dist;

        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = (baseColor) & 0xFF;

        // 如果在高光范围内 (dist < 0.15)，大幅增加亮度
        if (dist < 0.15f) {
            float strength = (0.15f - dist) / 0.15f; // 0.0 ~ 1.0 强度
            int add = (int) (180 * strength); // 增加亮度值
            r = Math.min(255, r + add);
            g = Math.min(255, g + add);
            b = Math.min(255, b + add);
        }

        // 3. 组装 ARGB
        int a = (int) (baseAlpha * 255);

        // 越外层稍微透明一点，增加层次感，但 L2 金边不透明
        if (index > 0 && !(isL2 && index >= maxLayers)) {
            a = (int) (a * 0.85f);
        }

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 简单的 RGB 线性插值
     */
    private static int mixColors(int c1, int c2, float ratio) {
        if (ratio <= 0) return c1;
        if (ratio >= 1) return c2;

        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (r << 16) | (g << 8) | b;
    }
}