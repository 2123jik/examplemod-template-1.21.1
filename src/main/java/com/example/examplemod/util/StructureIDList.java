package com.example.examplemod.util;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.List;

import static com.example.examplemod.ExampleMod.MODID;

public class StructureIDList {

    // 您拥有的所有结构体文件的 ResourceLocation 列表
    public static final List<ResourceLocation> ALL_STRUCTURES = List.of(
            // 示例中已使用的结构
            ResourceLocation.fromNamespaceAndPath(MODID, "structures/abandoned_temple/abandoned_road_0.nbt"),
            // 添加更多结构路径...
            ResourceLocation.fromNamespaceAndPath(MODID, "structures/tower/tower_base.nbt"),
            ResourceLocation.fromNamespaceAndPath(MODID, "structures/hut/small_hut.nbt")
            // ...
    );

    /**
     * 从列表中随机选择一个结构体 ID
     */
    public static ResourceLocation getRandomStructure(RandomSource random) {
        return ALL_STRUCTURES.get(random.nextInt(ALL_STRUCTURES.size()));
    }
}