package com.example.examplemod.util;

import dev.xkmc.l2hostility.init.data.LHConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.OptionalInt;

public class AttributeHelper {

    public static double getAttackDamageValue(LivingEntity entity) {
        if (!entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            return 0.0;
        }

        double baseDamage = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);

        int level = getL2HostilityLevel(entity).orElse(1);

        double l2hMultiplier = level * LHConfig.SERVER.damageFactor.getAsDouble();

        return baseDamage * l2hMultiplier;
    }

    public static OptionalInt getL2HostilityLevel(LivingEntity entity) {
        CompoundTag rootTag = entity.saveWithoutId(new CompoundTag());

        if (rootTag.contains("neoforge:attachments", Tag.TAG_COMPOUND)) {
            CompoundTag attachmentsTag = rootTag.getCompound("neoforge:attachments");
            if (attachmentsTag.contains("l2hostility:mob", Tag.TAG_COMPOUND)) {
                CompoundTag mobTag = attachmentsTag.getCompound("l2hostility:mob");
                if (mobTag.contains("lv", Tag.TAG_INT)) {
                    return OptionalInt.of(mobTag.getInt("lv"));
                }
            }
        }

        return OptionalInt.of(1);
    }

}