package com.example.examplemod.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

public class InlineSpellRenderer implements ClientTooltipComponent {

    private final InlineSpellData data;
    private static final int ICON_SIZE = 10; // 图标渲染大小

    public InlineSpellRenderer(InlineSpellData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return 10; 
    }

    @Override
    public int getWidth(Font font) {
        // 前缀宽 + 图标宽 + 间距(2) + 后缀宽
        return font.width(data.prefix()) + ICON_SIZE + 2 + font.width(data.suffix());
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        // 1. 渲染前缀
        graphics.drawString(font, data.prefix(), x, y + 1, data.color(), false);

        int xAfterPrefix = x + font.width(data.prefix());

        // 2. 渲染图标
        ResourceLocation iconRes = data.spell().getSpellIconResource();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        PoseStack pose = graphics.pose();
        pose.pushPose();
        // 移动到文字后方
        pose.translate(xAfterPrefix, y, 0);
        
        // Iron's Spells 的图标通常是 16x16 的纹理
        // 我们将其缩放到 10x10 以适应文字高度
        float scale = (float) ICON_SIZE / 16.0F; 
        pose.scale(scale, scale, 1.0F);

        // 绘制完整纹理 (u=0, v=0, width=16, height=16)
        graphics.blit(iconRes, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        
        pose.popPose();

        // 3. 渲染后缀
        // 使用法术所属学派的颜色，或者默认颜色
        int suffixColor = data.color();
        // 也可以用: data.spell().getSchoolType().getDisplayName().getStyle().getColor().getValue();
        
        graphics.drawString(font, data.suffix(), xAfterPrefix + ICON_SIZE + 2, y + 1, suffixColor, false);
    }
}