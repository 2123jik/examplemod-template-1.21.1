package com.example.examplemod.util;

import com.example.examplemod.register.ModAttachments;
import com.example.examplemod.register.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EffectUtils {

    /**
     * 施加燃烧咒，并进行凶手快照记录
     */
    public static void applyCombustionCurse(LivingEntity target, Entity attacker, int durationTick, int fireSeconds) {
        if (target.level().isClientSide) return;

        // 1. 施加效果
        target.addEffect(new MobEffectInstance(ModEffects.COMBUSTION_CURSE, durationTick, 0));

        // 2. 点燃目标 (需要在 EntityMixin 中实现堆叠逻辑)
        target.igniteForSeconds(fireSeconds);

        // 3. 【快照】：将凶手的 UUID 写入受害者的附件
        if (attacker != null) {
            target.setData(ModAttachments.LAST_ATTACKER_UUID.get(), Optional.of(attacker.getUUID()));
        }
    }
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