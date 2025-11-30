这份文档是为您准备的 **Minecraft 1.21+ 数据驱动附魔系统开发参考手册**。它基于您提供的 `EnchantmentEffectComponents` 源码编写，采用了商业技术文档的标准格式，可以直接发布在 GitHub Wiki、GitBook 或公司内部开发文档库中。

---

# Minecraft 开发文档：附魔效果组件 (Enchantment Effect Components)

**版本适用性**：Minecraft 1.21+ (Java Edition)  
**相关系统**：Data-Driven Enchantments (数据驱动附魔), Datapacks, Modding API  
**最后更新**：2024年

---

## 1. 概述

`EnchantmentEffectComponents` 是 Minecraft 1.21 引入的数据驱动附魔系统的核心注册表接口。它定义了所有可用的附魔效果类型（Effect Components）。

在新的系统中，附魔不再是硬编码的逻辑，而是由一系列**组件（Components）**组成的集合。每个组件定义了附魔的具体行为（如：造成额外伤害、减少受到的伤害、产生粒子效果等）。这些组件可以通过数据包（Datapack）的 JSON 文件进行配置，也可以通过 Mod 代码动态调用。

### 核心概念

*   **DataComponentType**: 每个字段代表一种特定类型的数据组件键（Key）。
*   **ConditionalEffect**: 大多数效果都被包裹在 `ConditionalEffect` 中，意味着它们支持 Loot Tables 中的谓词（Predicates）条件（如：仅在夜晚生效、仅对亡灵生物生效）。
*   **LootContextParamSet**: 定义了在计算该效果时，可用的上下文参数（如：攻击者、受害者、物品、位置）。

---

## 2. 组件参考索引

以下表格列出了所有注册的附魔效果组件及其功能定义、数据类型和上下文环境。

### 2.1 战斗与伤害计算 (Combat & Damage)

| 组件键名 (Registry Name) | 数据类型 | 上下文 (Context) | 描述 |
| :--- | :--- | :--- | :--- |
| `damage` | `List<Conditional<ValueEffect>>` | `ENCHANTED_DAMAGE` | **攻击伤害**。计算攻击时造成的额外伤害（如锋利）。 |
| `damage_protection` | `List<Conditional<ValueEffect>>` | `ENCHANTED_DAMAGE` | **伤害减免**。计算受到伤害时的减免量（如保护）。 |
| `damage_immunity` | `List<Conditional<DamageImmunity>>` | `ENCHANTED_DAMAGE` | **伤害免疫**。完全免疫特定类型的伤害。 |
| `smash_damage_per_fallen_block` | `List<Conditional<ValueEffect>>` | `ENCHANTED_DAMAGE` | **坠落攻击加成**。基于坠落距离计算伤害（如重锤的特殊效果）。 |
| `knockback` | `List<Conditional<ValueEffect>>` | `ENCHANTED_DAMAGE` | **击退增强**。增加攻击造成的击退距离（如击退）。 |
| `armor_effectiveness` | `List<Conditional<ValueEffect>>` | `ENCHANTED_DAMAGE` | **护甲穿透/效能**。修改护甲在伤害计算中的有效性（如破甲效果）。 |
| `post_attack` | `List<Targeted<EntityEffect>>` | `ENCHANTED_DAMAGE` | **攻击后回调**。攻击发生后执行的实体效果（如燃烧、中毒）。 |

### 2.2 物品与耐久 (Item & Durability)

| 组件键名 (Registry Name) | 数据类型 | 上下文 (Context) | 描述 |
| :--- | :--- | :--- | :--- |
| `item_damage` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ITEM` | **耐久损耗**。修改物品使用时的耐久度消耗（如耐久）。 |
| `ammo_use` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ITEM` | **弹药消耗**。修改远程武器的弹药消耗几率（如无限）。 |
| `repair_with_xp` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ITEM` | **经验修补**。利用经验值修复物品的比例（如经验修补）。 |
| `prevent_equipment_drop` | `Unit` | N/A | **防止掉落**。死亡时物品不会掉落（如消失诅咒）。 |
| `prevent_armor_change` | `Unit` | N/A | **防止脱下**。物品装备后无法被取下（如绑定诅咒）。 |

### 2.3 投射物系统 (Projectiles)

| 组件键名 (Registry Name) | 数据类型 | 上下文 (Context) | 描述 |
| :--- | :--- | :--- | :--- |
| `projectile_piercing` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ITEM` | **穿透能力**。投射物可穿透的实体数量（如穿透）。 |
| `projectile_spawned` | `List<Conditional<EntityEffect>>` | `ENCHANTED_ENTITY` | **生成回调**。投射物生成时触发的实体效果。 |
| `projectile_spread` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ENTITY` | **散射控制**。修改投射物的散射角度（如多重射击的散射）。 |
| `projectile_count` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ENTITY` | **弹丸数量**。增加一次射击发射的弹丸数量（如多重射击）。 |

### 2.4 实体交互与环境 (Entity & World)

| 组件键名 (Registry Name) | 数据类型 | 上下文 (Context) | 描述 |
| :--- | :--- | :--- | :--- |
| `attributes` | `List<AttributeEffect>` | N/A | **属性修饰**。装备时给予的属性加成（如移动速度、生命上限）。 |
| `location_changed` | `List<Conditional<LocationEffect>>` | `ENCHANTED_LOCATION` | **位置改变**。当实体移动或改变位置时触发的效果（如冰霜行者）。 |
| `tick` | `List<Conditional<EntityEffect>>` | `ENCHANTED_ENTITY` | **每刻更新**。每一 Tick 对持有者执行的效果。 |
| `hit_block` | `List<Conditional<EntityEffect>>` | `HIT_BLOCK` | **击中方块**。攻击或交互方块时触发的效果。 |
| `equipment_drops` | `List<Targeted<ValueEffect>>` | `ENCHANTED_DAMAGE` | **装备掉落**。修改被击杀实体掉落装备的概率。 |

### 2.5 战利品与经验 (Loot & Experience)

| 组件键名 (Registry Name) | 数据类型 | 上下文 (Context) | 描述 |
| :--- | :--- | :--- | :--- |
| `block_experience` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ITEM` | **挖掘经验**。修改破坏方块掉落的经验值。 |
| `mob_experience` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ENTITY` | **击杀经验**。修改击杀生物掉落的经验值。 |
| `fishing_luck_bonus` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ENTITY` | **钓鱼运气**。修改钓鱼时的运气属性（如海之眷顾）。 |
| `fishing_time_reduction` | `List<Conditional<ValueEffect>>` | `ENCHANTED_ENTITY` | **钓鱼等待**。减少鱼咬钩的等待时间（如诱饵）。 |

### 2.6 特定武器机制 (Trident & Crossbow)

| 组件键名 (Registry Name) | 数据类型 | 描述 |
| :--- | :--- | :--- |
| `trident_return_acceleration` | `List<Conditional<ValueEffect>>` | 三叉戟返回忠诚度的加速度。 |
| `trident_spin_attack_strength` | `ValueEffect` | 激流附魔的冲刺力度。 |
| `trident_sound` | `List<Holder<SoundEvent>>` | 三叉戟特有的音效。 |
| `crossbow_charge_time` | `ValueEffect` | 十字弩装填时间修饰（如快速装填）。 |
| `crossbow_charging_sounds` | `List<ChargingSounds>` | 十字弩装填时的声音序列。 |

---
