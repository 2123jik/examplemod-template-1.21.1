package com.example.examplemod.client.layer;

import com.example.examplemod.client.util.SmartCooldownRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RenderCustomEffect {
    private static final int ICON_SIZE = 18;
    private static final int GAP_X = 24;
    private static final int GAP_Y = 24;
    private static final float BLIT_OFFSET = 100.0f;

    private static final Map<MobEffect, Integer> EFFECT_MAX_DURATION_CACHE = new HashMap<>();
    public static void renderCustomEffect(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Collection<MobEffectInstance> effects = player.getActiveEffects();

        if (effects.isEmpty()) {
            EFFECT_MAX_DURATION_CACHE.clear();
            return;
        }

        Set<MobEffect> currentEffectTypes = effects
                .stream()
                .map(MobEffectInstance::getEffect)
                .map(Holder::value)
                .collect(Collectors.toSet());
        EFFECT_MAX_DURATION_CACHE.keySet().retainAll(currentEffectTypes);

        // 3. 分组准备渲染
        Map<MobEffectCategory, List<MobEffectInstance>> groupedEffects = effects.stream()
                .collect(Collectors.groupingBy(e -> e.getEffect().value().getCategory(), Collectors.toList()));

        double scale= mc.getWindow().getGuiScale();

        double currentY=(mc.getWindow().getHeight())/scale -(46+ICON_SIZE);
        double currentX =(mc.getWindow().getWidth()/2F)/scale- 100;
        for (MobEffectCategory category : MobEffectCategory.values()) {
            List<MobEffectInstance> list = groupedEffects.get(category);
            if (list == null || list.isEmpty()) continue;
            for (MobEffectInstance effectInstance : list) {

                renderSingleEffect(graphics, mc, (int) currentX, (int) currentY, effectInstance);
                currentX += GAP_X;
            }
            currentX =(mc.getWindow().getWidth()/2F)/scale- 100;
            currentY -= GAP_Y;
        }
    }

    private static void renderSingleEffect(GuiGraphics graphics, Minecraft mc, int x, int y, MobEffectInstance effectInstance) {
        TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effectInstance.getEffect());

        // 1. 绘制图标
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.enableBlend();
        graphics.blit(x, y, 0, 18, 18, sprite);
        graphics.drawString(mc.font, String.valueOf(effectInstance.getAmplifier()+1), x+ICON_SIZE/2, y+ICON_SIZE-2, effectInstance.getEffect().value().getColor());

        // 2. 使用工具类计算比例 (复用逻辑)
        float ratio = SmartCooldownRenderer.calculateRatio(effectInstance, EFFECT_MAX_DURATION_CACHE);

        // 3. 使用工具类绘制遮罩 (复用逻辑)
        // HUD通常需要较高的Z值，这里传入 BLIT_OFFSET
        SmartCooldownRenderer.renderCooldown(graphics, x, y, ICON_SIZE, ratio, BLIT_OFFSET);
    }
}