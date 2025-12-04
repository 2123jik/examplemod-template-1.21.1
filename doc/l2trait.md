这是一个基于你提供的 `LHTraits.java` (注册表) 和 `ShulkerTrait.java` (具体实现) 代码生成的 **L2Hostility 词条 (Trait) 文档开发指南**。

这份指南旨在帮助开发者理解如何规范地注册词条，并编写用于自动生成 Wiki 或游戏内手册（如 Patchouli/Guidebook）的文档生成器。

---

# L2Hostility 词条文档开发指南

## 1. 词条注册规范 (LHTraits)

所有的怪物词条都通过 `L2Registrate` 在 `LHTraits.java` 中进行注册。为了确保文档生成器能正确抓取信息，注册必须遵循以下标准格式。

### 核心注册方法
```java
public static final RegistryEntry<YourTraitClass> YOUR_TRAIT = L2Hostility.REGISTRATE
    .regTrait("internal_id", YourTraitClass::new, // 1. ID 和 工厂方法
        rl -> new TraitConfig(rl, cost, weight, minLv, maxLv) // 2. 配置参数
            .addWhitelist(...) // 3. 过滤条件 (可选)
            .addBlacklist(...)
    )
    .desc("Description string with placeholders %s") // 4. 静态描述模板
    .lang("Display Name") // 5. 显示名称
    .register();
```

### 参数详解
为了文档的准确性，开发者需理解 `TraitConfig` 中的数值含义：
*   **Cost**: 词条消耗的点数（用于平衡怪物强度）。
*   **Weight**: 生成权重（数值越大越容易出现）。
*   **Min/Max Lv**: 词条允许出现的怪物等级区间。
*   **Whitelist/Blacklist**: 限制该词条只能（或不能）应用于特定的实体类型或标签。

---

## 2. 描述文本系统的实现

文档系统由 **静态描述键** 和 **动态参数注入** 两部分组成。

### 2.1 静态描述 (.desc)
在注册时使用 `.desc()` 定义基础文本。支持 `%s` 占位符。
*   **代码**: `.desc("Shoot bullets every %s seconds.")`
*   **生成**: 会自动在语言文件中生成键值 `trait.l2hostility.your_trait.desc`。

### 2.2 动态详情 (addDetail)
如果描述中包含从配置（Config）中读取的动态数值（如冷却时间、伤害倍率），必须在词条类中重写 `addDetail` 方法。

参考 `ShulkerTrait.java` 的实现：

```java
@Override
public void addDetail(List<Component> list) {
    // 1. 获取翻译键: traits.l2hostility.shulker.desc
    String key = getDescriptionId() + ".desc";
    
    // 2. 计算动态数值: 将 tick 转换为 秒
    double seconds = interval.getAsInt() / 20d;
    
    // 3. 构建组件: 注入数值到占位符
    list.add(Component.translatable(key, 
            Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.AQUA)
    ).withStyle(ChatFormatting.GRAY));
}
```

**文档生成器注意**: 不要直接读取 `.desc()` 的纯文本，必须调用实例的 `addDetail` 方法来获取处理过占位符的完整 `Component`。

---

## 3. 文档生成器开发逻辑 (Doc Gen)

如果你需要编写一个工具（例如 Data Generator 或运行时导出脚本）来导出所有词条的文档，请遵循以下逻辑。

### 3.1 遍历注册表
```java
// 伪代码示例
public void generateDocs() {
    // 获取所有注册的词条
    Collection<MobTrait> traits = LHTraits.TRAITS.getEntries().stream()
                                    .map(RegistryEntry::get)
                                    .toList();

    for (MobTrait trait : traits) {
        DocEntry entry = new DocEntry();
        
        // 1. 基础信息
        entry.id = trait.getRegistryName();
        entry.name = trait.getDisplayName(); // 获取 .lang() 的内容
        
        // 2. 获取配置信息 (TraitConfig)
        // 注意：需确保有接口能访问到 TraitConfig，通常通过注册表回调或特定Map获取
        TraitConfig config = L2Hostility.getTraitConfig(trait); 
        entry.rarity = config.weight;
        entry.cost = config.cost;
        entry.levelRange = config.minLv + " - " + config.maxLv;
        
        // 3. 获取详细描述
        List<Component> descriptionLines = new ArrayList<>();
        trait.addDetail(descriptionLines); 
        // 将 Component 转换为纯文本或 Markdown
        entry.description = convertComponentsToString(descriptionLines);
        
        // 4. 获取适用实体 (白名单/黑名单)
        // 这部分比较困难，因为是 Predicate。
        // 建议在 TraitConfig 中增加 public 的 TagKey/List<EntityType> 访问器以便文档提取
        entry.restrictions = extractRestrictions(config);
        
        exportToMarkdown(entry);
    }
}
```

### 3.2 处理限制条件 (Whitelist/Blacklist)
在 `LHTraits` 中，限制条件通常是以 Lambda 表达式形式存在的：
```java
.addWhitelist(e -> e.addTag(LHTagGen.MELEE_WEAPON_TARGET).add(EntityType.WARDEN))
```
**痛点**: 自动提取 Lambda 逻辑非常困难。
**建议**: 为了文档化，建议在 `TraitConfig` 中将白名单/黑名单的数据结构公开，或者在生成文档时手动维护一份 `特殊词条限制表`，例如：
*   **Counter Strike**: 仅限近战武器持有者 & Warden。
*   **Shulker**: 排除半Boss (SemiBoss)。

---

## 4. 现有词条分类 (用于目录生成)

根据 `LHTraits.java`，文档应分为以下几类：

1.  **基础属性 (No Desc/Stat modifiers)**
    *   Tank, Speedy, Protection, Invisible
2.  **通用词条 (Common)**
    *   攻击型: Fiery, Corrosion, Erosion, Strike
    *   防御型: Regen, Adaptive, Reflect, Split
    *   环境型: Gravity, Moonwalk, Arena
    *   弹射物: Shulker, Grenade
3.  **传说词条 (Legendary)**
    *   *特征*: 极高消耗，强力效果。
    *   Dementor (摄魂), Dispell (驱散), Undying (不朽), Ragnarok (诸神黄昏) 等。
4.  **药水效果 (Effects)**
    *   所有基于 `TargetEffectTrait` 的词条 (Weakness, Poison, Wither 等)。
    *   *自动生成*: 这类词条通常遵循统一的描述模板，可以直接读取其应用的药水效果名称。

---

## 5. 总结：新增词条 CheckList

当你添加一个新词条时，请检查：

*   [ ] **ID** 是否全小写下划线格式。
*   [ ] **Config** 是否设置了合理的 Cost/Weight/Level。
*   [ ] **Lang** 是否设置了易读的显示名称。
*   [ ] **Desc** 是否提供了描述模板（如果有动态数值，是否使用了 `%s`）。
*   [ ] **AddDetail** 如果有动态数值，是否重写了此方法并正确格式化了颜色。
*   [ ] **Tags** 如果是药水类，是否加了 `POTION` tag。