package com.example.examplemod.client.tooltip;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AttributeVisualMapping {

    // 存储 属性ID -> 药水效果ID 的映射
    private static final Map<ResourceLocation, ResourceLocation> ID_MAPPING = new HashMap<>();
    // 缓存解析后的结果
    private static final Map<Holder<Attribute>, Holder<MobEffect>> CACHED_MAPPING = new HashMap<>();

    static {
        register("aces_spell_utils:evasive", "");
        register("aces_spell_utils:goliath_slayer", "");
        register("aces_spell_utils:hunger_steal", "");
        register("aces_spell_utils:mana_rend", "");
        register("aces_spell_utils:mana_steal", "");
        register("aces_spell_utils:spell_res_penetration", "");
        register("apothic_attributes:armor_pierce", "apothic_attributes:sundering");
        register("apothic_attributes:armor_shred", "l2complements:armor_corrosion");
        register("apothic_attributes:arrow_damage", "");
        register("apothic_attributes:arrow_velocity", "");
        register("apothic_attributes:cold_damage", "twilightdelight:frozen_range");
        register("apothic_attributes:crit_chance", "dungeonsdelight:decisive");//todo
        register("apothic_attributes:crit_damage", "");
        register("apothic_attributes:current_hp_damage", "");
        register("apothic_attributes:dodge_chance", "irons_spellbooks:evasion");
        register("apothic_attributes:draw_speed", "l2archery:fast_pulling");
        register("apothic_attributes:elytra_flight", "irons_spellbooks:angel_wings");
        register("apothic_attributes:experience_gained", "apothic_attributes:knowledge");
        register("apothic_attributes:fire_damage", "twilightdelight:fire_range");
        register("apothic_attributes:ghost_health", "");
        register("apothic_attributes:healing_received", "apothic_attributes:vitality");
        register("apothic_attributes:life_steal", "");
        register("apothic_attributes:mining_speed", "examplemod:mining_speed");
        register("apothic_attributes:overheal", "irons_spellbooks:fortify");
        register("apothic_attributes:projectile_damage", "examplemod:projectile_damage");
        register("apothic_attributes:prot_pierce", "gtbcs_geomancy_plus:erode");
        register("apothic_attributes:prot_shred", "gtbcs_geomancy_plus:erode");
        register("artifacts:generic.attack_burning_duration", "examplemod:attack_burning_duration");
        register("artifacts:generic.drinking_speed", "examplemod:drinking_speed");
        register("artifacts:generic.eating_speed", "examplemod:eating_speed");
        register("artifacts:generic.flatulence", "examplemod:flatulence");
        register("artifacts:generic.invincibility_ticks", "l2complements:cleanse");
        register("artifacts:generic.mount_speed", "examplemod:mount_speed");
        register("artifacts:generic.movement_speed_on_snow", "");
        register("artifacts:generic.slip_resistance", "");
        register("artifacts:generic.sprinting_speed", "");
        register("artifacts:generic.sprinting_step_height", "");
        register("artifacts:player.entity_experience", "apothic_attributes:knowledge");
        register("artifacts:player.villager_reputation", "minecraft:hero_of_the_village");
        register("gtbcs_geomancy_plus:geo_magic_resist", "");
        register("gtbcs_geomancy_plus:geo_spell_power", "");
        register("irons_spellbooks:blood_magic_resist", "");
        register("irons_spellbooks:blood_spell_power", "");
        register("irons_spellbooks:cast_time_reduction", "");
        register("irons_spellbooks:casting_movespeed", "");
        register("irons_spellbooks:cooldown_reduction", "");
        register("irons_spellbooks:eldritch_magic_resist", "");
        register("irons_spellbooks:eldritch_spell_power", "");
        register("irons_spellbooks:ender_magic_resist", "");
        register("irons_spellbooks:ender_spell_power", "");
        register("irons_spellbooks:evocation_magic_resist", "");
        register("irons_spellbooks:evocation_spell_power", "");
        register("irons_spellbooks:fire_magic_resist", "");
        register("irons_spellbooks:fire_spell_power", "");
        register("irons_spellbooks:holy_magic_resist", "");
        register("irons_spellbooks:holy_spell_power", "");
        register("irons_spellbooks:ice_magic_resist", "");
        register("irons_spellbooks:ice_spell_power", "");
        register("irons_spellbooks:lightning_magic_resist", "");
        register("irons_spellbooks:lightning_spell_power", "");
        register("irons_spellbooks:mana_regen", "");
        register("irons_spellbooks:max_mana", "");
        register("irons_spellbooks:nature_magic_resist", "");
        register("irons_spellbooks:nature_spell_power", "");
        register("irons_spellbooks:spell_power", "");
        register("irons_spellbooks:spell_resist", "");
        register("irons_spellbooks:summon_damage", "");
        register("iss_magicfromtheeast:dune_magic_resist", "");
        register("iss_magicfromtheeast:dune_spell_power", "");
        register("iss_magicfromtheeast:spirit_magic_resist", "");
        register("iss_magicfromtheeast:spirit_spell_power", "");
        register("iss_magicfromtheeast:symmetry_magic_resist", "");
        register("iss_magicfromtheeast:symmetry_spell_power", "");
        register("l2damagetracker:bow_strength", "");
        register("l2damagetracker:crit_damage", "");
        register("l2damagetracker:crit_rate", "");
        register("l2damagetracker:damage_absorption", "");
        register("l2damagetracker:damage_reduction", "minecraft:resistance");
        register("l2damagetracker:explosion_damage", "");
        register("l2damagetracker:fire_damage", "");
        register("l2damagetracker:freezing_damage", "");
        register("l2damagetracker:lightning_damage", "");
        register("l2damagetracker:magic_damage", "");
        register("l2damagetracker:regen", "");
        register("l2hostility:extra_difficulty", "");
        register("l2weaponry:reflect_time", "");
        register("l2weaponry:shield_defense", "");
        register("modulargolems:golem_jump", "");
        register("modulargolems:golem_regen", "");
        register("modulargolems:golem_size", "");
        register("modulargolems:golem_sweep", "");
        register("neoforge:creative_flight", "");
        register("neoforge:nametag_distance", "");
        register("neoforge:swim_speed", "minecraft:dolphins_grace");
        register("minecraft:generic.armor", "examplemod:armor");
        register("minecraft:generic.armor_toughness", "examplemod:armor_toughness");
        register("minecraft:generic.attack_damage", "minecraft:strength");
        register("minecraft:generic.attack_knockback", "examplemod:attack_knockback");
        register("minecraft:generic.attack_speed", "youkaishomecoming:tea_polyphenols");
        register("minecraft:generic.burning_time", "");
        register("minecraft:generic.explosion_knockback_resistance", "");
        register("minecraft:generic.fall_damage_multiplier", "");
        register("minecraft:generic.gravity", "");
        register("minecraft:generic.jump_strength", "minecraft:jump_boost");
        register("minecraft:generic.knockback_resistance", "examplemod:knockback_resistance");
        register("minecraft:generic.luck", "minecraft:luck");
        register("minecraft:generic.max_absorption", "minecraft:absorption");
        register("minecraft:generic.max_health", "minecraft:health_boost");
        register("minecraft:generic.movement_efficiency", "");
        register("minecraft:generic.movement_speed", "minecraft:speed");
        register("minecraft:generic.oxygen_bonus", "minecraft:water_breathing");
        register("minecraft:generic.safe_fall_distance", "");
        register("minecraft:generic.scale", "fruitsdelight:shrinking");
        register("minecraft:generic.step_height", "");
        register("minecraft:generic.water_movement_efficiency", "");
        register("minecraft:player.block_break_speed", "");
        register("minecraft:player.block_interaction_range", "examplemod:block_interaction_range");
        register("minecraft:player.entity_interaction_range", "");
        register("minecraft:player.mining_efficiency", "");
        register("minecraft:player.sneaking_speed", "");
        register("minecraft:player.submerged_mining_speed", "");
        register("minecraft:player.sweeping_damage_ratio", "");
        register("minecraft:generic.flying_speed", "");
        register("minecraft:generic.follow_range", "");
    }

    private static void register(String attributeId, String effectId) {
        ResourceLocation attrLoc = ResourceLocation.tryParse(attributeId);
        ResourceLocation effectLoc = ResourceLocation.tryParse(effectId);
        if (attrLoc != null && effectLoc != null) {
            ID_MAPPING.put(attrLoc, effectLoc);
        }
    }

    /**
     * 获取属性对应的药水效果 Holder
     */
    public static Optional<Holder<MobEffect>> getAssociatedEffect(Holder<Attribute> attribute) {
        // 如果已经缓存了，直接返回
        if (CACHED_MAPPING.containsKey(attribute)) {
            return Optional.ofNullable(CACHED_MAPPING.get(attribute));
        }

        ResourceLocation attrId = attribute.unwrapKey().map(key -> key.location()).orElse(null);
        if (attrId != null) {
            ResourceLocation effectId = ID_MAPPING.get(attrId);
            if (effectId != null) {
                Optional<Holder.Reference<MobEffect>> effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(effectId);
                if (effectHolder.isPresent()) {
                    CACHED_MAPPING.put(attribute, effectHolder.get());
                    return Optional.of(effectHolder.get());
                }
            }
        }

        // 存入 null 避免重复查找失败的 ID
        CACHED_MAPPING.put(attribute, null);
        return Optional.empty();
    }
}