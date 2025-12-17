package com.example.examplemod.client.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * 用于在 Tooltip 中显示一张图片的纯数据载体
 * @param texture 图片的资源路径
 * @param width   渲染宽度
 * @param height  渲染高度
 * @param u       纹理的 U 坐标 (通常为0)
 * @param v       纹理的 V 坐标 (通常为0)
 * @param texW    整个纹理文件的宽度 (如果是256x256的图就填256)
 * @param texH    整个纹理文件的高度
 */
public record ImageTooltipData(ResourceLocation texture, int width, int height, int u, int v, int texW, int texH) implements TooltipComponent {
    
    // 简便构造函数：默认画整张图
    public ImageTooltipData(ResourceLocation texture, int width, int height) {
        this(texture, width, height, 0, 0, width, height);
    }
}