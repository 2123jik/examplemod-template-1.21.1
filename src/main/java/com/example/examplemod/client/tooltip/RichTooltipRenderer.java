package com.example.examplemod.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import com.example.examplemod.client.tooltip.TooltipResolver;
public class RichTooltipRenderer implements ClientTooltipComponent {

    private final RichTooltipData data;
    private static final int ICON_SIZE = 9; // 图标渲染大小
    private static final int ICON_SPACING = 2; // 图标与文字的间距

    public RichTooltipRenderer(RichTooltipData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (Object segment : data.parsedSegments()) {
            if (segment instanceof String str) {
                width += font.width(str);
            } else if (segment instanceof TooltipResolver.IconDrawer) {
                width += ICON_SIZE + ICON_SPACING;
            }
        }
        return width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int currentX = x;

        for (Object segment : data.parsedSegments()) {
            if (segment instanceof String str) {
                // 渲染文本
                graphics.drawString(font, str, currentX, y + 1, data.defaultColor(), false);
                currentX += font.width(str);
            } else if (segment instanceof TooltipResolver.IconDrawer drawer) {
                // 渲染图标
                // Item 图标特殊处理 (因为 renderFakeItem 使用自己的缩放逻辑)
                // 这里为了简单，我们假设 Drawer 内部处理好了 PoseStack，或者我们在这里处理
                
                // 给图标一点间距
                // currentX += 1; 
                
                // 调用 Drawer 绘制
                // 注意：ItemRenderer 可能会重置颜色，所以图标最好在文字绘制之间小心处理
                drawer.draw(graphics, currentX, y, ICON_SIZE);
                
                currentX += ICON_SIZE + ICON_SPACING;
            }
        }
    }
}