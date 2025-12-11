package com.example.examplemod.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.InventoryMenu;

public class InlineAttributeRenderer implements ClientTooltipComponent {

    private final InlineAttributeData data;
    // 属性图标建议稍微小一点点，或者和文字对齐，9x9 是经典尺寸
    private static final int ICON_SIZE = 9; 

    public InlineAttributeRenderer(InlineAttributeData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public int getWidth(Font font) {
        // 如果该属性没有对应的药水映射，就不留空位，直接只显示文字（或者你可以给个默认图标）
        boolean hasIcon = AttributeVisualMapping.getAssociatedEffect(data.attribute()).isPresent();
        int iconWidth = hasIcon ? (ICON_SIZE + 2) : 0;
        
        return font.width(data.prefix()) + iconWidth + font.width(data.suffix());
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        // 1. 渲染前缀 (例如 "+ 5 ")
        graphics.drawString(font, data.prefix(), x, y + 1, data.color(), false);

        int xCurrent = x + font.width(data.prefix());

        // 2. 渲染图标 (如果有映射)
        var effectOptional = AttributeVisualMapping.getAssociatedEffect(data.attribute());
        
        if (effectOptional.isPresent()) {
            Holder<MobEffect> effect = effectOptional.get();
            
            // 获取药水图集中的 Sprite
            TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);

            // 绑定纹理
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.enableBlend();

            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(xCurrent, y, 0);
            
            // 药水图标原始是 18x18，我们需要 9x9 -> 缩放 0.5
            float scale = (float) ICON_SIZE / 18.0f;
            pose.scale(scale, scale, 1.0f);
            
            // 绘制 Sprite
            graphics.blit(0, 0, 0, 18, 18, sprite);
            
            pose.popPose();
            
            // 推进坐标
            xCurrent += (ICON_SIZE + 2);
        }

        // 3. 渲染后缀 (属性名)
        // 既然你提到药水有颜色，我们可以选择：
        // 方案 A: 使用原始文本颜色 (通常是灰色或蓝色)
        // 方案 B: 使用药水代表色 (例如力量是红色) -> 这里演示方案 B，让它看起来更 RPG
        
        int suffixColor = data.color(); // 默认为 data 传入的颜色
        
        // 如果想强制使用药水颜色，解开下面这行注释：
        // if (effectOptional.isPresent()) suffixColor = effectOptional.get().value().getColor();

        graphics.drawString(font, data.suffix(), xCurrent, y + 1, suffixColor, false);
    }
}