package com.example.examplemod.server.data;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minecraft 全息图谱导出器 (SQLite 终极版)
 *
 * 核心功能：
 * 1. 导出所有注册表、标签、创造模式页签。
 * 2. 构建 FTS5 全文搜索索引，支持 ID、中文名、Lore、组件类型的混合搜索。
 * 3. 自动扁平化组件信息，无需 JOIN 即可搜索 "food" 或 "max_stack_size"。
 */
//@EventBusSubscriber(value = Dist.CLIENT)
public class UniversalGraphExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("MC-Graph");
    private static final String DB_NAME = "minecraft_static_universe.db";
    // 启用 WAL 模式和 10秒超时，防止锁死
    private static final String SQLITE_CONFIG = "?busy_timeout=10000";
    private static final String SQLITE_JAR_URL = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.0.0/sqlite-jdbc-3.46.0.0.jar";

    // 缓存用于建立链接和翻译
    private static final Map<String, List<Long>> DEFINITION_CACHE = new HashMap<>();
    private static final Map<String, Long> MOD_NODE_CACHE = new HashMap<>();
    private static final Map<String, String> LANG_CACHE = new HashMap<>();

    private static final AtomicBoolean IS_EXPORTING = new AtomicBoolean(false);

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        // 仅在客户端进入世界且数据包同步完成后运行
        if (Minecraft.getInstance().level == null || FMLEnvironment.dist != Dist.CLIENT) return;
        if (IS_EXPORTING.get()) return;

        new Thread(() -> {
            if (!IS_EXPORTING.compareAndSet(false, true)) return;
            try {
                // 稍微延迟，确保其他 Mod 初始化完毕
                Thread.sleep(3000);
                LOGGER.info(">>> [Graph] 开始全息图谱导出任务...");
                exportUniverse();
            } catch (Exception e) {
                LOGGER.error(">>> [Graph] 导出严重错误", e);
            } finally {
                IS_EXPORTING.set(false);
            }
        }, "MC-Graph-Exporter").start();
    }

    private static void exportUniverse() throws Exception {
        Driver driver = loadSqliteDriver();
        File dbFile = new File(DB_NAME);

        // 尝试清理旧库
        if (dbFile.exists()) {
            System.gc();
            if (!dbFile.delete()) LOGGER.warn(">>> [Graph] 旧数据库占用中，尝试覆盖模式...");
        }

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath() + SQLITE_CONFIG;
        try (Connection conn = driver.connect(url, new Properties())) {
            // 性能与并发优化
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
                stmt.execute("PRAGMA foreign_keys=ON;");
            }
            conn.setAutoCommit(false); // 开启事务

            LOGGER.info(">>> [Graph] 1/4 初始化数据库结构...");
            initSchema(conn);

            long rootId = createNode(conn, null, "ROOT", "Static_Universe", "1.21+");

            LOGGER.info(">>> [Graph] 2/4 导出语言文件...");
            // 1. 导出语言并缓存到内存
            String langCode = Minecraft.getInstance().getLanguageManager().getSelected().toString();
            long langRootId = createNode(conn, rootId, "CATEGORY", "Languages", langCode);
            exportLanguagesAndCache(conn, langRootId);

            LOGGER.info(">>> [Graph] 3/4 导出注册表与构建索引...");
            // 2. 核心：导出定义并构建 FTS 索引
            long defRootId = createNode(conn, rootId, "CATEGORY", "Definitions", "Registries");
            bootstrapDefinitions(conn, defRootId);

            LOGGER.info(">>> [Graph] 4/4 导出创造模式实例...");
            // 3. 导出物品实例（包含 NBT/Component 数据）
            long creativeRootId = createNode(conn, rootId, "CATEGORY", "Creative_Tabs", "Prototypes");
            exportCreativeTabs(conn, creativeRootId);

            conn.commit(); // 提交事务
            LOGGER.info(">>> [Graph] 导出成功！文件: {}, 大小: {} KB", dbFile.getAbsolutePath(), dbFile.length() / 1024);
        }
    }

    private static void initSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS universe_links");
            stmt.execute("DROP TABLE IF EXISTS autocomplete_index");
            stmt.execute("DROP TABLE IF EXISTS universe_nodes");

            // 节点表
            stmt.execute("CREATE TABLE universe_nodes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "parent_id INTEGER, " +
                    "node_type TEXT NOT NULL, " +
                    "key TEXT, " +
                    "value TEXT, " +
                    "FOREIGN KEY(parent_id) REFERENCES universe_nodes(id) ON DELETE CASCADE" +
                    ")");
            stmt.execute("CREATE INDEX idx_type_key ON universe_nodes(node_type, key)");
            stmt.execute("CREATE INDEX idx_value ON universe_nodes(value)");

            // 关系表
            stmt.execute("CREATE TABLE universe_links (" +
                    "source_node_id INTEGER NOT NULL, " +
                    "target_node_id INTEGER NOT NULL, " +
                    "FOREIGN KEY(source_node_id) REFERENCES universe_nodes(id), " +
                    "FOREIGN KEY(target_node_id) REFERENCES universe_nodes(id)" +
                    ")");
            stmt.execute("CREATE INDEX idx_links_target ON universe_links(target_node_id)");

            // FTS5 搜索表：prefix支持前缀搜索 (foo* -> food)
            stmt.execute("CREATE VIRTUAL TABLE autocomplete_index USING fts5(content, node_id UNINDEXED, prefix='1 2 3')");
        }
    }

    @SuppressWarnings("unchecked")
    private static void exportLanguagesAndCache(Connection conn, long parentId) throws SQLException {
        LANG_CACHE.clear();
        Language languageInstance = Language.getInstance();
        Map<String, String> rawLangMap = null;

        // 反射获取语言 Map
        try {
            Class<?> clazz = languageInstance.getClass();
            while (clazz != null && rawLangMap == null) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object obj = field.get(languageInstance);
                        if (obj instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) obj;
                            if (map.containsKey("item.minecraft.diamond") || map.containsKey("gui.done")) {
                                rawLangMap = (Map<String, String>) map;
                                break;
                            }
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            LOGGER.error(">>> 反射获取语言失败", e);
        }

        if (rawLangMap != null) {
            LANG_CACHE.putAll(rawLangMap);
            String sql = "INSERT INTO universe_nodes (parent_id, node_type, key, value) VALUES (?, 'TRANSLATION', ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int batch = 0;
                for (Map.Entry<String, String> entry : rawLangMap.entrySet()) {
                    ps.setLong(1, parentId);
                    ps.setString(2, entry.getKey());
                    ps.setString(3, entry.getValue());
                    ps.addBatch();
                    if (++batch % 2000 == 0) ps.executeBatch();
                }
                ps.executeBatch();
            }
        }
    }

    // --- 核心逻辑：导出定义 + 构建“万物皆可搜”索引 ---
    private static void bootstrapDefinitions(Connection conn, long rootId) throws SQLException {
        DEFINITION_CACHE.clear();
        MOD_NODE_CACHE.clear();

        String ftsSql = "INSERT INTO autocomplete_index (content, node_id) VALUES (?, ?)";
        PreparedStatement ftsPs = conn.prepareStatement(ftsSql);
        int ftsBatch = 0;

        RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
        var allRegistries = registryAccess.registries().toList();

        for (var registryEntry : allRegistries) {
            Registry<?> registry = registryEntry.value();
            ResourceLocation registryKey = registry.key().location();
            String registryKeyStr = registryKey.toString();

            long modId = getOrCreateNode(conn, rootId, "MOD", registryKey.getNamespace(), null);
            long regId = createNode(conn, modId, "REGISTRY", registryKey.getPath(), null);

            for (Holder.Reference<?> holder : registry.holders().toList()) {
                ResourceLocation key = holder.key().location();
                String uid = key.toString();

                // 1. 创建定义节点
                long defId = createNode(conn, regId, "DEF", key.getPath(), uid);
                DEFINITION_CACHE.computeIfAbsent(uid, k -> new ArrayList<>()).add(defId);

                // --- 构建搜索内容 (Namespace + ID + Lang + Components + Tooltips) ---
                StringBuilder searchContent = new StringBuilder();

                // A. 基础信息
                searchContent.append(key.getNamespace()).append(" ");
                searchContent.append(key.getPath()).append(" ");
                searchContent.append(uid).append(" ");
                searchContent.append(registryKeyStr).append(" "); // 把 "minecraft:item" 加进去

                // B. 翻译名称
                String descId = tryGetDescriptionId(holder.value(), registry, key);
                if (descId != null) {
                    createNode(conn, defId, "PROPERTY", "description_id", descId);
                    searchContent.append(descId).append(" ");
                    String translated = LANG_CACHE.get(descId);
                    if (translated != null) searchContent.append(translated).append(" ");
                }

                // C. 物品特有：索引组件类型 和 Tooltip
                if ( holder.value() instanceof Item item) {
                    try {
                        // C1. 扁平化索引组件 (让你能搜 'food', 'damage' 等)
                        for (TypedDataComponent<?> comp : item.components()) {
                            ResourceLocation compKey = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(comp.type());
                            if (compKey != null) {
                                searchContent.append(compKey.toString()).append(" "); // minecraft:food
                                searchContent.append(compKey.getPath()).append(" ");  // food
                            }
                        }

                        // C2. 索引 Tooltip (描述/Lore)
                        ItemStack stack = new ItemStack(item);
                        List<Component> tooltips = stack.getTooltipLines(
                                Item.TooltipContext.of(Minecraft.getInstance().level),
                                Minecraft.getInstance().player,
                                TooltipFlag.Default.NORMAL
                        );
                        for (Component comp : tooltips) {
                            String line = comp.getString();
                            if (!line.isBlank()) searchContent.append(line).append(" ");
                        }
                    } catch (Throwable t) {
                        // 忽略某些物品在后台生成 Tooltip 时的报错
                    }
                }

                // D. 写入 FTS 表
                ftsPs.setString(1, searchContent.toString());
                ftsPs.setLong(2, defId);
                ftsPs.addBatch();

                if (++ftsBatch % 2000 == 0) ftsPs.executeBatch();
            }
        }
        ftsPs.executeBatch();
        ftsPs.close();
    }

    private static String tryGetDescriptionId(Object value, Registry<?> registry, ResourceLocation id) {
        try {
            if (value instanceof Item i) return i.getDescriptionId();
            if (value instanceof Block b) return b.getDescriptionId();
            if (value instanceof EntityType<?> e) return e.getDescriptionId();
            if (value instanceof MobEffect m) return m.getDescriptionId();
            if (value instanceof Enchantment) return Util.makeDescriptionId("enchantment", id);
            if (value instanceof Attribute) return Util.makeDescriptionId("attribute", id);
        } catch (Exception ignored) {}
        return null;
    }

    private static void exportCreativeTabs(Connection conn, long parentId) throws SQLException {
        try {
            CreativeModeTabs.tryRebuildTabContents(
                    Minecraft.getInstance().level.enabledFeatures(),
                    true,
                    Minecraft.getInstance().level.registryAccess()
            );
        } catch (Exception ignored) {}

        RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
        RegistryOps<JsonElement> registryOps = registryAccess.createSerializationContext(JsonOps.INSTANCE);

        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            String tabName = tab.getDisplayName().getString();
            long tabId = createNode(conn, parentId, "TAB", tabName, null);

            Collection<ItemStack> displayItems = tab.getDisplayItems();
            int index = 0;
            for (ItemStack stack : displayItems) {
                exportItemStack(conn, tabId, index++, stack, registryOps);
            }
        }
    }

    private static void exportItemStack(Connection conn, long parentId, int index, ItemStack stack, RegistryOps<JsonElement> registryOps) throws SQLException {
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemUid = itemKey != null ? itemKey.toString() : "minecraft:air";
        String displayValue = stack.getCount() + " x " + itemUid;

        // 1. 创建实例节点
        long stackId = createNode(conn, parentId, "INSTANCE", String.valueOf(index), displayValue);
        linkToDefinitions(conn, stackId, itemUid);


        exportInstanceTooltipToFts(conn, stackId, stack);
        // =================================================================

        // 2. 导出 NBT 组件 (保持原样)
        for (TypedDataComponent<?> component : stack.getComponents()) {
            exportComponent(conn, stackId, component, registryOps);
        }
        exportComputedAttributes(conn, stackId, stack);
    }
    private static void exportComputedAttributes(Connection conn, long parentId, ItemStack stack) throws SQLException {
        // 创建一个名为 "COMPUTED_STATS" 的虚拟节点，用于存放计算后的属性
        long computedRootId = createNode(conn, parentId, "COMPUTED_STATS", "effective_values", "Runtime Calculated");

        // 遍历所有可能的装备槽位（主手、副手、头、胸、腿、脚、身体）
        // 这样不仅能抓到武器伤害，还能抓到护甲值
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {

            // 使用 forEachModifier 获取该槽位下所有生效的属性（包含原版默认+动态计算+NBT修改）
            stack.forEachModifier(slot, (attributeHolder, modifier) -> {
                try {
                    // 获取属性名 (例如 minecraft:attack_damage)
                    String attrKey = attributeHolder.unwrapKey()
                            .map(k -> k.location().toString())
                            .orElse("unknown_attribute");

                    // 获取数值和操作类型
                    double amount = modifier.amount();
                    String operation = modifier.operation().name(); // ADD_VALUE, ADD_MULTIPLIED_BASE 等
                    String slotName = slot.getName();

                    // 只有数值不为0才导出，避免垃圾数据
                    if (amount != 0) {
                        // 创建属性节点：
                        // Key = 属性名 (minecraft:generic.attack_damage)
                        // Value = 数值 (5.0)
                        long attrId = createNode(conn, computedRootId, "ATTRIBUTE", attrKey, String.valueOf(amount));

                        // 记录额外细节
                        createNode(conn, attrId, "PROPERTY", "slot", slotName);
                        createNode(conn, attrId, "PROPERTY", "operation", operation);

                        // 尝试链接到属性的定义节点（方便跳转）
                        linkToDefinitions(conn, attrId, attrKey);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    // 专门用于导出创造模式物品栏实例的 Tooltip 到 FTS 索引
    private static void exportInstanceTooltipToFts(Connection conn, long stackId, ItemStack stack) {
        try {
            // 获取详细描述 (包含属性、Lore、附魔效果、Apotheosis属性等)
            List<Component> tooltips = stack.getTooltipLines(
                    Item.TooltipContext.of(Minecraft.getInstance().level),
                    Minecraft.getInstance().player,
                    TooltipFlag.Default.NORMAL
            );

            StringBuilder content = new StringBuilder();

            // 1. 把物品 ID 也加进去，确保搜 ID 也能找到这个实例
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null) {
                content.append(key.getNamespace()).append(" ");
                content.append(key.getPath()).append(" ");
                content.append(key).append(" ");
            }

            // 2. 把所有可见的文本加进去
            for (Component comp : tooltips) {
                String line = comp.getString();
                if (!line.isBlank()) {
                    content.append(line).append(" ");
                }
            }

            // 3. 写入 FTS 表
            // 注意：这里的 node_id 是 stackId (实例ID)，而不是 defId (定义ID)
            String sql = "INSERT INTO autocomplete_index (content, node_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, content.toString());
                ps.setLong(2, stackId);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
            // 忽略某些物品在后台获取 Tooltip 时的异常，防止崩溃
        }
    }
    private static <T> void exportComponent(Connection conn, long stackId, TypedDataComponent<T> component, RegistryOps<JsonElement> registryOps) throws SQLException {
        DataComponentType<T> type = component.type();
        ResourceLocation typeLoc = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
        if (typeLoc == null) return;

        long compId = createNode(conn, stackId, "COMPONENT", typeLoc.toString(), null);
        linkToDefinitions(conn, compId, typeLoc.toString());

        if (type.codec() != null) {
            try {
                var result = type.codec().encodeStart(registryOps, component.value());
                result.result().ifPresent(json -> {
                    try {
                        exportJsonTree(conn, compId, null, json);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception ignored) {}
        }
    }

    private static void exportJsonTree(Connection conn, long parentId, String key, JsonElement json) throws SQLException {
        if (json.isJsonObject()) {
            long nodeId = createNode(conn, parentId, "TAG_COMPOUND", key, null);
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                exportJsonTree(conn, nodeId, entry.getKey(), entry.getValue());
            }
        } else if (json.isJsonArray()) {
            long nodeId = createNode(conn, parentId, "TAG_LIST", key, null);
            JsonArray arr = json.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                exportJsonTree(conn, nodeId, String.valueOf(i), arr.get(i));
            }
        } else if (json.isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive();
            String rawVal = prim.getAsString();
            long nodeId = createNode(conn, parentId, "TAG_VALUE", key, rawVal);
            if (prim.isString() && rawVal.contains(":") && !rawVal.contains(" ")) {
                linkToDefinitions(conn, nodeId, rawVal);
            }
        }
    }

    private static long createNode(Connection conn, Long parentId, String type, String key, String value) throws SQLException {
        String sql = "INSERT INTO universe_nodes (parent_id, node_type, key, value) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (parentId != null) ps.setLong(1, parentId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, type);
            ps.setString(3, key);
            ps.setString(4, value);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    private static long getOrCreateNode(Connection conn, long parentId, String type, String key, String value) throws SQLException {
        String cacheKey = parentId + ":" + type + ":" + key;
        if (MOD_NODE_CACHE.containsKey(cacheKey)) return MOD_NODE_CACHE.get(cacheKey);
        long id = createNode(conn, parentId, type, key, value);
        MOD_NODE_CACHE.put(cacheKey, id);
        return id;
    }

    private static void linkToDefinitions(Connection conn, long sourceNodeId, String uid) throws SQLException {
        List<Long> targetIds = DEFINITION_CACHE.get(uid);
        if (targetIds != null) {
            String sql = "INSERT INTO universe_links (source_node_id, target_node_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Long targetId : targetIds) {
                    ps.setLong(1, sourceNodeId);
                    ps.setLong(2, targetId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    // --- 自动下载驱动 ---
    private static Driver loadSqliteDriver() throws Exception {
        File libDir = new File("libs");
        if (!libDir.exists()) libDir.mkdirs();
        File jarFile = new File(libDir, "sqlite-jdbc-3.46.0.0.jar");

        if (!jarFile.exists() || jarFile.length() == 0) {
            LOGGER.info(">>> 正在下载 SQLite 驱动...");
            try (InputStream in = new URL(SQLITE_JAR_URL).openStream();
                 FileOutputStream out = new FileOutputStream(jarFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, UniversalGraphExporter.class.getClassLoader());
        Class<?> driverClass = loader.loadClass("org.sqlite.JDBC");
        return (Driver) driverClass.getConstructor().newInstance();
    }

}