package com.example.examplemod.server.effect;

import com.example.examplemod.register.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static com.example.examplemod.ExampleMod.MODID;
import static com.example.examplemod.register.ModEffects.MAKEN_POWER;
import static dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes.LIFE_STEAL;
import static net.minecraft.world.entity.ai.attributes.Attributes.*;

public class MakenPowerEffect extends MobEffect {
    public MakenPowerEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(
                LIFE_STEAL,
                ResourceLocation.fromNamespaceAndPath(MODID, "makenpowereffect"),
                0.20,
                AttributeModifier.Operation.ADD_VALUE
        );
        this.addAttributeModifier(MOVEMENT_SPEED,ResourceLocation.fromNamespaceAndPath(MODID, "makenpowereffect_movement_speed"),0.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(ATTACK_DAMAGE,ResourceLocation.fromNamespaceAndPath(MODID, "makenpowereffect_attack_damage"),0.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(ENTITY_INTERACTION_RANGE,ResourceLocation.fromNamespaceAndPath(MODID, "makenpowereffect_entity_interaction_range"),3,AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(MAX_ABSORPTION,ResourceLocation.fromNamespaceAndPath(MODID, "makenpowereffect_max_absorption"),204800,AttributeModifier.Operation.ADD_VALUE);
    }
    /**
     * 当拥有此效果的实体击杀了另一个生物时，从此事件处理器调用此方法。
     * @param killer 击杀者，即拥有此效果的玩家
     */
    public static void onKill(Player killer) {
        // --- 延长自身效果时间的逻辑 ---
        MobEffectInstance currentInstance = killer.getEffect(MAKEN_POWER);
        if (currentInstance != null) {
            int currentDuration = currentInstance.getDuration();
            // 增加5秒（100 ticks），总时长不超过30秒（600 ticks）
            int newDuration = Math.min(currentDuration + 100, 600);

            // 创建一个新的效果实例并应用，保留原有属性
            killer.addEffect(new MobEffectInstance(
                    MAKEN_POWER,
                    newDuration,
                    currentInstance.getAmplifier(),
                    currentInstance.isAmbient(),
                    currentInstance.isVisible(),
                    currentInstance.showIcon()
            ));
        }

        // --- 对周围实体施加恐惧效果的逻辑 ---
        Level level = killer.getCommandSenderWorld();
        AABB area = new AABB(killer.blockPosition()).inflate(8);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != killer && e.isAlive());
        for (LivingEntity victim : nearbyEntities) {
            victim.addEffect(new MobEffectInstance(ModEffects.FEAR, 60, 0)); // 3秒
        }
    }

    @Override
    public boolean applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        return super.applyEffectTick(pLivingEntity, pAmplifier);
    }
}