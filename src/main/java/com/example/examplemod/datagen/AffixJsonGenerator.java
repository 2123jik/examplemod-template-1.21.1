package com.example.examplemod.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AffixJsonGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // 输出根目录
    private static final String OUTPUT_DIR = "generated_affixes";

    // 固定配置
    private static final String TYPE_ID = "examplemod:spell_level";
    private static final List<String> APPLICABLE_TYPES = List.of(
            "apotheosis:boots",
            "apotheosis:chestplate",
            "apotheosis:helmet",
            "apotheosis:leggings",
            "apotheosis:melee_weapon",
            "apotheosis:trident",
            "apotheosis:bow",
            "examplemod:spellbook",
            "examplemod:staff",
            "apotheosis:breaker",
            "apotheosis:shears",
            "apotheosis:shield",
            "examplemod:back",
            "examplemod:belt",
            "examplemod:body",
            "examplemod:bracelet",
            "examplemod:charm",
            "examplemod:hands",
            "examplemod:head",
            "examplemod:necklace",
            "examplemod:ring",
            "examplemod:sheath"
            );

    public static void generate() {
        File rootDir = new File(OUTPUT_DIR);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        System.out.println("Starting Affix JSON generation (Grouped by School)...");

        // 1. 生成所有 School Affixes
        SchoolRegistry.REGISTRY.stream().forEach(school -> {
            generateSchoolAffix(rootDir, school);
        });

        // 2. 生成所有 Spell Affixes
        SpellRegistry.getEnabledSpells().forEach(spell -> {
            generateSpellAffix(rootDir, spell);
        });

        System.out.println("Affix JSON generation complete. Check folder: " + rootDir.getAbsolutePath());
    }

    private static void generateSchoolAffix(File rootDir, SchoolType school) {
        ResourceLocation id = school.getId();
        String schoolName = id.getPath(); // 获取派系名称，如 "fire", "ice"

        // --- 变更点：创建派系子文件夹 ---
        File schoolDir = new File(rootDir, schoolName);
        if (!schoolDir.exists()) {
            schoolDir.mkdirs();
        }
        // -----------------------------

        // 文件名: school_fire.json (放在 fire 文件夹下)
        String filename = "school_" + schoolName + ".json";

        AffixJson json = new AffixJson();
        json.type = TYPE_ID;
        json.definition = new AffixDefinitionData(25, 0.1f);
        json.school = id.toString();
        json.types = APPLICABLE_TYPES;

        // 数值: Max 3
        json.values.put("apotheosis:rare", new LevelWrapper(1));
        json.values.put("apotheosis:epic", new LevelWrapper(1,2));
        json.values.put("apotheosis:mythic", new LevelWrapper(1,3));

        // 写入到子文件夹中
        writeJson(new File(schoolDir, filename), json);
    }

    private static void generateSpellAffix(File rootDir, AbstractSpell spell) {
        ResourceLocation id = spell.getSpellResource();

        // --- 变更点：获取该法术的 School 并创建子文件夹 ---
        SchoolType schoolType = spell.getSchoolType();
        String schoolFolderName = (schoolType != null) ? schoolType.getId().getPath() : "misc"; // 防止空指针，虽一般不会

        File schoolDir = new File(rootDir, schoolFolderName);
        if (!schoolDir.exists()) {
            schoolDir.mkdirs();
        }
        // ---------------------------------------------

        // 文件名: spell_fireball.json (放在 fire 文件夹下)
        String filename = "spell_" + id.getPath() + ".json";

        AffixJson json = new AffixJson();
        json.type = TYPE_ID;
        json.definition = new AffixDefinitionData(10, 0.5f);
        json.spell = id.toString();
        json.types = APPLICABLE_TYPES;

        // 数值: Max 5
        json.values.put("apotheosis:common", new LevelWrapper(1));
        json.values.put("apotheosis:uncommon", new LevelWrapper(1,2));
        json.values.put("apotheosis:rare", new LevelWrapper(1,3));
        json.values.put("apotheosis:epic", new LevelWrapper(1,4));
        json.values.put("apotheosis:mythic", new LevelWrapper(1,5));

        // 写入到子文件夹中
        writeJson(new File(schoolDir, filename), json);
    }

    private static void writeJson(File file, Object object) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(object, writer);
            // System.out.println("Generated: " + file.getPath()); // 可选：打印生成的路径以便调试
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // POJO Classes (保持不变)
    // ==========================================

    private static class AffixJson {
        String type;
        AffixDefinitionData definition;
        String school;
        String spell;
        Map<String, LevelWrapper> values = new LinkedHashMap<>();
        List<String> types;
    }

    private static class AffixDefinitionData {
        String affix_type = "basic_effect";
        Weights weights;
        List<String> exclusive_set = new ArrayList<>();

        public AffixDefinitionData(int weight, float quality) {
            this.weights = new Weights(weight, quality);
        }
    }

    private static class Weights {
        int weight;
        float quality;

        public Weights(int weight, float quality) {
            this.weight = weight;
            this.quality = quality;
        }
    }

    private static class LevelWrapper {
        StepFunctionData level;

        public LevelWrapper(int fixedValue) {
            this.level = new StepFunctionData(fixedValue, 1, 0);
        }

        public LevelWrapper(int min, int max) {
            int steps = max - min;
            this.level = new StepFunctionData(min, steps, 1);
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