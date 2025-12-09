package com.example.examplemod.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ApotheosisDataDumper {

    public static void dumpData() {
        JsonObject root = new JsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // 1. 导出所有药水效果 (Mob Effects)
        JsonArray effects = new JsonArray();
        BuiltInRegistries.MOB_EFFECT.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> {
                    JsonObject effect = new JsonObject();
                    MobEffect mobEffect = entry.getValue();
                    ResourceLocation id = entry.getKey().location();

                    effect.addProperty("id", id.toString());
                    effect.addProperty("name", mobEffect.getDisplayName().getString());

                    // --- 增强：兼容特殊MOD的描述获取逻辑 ---
                    // 获取基础语言键 (例如: effect.ess_requiem.wretch)
                    String baseKey = mobEffect.getDescriptionId();

                    // 定义可能的描述后缀，按优先级排序
                    // 1. .description (原版及大多数规范模组，如你提供的例子)
                    // 2. .desc (旧版或其他模组习惯)
                    // 3. .tooltip (部分装备或特殊效果模组)
                    // 4. (无后缀) 直接使用 key 本身 (极少情况)
                    List<String> textKeys = Arrays.asList(
                            baseKey + ".description",
                            baseKey + ".desc",
                            baseKey + ".tooltip",
                            baseKey
                    );

                    String description = "";
                    for (String key : textKeys) {
                        String translated = getTranslation(key);
                        // 如果翻译结果不等于Key本身，说明找到了有效的翻译文本
                        if (translated != null && !translated.equals(key) && !translated.isEmpty()) {
                            description = translated;
                            break;
                        }
                    }

                    // 如果还是没找到，且确实是某些特殊模组，可以在这里加硬编码逻辑（通常不需要）
                    if (description.isEmpty()) {
                        description = "无描述";
                    }

                    effect.addProperty("description", description);

                    // --- 基础属性 ---
                    effect.addProperty("category", mobEffect.getCategory().name().toLowerCase()); // beneficial/harmful/neutral
                    effect.addProperty("color", "#" + String.format("%06X", (0xFFFFFF & mobEffect.getColor())));

                    effects.add(effect);
                });
        root.add("effects", effects);

        // 2. 导出所有稀有度 (Rarities)
        JsonArray rarities = new JsonArray();
        RarityRegistry.INSTANCE.getValues().stream()
                .sorted(Comparator.comparingInt(LootRarity::sortIndex))
                .forEach(rarity -> {
                    ResourceLocation key = RarityRegistry.INSTANCE.getKey(rarity);
                    if (key != null) {
                        JsonObject r = new JsonObject();
                        r.addProperty("id", key.toString());
                        r.addProperty("color", "#" + String.format("%06X", (0xFFFFFF & rarity.color().getValue())));
                        rarities.add(r);
                    }
                });
        root.add("rarities", rarities);

        // 3. 导出战利品分类 (Loot Categories)
        JsonArray categories = new JsonArray();
        Apoth.BuiltInRegs.LOOT_CATEGORY.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> {
                    JsonObject cat = new JsonObject();
                    cat.addProperty("id", entry.getKey().location().toString());
                    categories.add(cat);
                });
        root.add("categories", categories);

        // 4. 导出 Target 枚举
        JsonArray targets = new JsonArray();
        // 注意：如果你使用的 Apotheosis 版本不同，Target 枚举的位置可能不同，请根据实际情况调整
        try {
            // 这是一个反射式的安全写法，或者直接使用你原来确定存在的类路径
            Class<?> targetClass = Class.forName("dev.shadowsoffire.apotheosis.affix.effect.MobEffectAffix$Target");
            for (Object t : targetClass.getEnumConstants()) {
                targets.add(t.toString());
            }
        } catch (ClassNotFoundException e) {
            // 如果类路径变了，尝试直接使用硬编码的常用值，或保持空以防报错
            targets.add("ATTACKER");
            targets.add("VICTIM");
            targets.add("ARROW");
        }
        root.add("targets", targets);

        // 写入文件
        File file = new File("apoth_export.json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(root, writer);
            System.out.println("Apotheosis data dumped to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 安全获取翻译文本的方法。
     * 优先尝试从客户端语言管理器获取（如果在客户端运行），
     * 否则尝试从组件解析（服务器环境）。
     */
    private static String getTranslation(String key) {
        // 尝试1: 直接通过 Component 解析（这是最通用的方法，兼容 NeoForge/Forge 自动加载的语言包）
        String componentResult = Component.translatable(key).getString();

        // 如果解析结果和 key 不一样，说明成功了
        if (!componentResult.equals(key)) {
            return componentResult;
        }

        // 尝试2: 某些极端情况下（例如纯服务端且未加载en_us），可能需要直接访问 Language 实例
        // 注意：服务器端 Language.getInstance() 通常默认只有 key 或者 fallback
        if (Language.getInstance().has(key)) {
            return Language.getInstance().getOrDefault(key);
        }

        return key; // 实在找不到，返回 key 供调用者判断
    }
}