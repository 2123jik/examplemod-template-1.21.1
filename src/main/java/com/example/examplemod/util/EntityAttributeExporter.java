package com.example.examplemod.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.fml.loading.FMLPaths;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.example.examplemod.ExampleMod.LOGGER;

public class EntityAttributeExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 辅助方法：获取属性集
    private AttributeSupplier getEntityDefaultAttributes(EntityType<?> entityType) {
        if (!LivingEntity.class.isAssignableFrom(entityType.getBaseClass())) {
            return null;
        }
        try {
            AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) entityType);
            if (supplier != null && supplier.hasAttribute(Attributes.MAX_HEALTH)) {
                return supplier;
            }
            return null;
        } catch (Exception e) {
            // 某些 Mod 实体可能注册不规范，忽略错误
            return null;
        }
    }

    // 辅助方法：添加属性到 JsonObject
    private void addAttributeIfPresent(JsonObject json, String key, AttributeSupplier attributes, Holder<Attribute> attribute) {
        if (attributes.hasAttribute(attribute)) {
            json.addProperty(key, attributes.getBaseValue(attribute));
        }
    }

    public void exportAllEntityAttributes() {
        LOGGER.info("====== 开始导出实体属性 (JSON 文件) ======");

        JsonObject rootJson = new JsonObject();
        Path exportPath = FMLPaths.GAMEDIR.get().resolve("export").resolve("entity_attributes.json");

        BuiltInRegistries.ENTITY_TYPE.forEach(entityType -> {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (id == null) return;

            AttributeSupplier attributes = getEntityDefaultAttributes(entityType);

            if (attributes != null) {
                JsonObject entityJson = new JsonObject();

                addAttributeIfPresent(entityJson, "generic.max_health", attributes, Attributes.MAX_HEALTH);
                addAttributeIfPresent(entityJson, "generic.movement_speed", attributes, Attributes.MOVEMENT_SPEED);
                addAttributeIfPresent(entityJson, "generic.attack_damage", attributes, Attributes.ATTACK_DAMAGE);
                addAttributeIfPresent(entityJson, "generic.armor", attributes, Attributes.ARMOR);
                addAttributeIfPresent(entityJson, "generic.follow_range", attributes, Attributes.FOLLOW_RANGE);
                addAttributeIfPresent(entityJson, "generic.knockback_resistance", attributes, Attributes.KNOCKBACK_RESISTANCE);
                addAttributeIfPresent(entityJson, "generic.armor_toughness", attributes, Attributes.ARMOR_TOUGHNESS);

                rootJson.add(id.toString(), entityJson);
            }
        });

        try {
            if (exportPath.getParent() != null) {
                Files.createDirectories(exportPath.getParent());
            }
            try (FileWriter writer = new FileWriter(exportPath.toFile())) {
                GSON.toJson(rootJson, writer);
            }
            LOGGER.info("属性已保存至: {}", exportPath);
        } catch (IOException e) {
            LOGGER.error("保存属性文件失败", e);
        }

        LOGGER.info("====== 属性导出结束 ======");
    }
}