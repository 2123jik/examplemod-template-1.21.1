package com.example.examplemod.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.RegistryOps;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.example.examplemod.ExampleMod.LOGGER;

public class EntityLootTableExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public void exportAllEntityLootTables(MinecraftServer server) {
        LOGGER.info("====== 开始导出实体战利品表 (JSON 文件) ======");

        // 设定导出根目录：游戏运行目录/export/loot_tables/
        Path exportRoot = FMLPaths.GAMEDIR.get().resolve("export").resolve("loot_tables");

        // 获取用于序列化的 Ops (需要 RegistryAccess 来处理物品/方块的引用)
        RegistryOps<JsonElement> registryOps = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);

        BuiltInRegistries.ENTITY_TYPE.forEach(entityType -> {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (entityId == null) return;

            // 1. 获取战利品表的 ResourceKey
            ResourceKey<LootTable> lootTableKey = entityType.getDefaultLootTable();
            ResourceLocation lootTableId = lootTableKey.location();

            // 忽略空的战利品表 (例如 BuiltInLootTables.EMPTY)
            if (lootTableId.getPath().equals("empty")) return;

            // 2. 从服务端数据管理器中获取实际的 LootTable 对象
            // getLootData() 管理着当前加载的所有战利品表
            LootTable lootTable = server.reloadableRegistries().getLootTable(lootTableKey);

            // 如果是空的默认表，有时不需要导出，视需求而定
            if (lootTable == LootTable.EMPTY) return;

            try {
                // 3. 使用 Codec 将 Java 对象序列化回 JSON
                // 注意：这里使用 DIRECT_CODEC，因为我们直接操作的是 LootTable 对象，而不是 Registry 中的 Holder
                JsonElement jsonOutput = LootTable.DIRECT_CODEC.encodeStart(registryOps, lootTable)
                        .getOrThrow(err -> new IllegalStateException("Failed to encode loot table: " + err));

                // 4. 构建文件路径: export/loot_tables/minecraft/entities/zombie.json
                Path filePath = exportRoot.resolve(lootTableId.getNamespace()).resolve(lootTableId.getPath() + ".json");
                File file = filePath.toFile();

                // 确保父目录存在
                if (file.getParentFile() != null) {
                    Files.createDirectories(file.getParentFile().toPath());
                }

                // 5. 写入文件
                try (FileWriter writer = new FileWriter(file)) {
                    GSON.toJson(jsonOutput, writer);
                }

                LOGGER.info("已导出: {} -> {}", entityId, filePath);

            } catch (Exception e) {
                LOGGER.error("导出 {} 的战利品表时出错: {}", entityId, e.getMessage());
            }
        });

        LOGGER.info("====== 战利品表导出完成: {} ======", exportRoot);
    }
}