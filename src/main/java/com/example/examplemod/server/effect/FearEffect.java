package com.example.examplemod.server.effect;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.example.examplemod.ExampleMod.loc;

public class FearEffect extends MobEffect {
    public FearEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                loc( "fear_speed_modifier"),
                -0.25,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
    }

    /**
     * 此方法仅在效果第一次被施加时调用一次。
     * 我们在这里实现“瞬间180度转身”的效果。
     */
    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        // 寻找最近的玩家作为恐惧源
        Player nearestPlayer = entity.level().getNearestPlayer(entity, 16.0);
        if (nearestPlayer != null) {
            // 强制生物立即朝向远离玩家的方向
            forceLookAway(entity, nearestPlayer);
        }
        super.onEffectStarted(entity, amplifier);
    }

    /**
     * 此方法在效果持续期间的每一刻都会被调用。
     * 我们在这里实现持续的“推开”和“朝向修正”。
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Player nearestPlayer = entity.level().getNearestPlayer(entity, 16.0);
        if (nearestPlayer != null) {
            // --- 1. 施加推力 ---
            Vec3 awayFromPlayer = entity.position().subtract(nearestPlayer.position()).normalize();

            // 这个值代表“推力”的强度，需要仔细调试
            double pushStrength = 0.02D;

            // 计算推力向量，只在水平方向上施加
            Vec3 pushVector = new Vec3(awayFromPlayer.x * pushStrength, 0, awayFromPlayer.z * pushStrength);

            // 将推力“添加”到生物当前的速度上
            entity.addDeltaMovement(pushVector);


            // --- 2. 强制朝向 ---
            forceLookAway(entity, nearestPlayer);
        }

        return true;
    }

    /**
     * 控制 applyEffectTick 的执行频率。
     * 返回 true 意味着每一游戏刻都执行，以产生最流畅、最强的控制效果。
     */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    /**
     * 一个辅助方法，用于计算并应用正确的“背对”朝向。
     * @param entity  需要被转向的生物
     * @param target  恐惧源（玩家）
     */
    private void forceLookAway(LivingEntity entity, Player target) {
        // 再次计算远离目标的向量，确保方向最新
        Vec3 awayVector = entity.position().subtract(target.position());

        // 使用 atan2 计算正确的水平旋转角度 (Yaw)
        // Minecraft 的 Yaw 计算需要从结果中减去90度
        float yaw = (float) (Mth.atan2(awayVector.z, awayVector.x) * (180.0D / Math.PI)) - 90.0F;

        // 强制设置生物的朝向
        // setYRot 控制实体模型本身朝向
        // setYHeadRot 控制实体头部的朝向
        // setYBodyRot 控制身体的朝向（对某些复杂动画很重要）
        // 全部设置可以确保视觉效果上完全统一
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
    }
}