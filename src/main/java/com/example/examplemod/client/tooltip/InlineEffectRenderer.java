package com.example.examplemod.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;

public class InlineEffectRenderer implements ClientTooltipComponent {

    private final InlineEffectData data;
    
    // 图标大小设为9，和默认字体高度一致，看起来最舒服
    private static final int ICON_SIZE = 9; 

    public InlineEffectRenderer(InlineEffectData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return 10; // 稍微给一点高度余量
    }

    @Override
    public int getWidth(Font font) {
        // 总宽度 = 前缀宽 + 图标宽 + 间距 + 后缀宽
        return font.width(data.prefix()) + ICON_SIZE + 2 + font.width(data.suffix());
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        // 1. 渲染前缀 (例如 "击中后造成 ")
        graphics.drawString(font, data.prefix(), x, y + 1, data.color(), false);

        // 计算图标绘制的 X 坐标
        int xAfterPrefix = x + font.width(data.prefix());

        // 2. 渲染图标
        // 绑定方块图集(药水图标在这)
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enableBlend();
        
        // 获取药水对应的 Sprite
        TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(data.effect());

        PoseStack pose = graphics.pose();
        pose.pushPose();
        // 移动到文字后面
        pose.translate(xAfterPrefix, y, 0); 
        // 缩放：原图通常是18x18，我们需要缩小到 9x9 (0.5倍)
        pose.scale(0.5f, 0.5f, 1.0f);
        
        // 绘制图标 (0,0 是相对坐标)
        graphics.blit(0, 0, 0, 18, 18, sprite);
        
        pose.popPose();

        // 3. 渲染后缀 (例如 "中毒 II (0:05)")
        // 注意：后缀的颜色这里用了药水原本的颜色，让它显眼一点，或者你可以改回 data.color()
        int effectColor = data.effect().value().getColor();
        
        // 如果想要文字颜色和普通文本一样，就用 data.color()，这里我用了药水色高亮名字
        graphics.drawString(font, data.suffix(), xAfterPrefix + ICON_SIZE + 2, y + 1, effectColor, false);
    }
}