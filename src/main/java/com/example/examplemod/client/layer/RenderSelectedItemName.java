package com.example.examplemod.client.layer;

import com.example.examplemod.client.tooltip.TooltipResolver;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

import static com.example.examplemod.client.tooltip.RichTooltipRenderer.ICON_SIZE;
import static com.example.examplemod.client.tooltip.RichTooltipRenderer.ICON_SPACING;
import static dev.shadowsoffire.apotheosis.Apoth.Components.RARITY;

@EventBusSubscriber(value = Dist.CLIENT)
public class RenderSelectedItemName {

    // --- 状态变量 ---
    private static int toolHighlightTimer = 0;
    private static ItemStack lastToolHighlight = ItemStack.EMPTY;

    /**
     * Client Tick: 处理计时器逻辑
     */
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack itemstack = mc.player.getMainHandItem();

        if (itemstack.isEmpty()) {
            toolHighlightTimer = 0;
        } else {
            // 如果物品发生变化（类型、名字、Tooltip变化）
            if (lastToolHighlight.isEmpty() ||
                    !itemstack.is(lastToolHighlight.getItem()) ||
                    !itemstack.getHoverName().equals(lastToolHighlight.getHoverName())) {

                // 重置计时器 (40 ticks = 2秒)
                toolHighlightTimer = (int)(40.0 * mc.options.notificationDisplayTime().get());
            } else if (toolHighlightTimer > 0) {
                toolHighlightTimer--;
            }
        }

        // 更新缓存
        lastToolHighlight = itemstack.isEmpty() ? ItemStack.EMPTY : itemstack.copy();
    }

    /**
     * 渲染方法：由你的 HudOverlay 或 RenderGuiEvent 调用
     */
    public static void renderSelectedItemName(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();

        // 计时器归零或物品为空时不渲染
        if (toolHighlightTimer <= 0 || lastToolHighlight.isEmpty()) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        // 1. 计算透明度 (Alpha)
        // 原版淡出逻辑：最后几 tick 快速变透明
        int alpha = (int)((float)toolHighlightTimer * 256.0F / 10.0F);
        if (alpha > 255) alpha = 255;

        // 如果完全透明，直接跳过
        if (alpha <= 0) return;

        // 2. 计算基础颜色 (RGB)
        // 默认白色
        int baseColor = 0xFFFFFF;

        // 获取原版稀有度
        Rarity rarity = lastToolHighlight.getRarity();
        if (rarity != null) {
            baseColor = rarity.color().getColor();
        }

        // 获取 Apotheosis 稀有度覆盖
        var lootRarity = lastToolHighlight.get(RARITY);
        if (lootRarity != null) {
            baseColor = lootRarity.get().color().getValue();
        }

        // 获取 Apotheosis 纯度颜色覆盖
        var purity = lastToolHighlight.get(Apoth.Components.PURITY);
        if (purity != null) {
            baseColor = purity.getColor().getValue();
        }

        // 3. 合成最终颜色 (ARGB)
        // 这里的关键是把 alpha 移位到最高8位，并保留 baseColor 的 RGB 部分
        // (baseColor & 0x00FFFFFF) 确保去掉原颜色可能携带的 alpha 信息
        int finalColor = (baseColor & 0x00FFFFFF) | (alpha << 24);

        // 4. 准备文本组件
        Component nameComponent = lastToolHighlight.getHoverName();
        MutableComponent component = nameComponent.copy();

        if (lastToolHighlight.has(DataComponents.CUSTOM_NAME)) {
            component.withStyle(ChatFormatting.ITALIC);
        }

        // 5. 解析富文本
        String rawText = component.getString();
        List<Object> segments = TooltipResolver.INSTANCE.parseText(rawText);

        // 6. 计算总宽度 (用于居中)
        int totalWidth = 0;
        for (Object segment : segments) {
            if (segment instanceof String str) {
                totalWidth += font.width(str);
            } else if (segment instanceof TooltipResolver.IconDrawer) {
                totalWidth += ICON_SIZE + ICON_SPACING;
            }
        }

        // 7. 计算坐标
        int x = (screenWidth - totalWidth) / 2;

        // [修正] Y坐标：原版通常在 height - 59，如果在屏幕中间会挡住准星
        // 如果你确实想在屏幕中间显示，可以改回 screenHeight / 2
        int y =16+ screenHeight / 2;

        if (!mc.gameMode.canHurtPlayer()) {
            y += 14; // 创造模式稍微向下偏移，避免被快捷栏挡住
        }

        // 8. 渲染循环
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int currentX = x;

        for (Object segment : segments) {
            if (segment instanceof String str) {
                // [修正] 使用带有 Alpha 的 finalColor
                graphics.drawString(font, str, currentX, y, finalColor, true); // true = 开启阴影
                currentX += font.width(str);
            } else if (segment instanceof TooltipResolver.IconDrawer drawer) {
                // [修正] 图标淡出
                // 设置 Shader 颜色，其中 alpha 必须是 0.0 ~ 1.0
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);

                drawer.draw(graphics, currentX, y - 1, ICON_SIZE); // y-1 为了微调对齐

                // 绘制完一个图标后重置颜色，以免影响后续渲染（虽然 drawString 会重置 shader，但保险起见）
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                currentX += ICON_SIZE + ICON_SPACING;
            }
        }

        RenderSystem.disableBlend();
    }
}