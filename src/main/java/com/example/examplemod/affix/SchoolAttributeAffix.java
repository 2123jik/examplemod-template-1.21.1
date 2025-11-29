package com.example.examplemod.affix;

import com.example.examplemod.util.AffixSchoolMapper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 自定义属性词条类：派系属性词条 (SchoolAttributeAffix)。
 * <p>
 * 这个类扩展了 Apotheosis 的基础属性词条 (AttributeAffix)。
 * 它的核心功能是限制词条生成的条件：
 * 1. 如果配置了 school (派系)，该词条只能生成在属于该派系的装备上。
 * 2. 如果没配置 school，该词条只能生成在不属于任何派系的普通装备上。
 */
public class SchoolAttributeAffix extends AttributeAffix {

    // 定义序列化/反序列化逻辑 (Codec)，用于读取 JSON 配置文件
    public static final Codec<SchoolAttributeAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.affixDef(), // 基础词条定义 (ID, 类型等)
                    BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(a -> a.attribute), // 要修改的属性 (如 minecraft:generic.max_health)
                    PlaceboCodecs.enumCodec(Operation.class).fieldOf("operation").forGetter(a -> a.operation), // 修改操作 (加法、乘法等)
                    LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values), // 不同稀有度的数值计算函数
                    LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories), // 允许生成的物品分类 (如法杖、胸甲)
                    ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a -> a.schoolId) // [可选字段] 魔法派系 ID (如 irons_spellbooks:ice)
            ).apply(inst, SchoolAttributeAffix::new));

    // 存储配置中读取的派系 ID (可能为空)
    protected final Optional<ResourceLocation> schoolId;
    // 存储解析后的 SchoolType 对象 (用于逻辑判断)
    protected final Optional<SchoolType> school;

    /**
     * 构造函数
     * @param schoolId 可选的派系 ID。如果存在，词条将绑定到该派系。
     */
    public SchoolAttributeAffix(AffixDefinition def, Holder<Attribute> attr, Operation op, Map<LootRarity, StepFunction> values, Set<LootCategory> categories, Optional<ResourceLocation> schoolId) {
        super(def, attr, op, values, categories);
        this.schoolId = schoolId;

        // 尝试解析 ResourceLocation ID 为实际的 SchoolType 对象
        this.school = schoolId.flatMap(id -> {
            SchoolType s = SchoolRegistry.REGISTRY.get(id);
            // 如果 ID 存在但找不到对应的派系 (可能是拼写错误或模组未加载)，打印警告
            if (s == null) {
                Apotheosis.LOGGER.warn("Unknown school ID {} provided for SchoolAttributeAffix, affix may not apply correctly until school is registered.", id);
                return Optional.empty();
            }
            return Optional.of(s);
        });
    }

    /**
     * 核心逻辑：判断词条是否可以应用到当前物品上。
     */
    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        // 1. 首先调用父类 (AttributeAffix) 的检查逻辑
        // 检查基础分类 (categories) 和稀有度 (values) 是否匹配
        if (!super.canApplyTo(stack, cat, rarity)) {
            return false;
        }

        // 2. 安全检查：如果分类无效，则不应用
        if (cat == null || cat.isNone()) {
            return false;
        }

        // 3. 获取该物品所属的魔法派系集合
        // (例如：通过 AffixSchoolMapper 检查该物品是否是火系法杖)
        Set<SchoolType> gearSchools = AffixSchoolMapper.getSpellSchoolsFromGear(stack);

        // 4. 派系匹配逻辑
        if (this.school.isPresent()) {
            // 情况 A: 词条指定了特定派系 (例如 "火系增强")
            // 要求：物品必须包含该派系 (例如必须是火系法杖/法术书)
            SchoolType requiredSchool = this.school.get();
            return gearSchools.contains(requiredSchool);
        } else {
            // 情况 B: 词条没有指定派系 (通用词条)
            // 要求：物品不能包含任何派系。
            // 意味着：这个通用属性词条不会出现在专门的魔法装备上，只出现在普通装备上。
            // (这是为了防止通用词条污染魔法装备池，或者反之亦然)
            return gearSchools.isEmpty();
        }
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
}