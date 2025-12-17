package com.example.examplemod.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import com.mojang.blaze3d.systems.RenderSystem;

public class ImageTooltipRenderer implements ClientTooltipComponent {
    private final ImageTooltipData data;

    public ImageTooltipRenderer(ImageTooltipData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return data.height() + 2; // +2 留一点上下边距，防止太挤
    }

    @Override
    public int getWidth(Font font) {
        return data.width();
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        // 设置颜色为白色，防止之前的渲染染色影响图片
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 核心绘制逻辑
        // 参数：texture, x, y, u, v, width, height, textureWidth, textureHeight
        graphics.blit(data.texture(), x, y + 1, data.u(), data.v(), data.width(), data.height(), data.texW(), data.texH());
    }
}