package com.example.examplemod.client.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public class InlineAttributeItemRenderer implements ClientTooltipComponent {

    private final InlineAttributeDataItem data;
    private static final int ICON_SIZE = 10; // 这里的 10 是我们希望渲染在屏幕上的大小

    public InlineAttributeItemRenderer(InlineAttributeDataItem data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public int getWidth(Font font) {
        return font.width(data.prefix()) + ICON_SIZE + 2 + font.width(data.suffix());
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        // 1. 渲染前缀 (例如 "+ 3 ")
        graphics.drawString(font, data.prefix(), x, y + 1, data.color(), false);

        int xAfterPrefix = x + font.width(data.prefix());

        // 2. 渲染图标 (物品)
        ItemStack iconStack = AttributeIconMapper.getIcon(data.attribute());

        PoseStack pose = graphics.pose();
        pose.pushPose();
        
        // 移动到目标位置
        pose.translate(xAfterPrefix, y, 0);
        
        // 缩放逻辑：
        // 物品默认渲染是 16x16，如果你想要 10x10 的大小，需要缩放 10/16 = 0.625
        float scale = (float) ICON_SIZE / 16.0f;
        
        // 这里的 translate 和 scale 顺序很重要
        // renderFakeItem 默认画在 (0,0)，所以我们要先位移再缩放，或者先缩放再位移(坐标也缩放)
        // 最稳妥是用 PoseStack 缩放，然后用 renderItem
        pose.scale(scale, scale, 1.0f);

        // 注意：renderFakeItem 不需要传入 x, y，因为它受 PoseStack 影响
        // 但是 renderFakeItem 内部会重置一些状态，可能会导致层级问题，如果出现图标在文字下面，需要调整 z-index
        graphics.renderFakeItem(iconStack, 0, 0);

        pose.popPose();

        // 3. 渲染后缀 (例如 "攻击伤害")
        // 使用原有的颜色，通常属性是蓝色或者绿色的
        graphics.drawString(font, data.suffix(), xAfterPrefix + ICON_SIZE + 2, y + 1, data.color(), false);
    }
}