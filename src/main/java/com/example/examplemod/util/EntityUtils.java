package com.example.examplemod.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import java.util.List;
import java.util.stream.Collectors;

public class EntityUtils {

    public static List<String> getAllLivingEntityPaths() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
            // 1. 过滤：检查是否有默认属性。
            // 只有 LivingEntity (玩家、怪物、动物、盔甲架) 会注册属性。
            // 投掷物、掉落物、交通工具等返回 false。
            .filter(DefaultAttributes::hasSupplier) 
            
            // 2. 获取 ResourceLocation (例如 minecraft:wolf)
            .map(BuiltInRegistries.ENTITY_TYPE::getKey)
            
            // 3. 提取 Path 部分 (例如 wolf)
            .map(ResourceLocation::getPath)
            
            // 4. 收集为 List
            .collect(Collectors.toList());
    }
}