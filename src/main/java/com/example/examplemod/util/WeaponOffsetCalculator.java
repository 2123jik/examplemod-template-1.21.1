package com.example.examplemod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import java.util.HashMap;
import java.util.Map;

public class WeaponOffsetCalculator {

    // 缓存：Item -> 计算出的偏移量 Y
    private static final Map<Item, Double> OFFSET_CACHE = new HashMap<>();

    public static double getOffset(ItemStack stack) {
        Item item = stack.getItem();

        // 如果缓存里有，直接返回（极快）
        if (OFFSET_CACHE.containsKey(item)) {
            return OFFSET_CACHE.get(item);
        }

        // 如果没有，开始计算
        double offset = calculateOffset(stack);
        OFFSET_CACHE.put(item, offset);
        return offset;
    }

    private static double calculateOffset(ItemStack stack) {
        // 1. 基础偏移量 (针对原版 16x 剑)
        double baseOffset = -0.6; 

        // 非剑类物品，保守一点
        if (!(stack.getItem() instanceof SwordItem)) {
            return -0.3;
        }

        // 2. 获取模型
        var mc = Minecraft.getInstance();
        // 注意：这里获取的是物品在手里的模型
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        
        if (model == null) return baseOffset;

        // 3. 获取贴图 (Sprite)
        // getParticleIcon 通常就是物品的主材质
        TextureAtlasSprite sprite = model.getParticleIcon();
        
        if (sprite == null) return baseOffset;

        // 4. 获取分辨率 (宽或高，取最大值)
        // 在 1.21 中，可能需要通过 contents() 获取尺寸
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        int resolution = Math.max(width, height);

        // 5. 根据分辨率应用逻辑
        // 假设：
        // 16x -> -0.6 (原版)
        // 32x -> -0.9 (通常比较长)
        // 64x -> -1.3 (非常长的大剑)
        
        if (resolution <= 16) {
            return baseOffset;
        } else if (resolution <= 32) {
            // 32x 材质，通常比原版剑长 50% 左右
            return -0.9; 
        } else {
            // 64x 及以上，通常是巨型武器
            return -1.4;
        }
    }
    
    // 如果更换了资源包，可能需要清理缓存 (可选)
    public static void clearCache() {
        OFFSET_CACHE.clear();
    }
}