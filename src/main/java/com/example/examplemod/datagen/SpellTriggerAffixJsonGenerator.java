package com.example.examplemod.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SpellTriggerAffixJsonGenerator {
    public static final List<String> meleeSpells = List.of(
            "irons_spellbooks:divine_smite", "irons_spellbooks:frostwave", "irons_spellbooks:shockwave",
            "irons_spellbooks:volt_strike", "irons_spellbooks:burning_dash", "irons_spellbooks:flaming_strike",
            "irons_spellbooks:raise_hell", "irons_spellbooks:stomp", "irons_spellbooks:heat_surge",
            "irons_spellbooks:fang_ward", "irons_spellbooks:devour",
            "ess_requiem:rip_and_tear", "ess_requiem:necrotic_burst", "ess_requiem:spikes_of_agony",
            "ess_requiem:damnation",
            "gtbcs_geomancy_plus:tremor_spike",
            "iss_magicfromtheeast:sword_dance", "iss_magicfromtheeast:soul_burst", "iss_magicfromtheeast:calamity_cut"
    );
    public static final List<String> defensiveSpells = List.of(
            "gtbcs_geomancy_plus:tremor_step",
            "gtbcs_geomancy_plus:solar_storm",
            "irons_spellbooks:heartstop",
            "irons_spellbooks:evasion",
            "irons_spellbooks:echoing_strikes",
            "irons_spellbooks:invisibility",
            "irons_spellbooks:slow",
            "irons_spellbooks:frostbite",
            "irons_spellbooks:oakskin",
            "irons_spellbooks:gluttony",
            "irons_spellbooks:abyssal_shroud",
            "ess_requiem:pact_of_the_dead",
            "ess_requiem:strain",
            "ess_requiem:reaper",
            "ess_requiem:ebony_armor",
            "irons_spellbooks:haste",
            "ess_requiem:cursed_immortality",
            "ess_requiem:ebony_cataphract",
            "irons_spellbooks:spider_aspect",
            "ess_requiem:bastion_of_light",
            "irons_spellbooks:charge",
            "irons_spellbooks:thunderstorm"
    );
    public static final List<String> projectileSpells = List.of(
            "gtbcs_geomancy_plus:eroding_boulder", "gtbcs_geomancy_plus:dripstone_bolt",
            "gtbcs_geomancy_plus:petrivise", "gtbcs_geomancy_plus:fissure",
            "irons_spellbooks:wither_skull", "irons_spellbooks:magic_arrow", "irons_spellbooks:magic_missile",
            "irons_spellbooks:lob_creeper", "irons_spellbooks:wisp", "irons_spellbooks:icicle",
            "irons_spellbooks:snowball", "irons_spellbooks:lightning_lance", "irons_spellbooks:ball_lightning",
            "irons_spellbooks:acid_orb", "irons_spellbooks:poison_arrow", "irons_spellbooks:poison_splash",
            "irons_spellbooks:arrow_volley", "irons_spellbooks:blaze_storm", "irons_spellbooks:fireball",
            "irons_spellbooks:firebolt", "irons_spellbooks:magma_bomb", "irons_spellbooks:fire_arrow",
            "irons_spellbooks:guiding_bolt", "irons_spellbooks:sonic_boom", "irons_spellbooks:eldritch_blast",
            "irons_spellbooks:acupuncture", "irons_spellbooks:blood_needles", "irons_spellbooks:blood_slash",
            "irons_spellbooks:fang_strike", "irons_spellbooks:firecracker", "irons_spellbooks:sunbeam",
            "irons_spellbooks:ray_of_frost", "irons_spellbooks:lightning_bolt", "irons_spellbooks:earthquake",
            "irons_spellbooks:sculk_tentacles", "irons_spellbooks:ice_block", "irons_spellbooks:ice_spikes",
            "irons_spellbooks:chain_lightning", "irons_spellbooks:blight", "irons_spellbooks:firefly_swarm",
            "irons_spellbooks:scorch", "irons_spellbooks:chain_creeper", "irons_spellbooks:sacrifice",
            "iss_magicfromtheeast:dragon_glide", "iss_magicfromtheeast:soul_catalyst",
            "iss_magicfromtheeast:phantom_charge", "iss_magicfromtheeast:bone_hands",
            "iss_magicfromtheeast:jade_judgement",
            "ess_requiem:corpse_explosion"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // 输出目录
    private static final String OUTPUT_DIR = "generated_affixes/spell_triggers";
    private static final String TYPE_ID = "examplemod:spell_trigger"; // 请确保这里的 ID 与你的 Mod 注册的 Affix 序列化器 ID 一致

    // 完整的物品列表
    private static final List<String> ALL_POSSIBLE_TYPES = List.of(
            "apotheosis:boots", "apotheosis:chestplate", "apotheosis:helmet", "apotheosis:leggings",
            "apotheosis:melee_weapon", "apotheosis:trident", "apotheosis:bow",
            "examplemod:spellbook", "examplemod:staff", // 假设你有这些自定义物品
            "apotheosis:breaker", "apotheosis:shears", "apotheosis:shield",
            "examplemod:back", "examplemod:belt", "examplemod:body", "examplemod:bracelet",
            "examplemod:charm", "examplemod:hands", "examplemod:head", "examplemod:necklace",
            "examplemod:ring", "examplemod:sheath"
    );

    public enum TriggerType {
        SPELL_DAMAGE, SPELL_HEAL, MELEE_HIT, PROJECTILE_HIT, HURT, SHIELD
    }

    public enum TargetType {
        SELF, TARGET
    }

    public static void generate() {
        File root = new File(OUTPUT_DIR);
        if (!root.exists()) root.mkdirs();

        System.out.println("Starting Spell Trigger Affix generation (CATEGORIZED)...");

        // 1. 近战攻击触发 (Trigger: MELEE_HIT, Target: TARGET)
        // 适用于：剑技、震荡波、近身爆发

        generateList(root, meleeSpells, TriggerType.MELEE_HIT, TargetType.TARGET);


        // 2. 弹射物/远程攻击触发 (Trigger: PROJECTILE_HIT & SPELL_DAMAGE, Target: TARGET)
        // 适用于：火球、箭矢、射线、远程召唤
        // 注意：这里同时生成 PROJECTILE_HIT 和 SPELL_DAMAGE 两种触发，因为它们通常可以互换使用

        generateList(root, projectileSpells, TriggerType.PROJECTILE_HIT, TargetType.TARGET);
        generateList(root, projectileSpells, TriggerType.SPELL_DAMAGE, TargetType.TARGET);


        // 3. 防御/受击触发 (Trigger: HURT, Target: SELF)
        // 适用于：受到伤害时触发治疗、隐身、传送、护盾

        generateList(root, defensiveSpells, TriggerType.SPELL_HEAL, TargetType.SELF);


        System.out.println("Generation complete at: " + root.getAbsolutePath());
    }

    /**
     * 为给定的法术列表生成指定触发条件的 JSON 文件
     */
    private static void generateList(File root, List<String> spellIds, TriggerType trigger, TargetType target) {
        for (String spellStr : spellIds) {
            ResourceLocation spellId = ResourceLocation.parse(spellStr);
            createAffixFile(root, spellId, trigger, target);
        }
    }

    private static void createAffixFile(File root, ResourceLocation spellId, TriggerType trigger, TargetType target) {
        // 创建目录: generated_affixes/spell_triggers/{trigger}/{target}/
        String subPath = trigger.name().toLowerCase() + "/" + target.name().toLowerCase();
        File dir = new File(root, subPath);
        if (!dir.exists()) dir.mkdirs();

        // 文件名: namespace_path.json (防止不同模组同名法术冲突，或者直接用path)
        // 这里使用 spellId.getPath() 与原逻辑保持一致
        File file = new File(dir, spellId.getPath() + ".json");

        TriggerAffixJson json = new TriggerAffixJson();
        json.type = TYPE_ID;
        // 调整权重和稀有度以适应平衡性
        json.definition = new AffixDefinitionData("ability", 15, 0.5f);
        json.spell = spellId.toString();
        json.trigger = trigger.name();
        json.target = target.name();
        json.types = ALL_POSSIBLE_TYPES;

        // 数值配置：根据稀有度提升等级，减少冷却
        // 格式: min, steps, step, cooldown(ticks)
        json.values.put("apotheosis:common", new TriggerValueData(1, 1, 0, 600));
        json.values.put("apotheosis:uncommon", new TriggerValueData(1, 2, 0, 480));
        json.values.put("apotheosis:rare", new TriggerValueData(2, 3, 1, 360));
        json.values.put("apotheosis:epic", new TriggerValueData(3, 4, 1, 240));
        json.values.put("apotheosis:mythic", new TriggerValueData(5, 5, 1, 120));

        writeJson(file, json);
    }

    private static void writeJson(File file, Object object) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(object, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= POJO 结构 =================

    private static class TriggerAffixJson {
        String type;
        AffixDefinitionData definition;
        String spell;
        String trigger;
        String target;
        Map<String, TriggerValueData> values = new LinkedHashMap<>();
        List<String> types;
    }

    private static class AffixDefinitionData {
        String affix_type;
        Weights weights;
        List<String> exclusive_set = new ArrayList<>();

        public AffixDefinitionData(String type, int weight, float quality) {
            this.affix_type = type;
            this.weights = new Weights(weight, quality);
        }
    }

    private static class Weights {
        int weight;
        float quality;
        public Weights(int w, float q) { this.weight = w; this.quality = q; }
    }

    private static class TriggerValueData {
        StepFunctionData level;
        int cooldown;

        public TriggerValueData(int min, int steps, int step, int cooldown) {
            this.level = new StepFunctionData((float)min, steps, (float)step);
            this.cooldown = cooldown;
        }
    }

    private static class StepFunctionData {
        float min;
        int steps;
        float step;

        public StepFunctionData(float min, int steps, float step) {
            this.min = min;
            this.steps = steps;
            this.step = step;
        }
    }
}