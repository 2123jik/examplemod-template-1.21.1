package com.example.examplemod.client.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.*;
import java.util.stream.Collectors;

public class RenderCustomEffect {
    private static final int ICON_SIZE = 18;
    private static final int GAP_X = 24;
    private static final int GAP_Y = 24;
    private static final float BLIT_OFFSET = 100.0f;

    // 新增：用于缓存每个 Buff 实例见过的最大时长的 Map
    private static final Map<MobEffect, Integer> EFFECT_MAX_DURATION_CACHE = new HashMap<>();

    public static void renderCustomEffect(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Collection<MobEffectInstance> effects = player.getActiveEffects();

        // 1. 如果没有 Buff，直接清空缓存并返回
        if (effects.isEmpty()) {
            EFFECT_MAX_DURATION_CACHE.clear();
            return;
        }

        // 2. 维护缓存：移除玩家身上已经不存在的 Buff
        // 获取当前所有 Buff 的类型集合
        Set<MobEffect> currentEffectTypes = effects.stream()
                .map(MobEffectInstance::getEffect)
                        .map(Holder<MobEffect>::value).collect(Collectors.toSet());
        // 从缓存中移除不再存在的 Buff
        EFFECT_MAX_DURATION_CACHE.keySet().retainAll(currentEffectTypes);

        // 3. 分组准备渲染
        Map<MobEffectCategory, List<MobEffectInstance>> groupedEffects = effects.stream()
                .collect(Collectors.groupingBy(e -> e.getEffect().value().getCategory(), Collectors.toList()));

        int startX = 2300 / mc.options.guiScale().get();
        int startY = 150;
        int currentX = startX;

        List<MobEffectCategory> categoryOrder = List.of(
                MobEffectCategory.BENEFICIAL,
                MobEffectCategory.NEUTRAL,
                MobEffectCategory.HARMFUL
        );

        for (MobEffectCategory category : categoryOrder) {
            List<MobEffectInstance> list = groupedEffects.get(category);
            if (list == null || list.isEmpty()) continue;
            int currentY = startY;

            for (MobEffectInstance effectInstance : list) {
                // 4. 更新缓存逻辑
                // 如果当前时间比记录的时间还长（说明刚喝药/刷新了），更新缓存
                // 如果缓存里没有，也存进去
                int currentDuration = effectInstance.getDuration();
                MobEffect effectType = effectInstance.getEffect().value();

                int cachedMax = EFFECT_MAX_DURATION_CACHE.getOrDefault(effectType, 0);

                if (currentDuration > cachedMax) {
                    cachedMax = currentDuration;
                    EFFECT_MAX_DURATION_CACHE.put(effectType, cachedMax);
                }

                // 5. 传入计算好的 maxDuration 进行渲染
                renderSingleEffect(graphics, mc, currentX, currentY, effectInstance, cachedMax);
                currentY += GAP_Y;
            }
            currentX += GAP_X;
        }
    }

    // 修改：增加 maxDuration 参数，移除 IExtendedMobEffect 逻辑
    private static void renderSingleEffect(GuiGraphics graphics, Minecraft mc, int x, int y, MobEffectInstance effectInstance, int maxDuration) {
        TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effectInstance.getEffect());

        // 1. 绘制图标
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.enableBlend();
        graphics.blit(x, y, 0, 18, 18, sprite);
        graphics.drawString(mc.font, String.valueOf(effectInstance.getAmplifier()+1),x+ICON_SIZE/2,y+ICON_SIZE-2,effectInstance.getEffect().value().getColor());
        // 2. 计算比例
        float currentDuration = effectInstance.getDuration();

        // 防止除以0
        if (maxDuration <= 0) maxDuration = (int) currentDuration;
        if (maxDuration == 0) maxDuration = 1;

        float ratio = 1.0f - (currentDuration / (float) maxDuration);

        // 3. 绘制冷却遮罩
        drawSmartCooldown(graphics, x, y, ratio);
    }

    private static void drawSmartCooldown(GuiGraphics graphics, int x, int y, float ratio) {
        if (ratio <= 0) return;

        // 如果满了，直接画个全黑方块 (注意这里的Z值)
        if (ratio >= 1.0f) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 100.0f); // 提升深度
            graphics.fill(x, y, x + RenderCustomEffect.ICON_SIZE, y + RenderCustomEffect.ICON_SIZE, 0x80000000); // 半透明黑
            graphics.pose().popPose();
            return;
        }

        // --- 核心渲染逻辑 ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest(); // 禁用深度测试，防止被图标遮挡
        RenderSystem.disableCull();      // [关键] 禁用面剔除！防止因为三角形画反了而看不见
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // 重置全局颜色

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        float cx = x + RenderCustomEffect.ICON_SIZE / 2.0f;
        float cy = y + RenderCustomEffect.ICON_SIZE / 2.0f;
        float r = RenderCustomEffect.ICON_SIZE / 2.0f;

        float red = 0.0f;
        float green = 0.0f;
        float blue = 0.0f;
        float alpha = 0.6f;

        float step = ratio * 4.0f;
        int fullQuadrants = (int) step;
        float currentQuadProgress = step - fullQuadrants;

        for (int i = 0; i < fullQuadrants; i++) {
            drawFixedQuadrant(buffer, matrix, cx, cy, r, i, red, green, blue, alpha);
        }

        double angleRad = currentQuadProgress * (Math.PI / 2.0);
        drawPartialQuadrant(buffer, matrix, cx, cy, r, fullQuadrants, angleRad, red, green, blue, alpha);

        // 3. 提交绘制
        try {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } catch (Exception e) {
            e.printStackTrace(); // 捕获可能的构建错误
        }

        RenderSystem.enableCull();      // 恢复面剔除
        RenderSystem.enableDepthTest(); // 恢复深度测试
        RenderSystem.disableBlend();
    }

    private static void drawFixedQuadrant(BufferBuilder buffer, Matrix4f mat, float cx, float cy, float r, int qIdx, float red, float g, float b, float a) {
        float x2, y2;
        float x3, y3;

        switch (qIdx) {
            case 0: // 右上
                x2 = cx + r; y2 = cy - r;
                x3 = cx + r; y3 = cy;
                addQuad(buffer, mat, cx, cy - r, x2, y2, x3, y3, cx, cy, red, g, b, a);
                break;
            case 1: // 右下
                x2 = cx + r; y2 = cy + r;
                x3 = cx;     y3 = cy + r;
                addQuad(buffer, mat, cx + r, cy, x2, y2, x3, y3, cx, cy, red, g, b, a);
                break;
            case 2: // 左下
                x2 = cx - r; y2 = cy + r;
                x3 = cx - r; y3 = cy;
                addQuad(buffer, mat, cx, cy + r, x2, y2, x3, y3, cx, cy, red, g, b, a);
                break;
            case 3: // 左上
                x2 = cx - r; y2 = cy - r;
                x3 = cx;     y3 = cy - r;
                addQuad(buffer, mat, cx - r, cy, x2, y2, x3, y3, cx, cy, red, g, b, a);
                break;
        }
    }

    private static void drawPartialQuadrant(BufferBuilder buffer, Matrix4f mat, float cx, float cy, float r, int qIdx, double angleRad, float red, float g, float b, float a) {
        boolean isPast45 = angleRad > (Math.PI / 4.0);
        float startX = 0, startY = 0;
        float cornerX = 0, cornerY = 0;
        float endX = 0, endY = 0;

        switch (qIdx) {
            case 0: // 右上
                startX = cx;     startY = cy - r;
                cornerX = cx + r; cornerY = cy - r;
                endX = cx + r;   endY = cy;
                break;
            case 1: // 右下
                startX = cx + r; startY = cy;
                cornerX = cx + r; cornerY = cy + r;
                endX = cx;       endY = cy + r;
                break;
            case 2: // 左下
                startX = cx;     startY = cy + r;
                cornerX = cx - r; cornerY = cy + r;
                endX = cx - r;   endY = cy;
                break;
            case 3: // 左上
                startX = cx - r; startY = cy;
                cornerX = cx - r; cornerY = cy - r;
                endX = cx;       endY = cy - r;
                break;
        }

        if (!isPast45) {
            double offset = r * Math.tan(angleRad);
            float moveX = 0, moveY = 0;
            moveY = switch (qIdx) {
                case 0 -> {
                    moveX = cx + (float) offset;
                    yield startY;
                }
                case 1 -> {
                    moveX = startX;
                    yield cy + (float) offset;
                }
                case 2 -> {
                    moveX = cx - (float) offset;
                    yield startY;
                }
                case 3 -> {
                    moveX = startX;
                    yield cy - (float) offset;
                }
                default -> moveY;
            };
            addTriangle(buffer, mat, cx, cy, startX, startY, moveX, moveY, red, g, b, a);
        } else {
            addTriangle(buffer, mat, cx, cy, startX, startY, cornerX, cornerY, red, g, b, a);
            double offset2 = r * Math.tan(angleRad - (Math.PI / 4.0));
            float moveX = 0, moveY = 0;
            moveY = switch (qIdx) {
                case 0 -> {
                    moveX = endX;
                    yield (cy - r) + (float) offset2;
                }
                case 1 -> {
                    moveX = (cx + r) - (float) offset2;
                    yield endY;
                }
                case 2 -> {
                    moveX = endX;
                    yield (cy + r) - (float) offset2;
                }
                case 3 -> {
                    moveX = (cx - r) + (float) offset2;
                    yield endY;
                }
                default -> moveY;
            };
            addTriangle(buffer, mat, cx, cy, cornerX, cornerY, moveX, moveY, red, g, b, a);
        }
    }

    private static void addTriangle(BufferBuilder buffer, Matrix4f mat, float x1, float y1, float x2, float y2, float x3, float y3, float r, float g, float b, float a) {
        buffer.addVertex(mat, x1, y1, BLIT_OFFSET).setColor(r, g, b, a);
        buffer.addVertex(mat, x2, y2, BLIT_OFFSET).setColor(r, g, b, a);
        buffer.addVertex(mat, x3, y3, BLIT_OFFSET).setColor(r, g, b, a);
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f mat, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r, float g, float b, float a) {
        addTriangle(buffer, mat, x1, y1, x2, y2, x4, y4, r, g, b, a);
        addTriangle(buffer, mat, x2, y2, x3, y3, x4, y4, r, g, b, a);
    }
}