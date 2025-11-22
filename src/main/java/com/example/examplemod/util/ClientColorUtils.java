package com.example.examplemod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.NativeImage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT) // 标记为仅客户端
public class ClientColorUtils {

    public static int getItemColor(ItemStack stack) {
        if (stack.isEmpty()) return 0xFFFFFF;

        Minecraft mc = Minecraft.getInstance();

        // 2. 获取模型和材质
        try {
            BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, null, 0);
            TextureAtlasSprite sprite = model.getParticleIcon();

            if (sprite == null) return 0xFFFFFF;

            // 3. 计算平均颜色 (基于之前的 SpriteContents 逻辑)
            return calculateAverageColorFromSprite(sprite);
        } catch (Exception e) {
            // 防止任何渲染错误导致游戏崩溃
            return 0xFFFFFF;
        }
    }

    private static int calculateAverageColorFromSprite(TextureAtlasSprite sprite) {
        SpriteContents contents = sprite.contents();
        NativeImage image = contents.getOriginalImage();

        int width = contents.width();
        int height = contents.height();

        long totalRed = 0, totalGreen = 0, totalBlue = 0;
        int count = 0;

        // 简单的步长优化：不必遍历每个像素，每隔一个像素采样一次可以提升4倍性能，且对平均色影响极小
        int step = 1;

        for (int x = 0; x < width; x += step) {
            for (int y = 0; y < height; y += step) {
                int color = image.getPixelRGBA(x, y);
                int alpha = (color >> 24) & 0xFF;

                if (alpha > 10) { // 忽略透明部分
                    totalRed   += (color & 0xFF);
                    totalGreen += ((color >> 8) & 0xFF);
                    totalBlue  += ((color >> 16) & 0xFF);
                    count++;
                }
            }
        }

        if (count == 0) return 0xFFFFFF;

        return (int) ((totalRed / count) << 16 | (totalGreen / count) << 8 | (totalBlue / count));
    }
}