package com.example.examplemod.util;

import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder; // 对应 ShulkerTrait 里的引用
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;     // 对应 ShulkerTrait 里的引用
import dev.xkmc.l2hostility.init.registrate.LHMiscs;                 // 注意包名是 init.registrate
import dev.xkmc.l2hostility.init.data.LHConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Optional;
import java.util.OptionalInt;

public class AttributeHelper {

    public static double getAttackDamageValue(LivingEntity entity) {
        if (!entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            return 0.0;
        }

        double baseDamage = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);

        // 如果获取不到等级，orElse(1) 默认按 1 级计算
        int level = getL2HostilityLevel(entity).orElse(1);

        double l2hMultiplier = level * LHConfig.SERVER.damageFactor.getAsDouble();

        return baseDamage * l2hMultiplier;
    }

    /**
     * 优化后：参考 ShulkerTrait.java 直接读取内存中的 Capability 对象
     */
    public static OptionalInt getL2HostilityLevel(LivingEntity entity) {
        // 1. 获取 Capability Holder (强制转换逻辑参考 ShulkerTrait)
        GeneralCapabilityHolder holder = (GeneralCapabilityHolder) LHMiscs.MOB.type();

        // 2. 获取现有的 Capability
        // 这里的泛型可能是 <Object> 或 <MobTraitCap>，根据反编译代码来看，通过 .get() 后强转最稳妥
        Optional<?> opt = holder.getExisting(entity);

        if (opt.isPresent()) {
            Object capObj = opt.get();
            // 3. 检查类型并强转 (安全检查)
            if (capObj instanceof MobTraitCap cap) {
                // 4. 直接返回 lv 字段
                return OptionalInt.of(cap.lv);
            }
        }

        return OptionalInt.empty();
    }
}