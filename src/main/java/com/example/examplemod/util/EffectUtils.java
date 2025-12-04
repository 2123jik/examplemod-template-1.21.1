package com.example.examplemod.util;

import com.example.examplemod.init.ModAttachments;
import com.example.examplemod.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

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
}