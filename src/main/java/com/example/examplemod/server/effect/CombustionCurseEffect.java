package com.example.examplemod.server.effect;


import com.example.examplemod.register.ModAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public class CombustionCurseEffect extends MobEffect {

    public CombustionCurseEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 不需要每 tick 造成伤害，我们只在结束时结算
        return false;
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        return false;
    }

    /**
     * 对应源码第 57 行：public void onMobRemoved(LivingEntity livingEntity, int amplifier, Entity.RemovalReason reason)
     * 当效果被移除（时间到了、喝奶、死亡等）时触发。
     */
    @Override
    public void onMobRemoved(LivingEntity victim, int amplifier, Entity.RemovalReason reason) {
        super.onMobRemoved(victim, amplifier, reason);

        if (victim.level().isClientSide) return;

        // 如果是因为实体死亡导致的移除，通常不需要再结算伤害了，否则会鞭尸
        if (reason == Entity.RemovalReason.KILLED) return;

        int fireTicks = victim.getRemainingFireTicks();
        if (fireTicks > 0) {
            // 计算伤害：假设每 20 tick (1秒) 点燃造成 1 点伤害
            float damage = (float) fireTicks / 20.0F * (amplifier + 1);

            // --- 核心逻辑：找回凶手 ---
            Entity attacker = null;

            // 从【受害者实体】的附件中读取 UUID
            Optional<UUID> uuidOpt = victim.getData(ModAttachments.LAST_ATTACKER_UUID.get());

            if (uuidOpt.isPresent() && victim.level() instanceof ServerLevel serverLevel) {
                // 在服务器世界查找实体
                attacker = serverLevel.getEntity(uuidOpt.get());
            }

            // --- 构建伤害源 ---
            DamageSource source;
            if (attacker instanceof LivingEntity livingAttacker) {
                // 使用 FIREBALL 类型 (minecraft:fireball)
                // 参数1: directEntity (投射物，这里没有，填null)
                // 参数2: causingEntity (真正的凶手，填 livingAttacker)
                // 这样系统会判定：这是 attacker 造成的伤害，从而触发你的僵尸增伤逻辑
                source = new DamageSource(
                        victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FIREBALL),
                        null,
                        livingAttacker
                );
            } else {
                // 找不到凶手（已消失/未记录），回退到普通火焰伤害
                source = victim.damageSources().onFire();
            }

            // 造成伤害
            victim.hurt(source, damage);

            // 结算完成，熄灭火焰并清除记录
            victim.clearFire();
            victim.setData(ModAttachments.LAST_ATTACKER_UUID.get(), Optional.empty());
        }
    }
}