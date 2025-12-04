package com.example.examplemod.affix;

import com.example.examplemod.category.LootCategories;
import com.example.examplemod.util.AffixSchoolMapper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.Map;
import java.util.Set;

/**
 * 自定义词条类：法力消耗减少词条 (ManaCostAffix)。
 * <p>
 * 该词条用于减少特定魔法派系（例如：火系、冰系）法术的法力消耗。
 * 它是基于 Apotheosis 的 Affix 系统扩展的。
 */
public class ManaCostAffix extends Affix {

    // 定义该词条的序列化/反序列化逻辑 (Codec)，用于从 JSON 配置文件中读取词条数据。
    public static final Codec<ManaCostAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Affix.affixDef(), // 读取基础词条定义
                    ResourceLocation.CODEC.fieldOf("school").forGetter(a -> a.school.getId()), // 读取魔法派系ID (如 irons_spellbooks:fire)
                    LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values), // 读取不同稀有度下的数值计算函数
                    LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories) // 读取允许生成的物品分类集合
            ).apply(inst, ManaCostAffix::new));

    // 词条对应的魔法派系类型 (来自 Iron's Spells 'n Spellbooks)
    protected final SchoolType school;
    // 存储每个稀有度对应的数值计算函数 (StepFunction 用于根据等级计算具体数值)
    protected final Map<LootRarity, StepFunction> values;
    // 允许该词条生成的物品分类集合
    protected final Set<LootCategory> categories;

    /**
     * 构造函数
     * @param definition 词条的基础定义
     * @param schoolId 魔法派系的资源路径 ID
     * @param values 不同稀有度的数值映射
     * @param categories 允许的物品分类
     */
    public ManaCostAffix(AffixDefinition definition, ResourceLocation schoolId, Map<LootRarity, StepFunction> values, Set<LootCategory> categories) {
        super(definition);
        // 从注册表中获取对应的 SchoolType 对象
        this.school = SchoolRegistry.getSchool(schoolId);
        // 如果找不到对应的魔法派系，抛出异常以防止配置错误
        if (this.school == null) {
            throw new IllegalArgumentException("Invalid school ID provided for ManaCostAffix: " + schoolId);
        }
        this.values = values;
        this.categories = categories;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    /**
     * 判断该词条是否可以应用到指定的物品堆 (ItemStack) 上。
     * 这是一个核心逻辑方法。
     */
    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        // 1. 硬性限制：只能应用在法杖 (STAFF)、近战武器 (MELEE_WEAPON) 或 法术书 (SPELLBOOK) 上
        LootCategory staff = LootCategories.STAFF.value();
        LootCategory spellbook = LootCategories.SPELLBOOK.value();

        // 注意：Apoth.LootCategories.MELEE_WEAPON 通常是静态常量对象，直接引用即可
        // 逻辑：如果分类 不是法杖 且 不是近战 且 不是法术书，则返回 false
        if (cat != staff && cat != Apoth.LootCategories.MELEE_WEAPON && cat != spellbook) {
            return false;
        }

        // 2. 检查当前稀有度是否有配置对应的数值，如果没有则无法应用
        if (!this.values.containsKey(rarity)) {
            return false;
        }

        // 3. 如果 JSON 配置中定义了特定的 categories 集合，检查当前物品分类是否包含在内
        if (!this.categories.isEmpty() && !this.categories.contains(cat)) {
            return false;
        }

        // 4. 派系匹配检查 (关键逻辑)：
        // 检查物品本身是否属于该词条对应的魔法派系。
        // 例如：防止"火系减耗"词条出现在"冰系法杖"上。
        Set<SchoolType> gearSchools = AffixSchoolMapper.getSpellSchoolsFromGear(stack);
        return gearSchools.contains(this.school);
    }

    /**
     * 获取词条在物品提示框 (Tooltip) 中的描述文本。
     * 例如："火系法力消耗 -10%"
     */
    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        // 根据稀有度和等级计算减少的百分比
        float reduction = this.getReductionPercent(inst.getRarity(), inst.level());

        // 生成本地化键名，例如 "school.irons_spellbooks.fire"
        String schoolTranslationKey = "school." + school.getId().getNamespace() + "." + school.getId().getPath();

        // 返回格式化后的文本组件
        // affix.examplemod.mana_cost.desc 可能是 "%s 法力消耗 -%s"
        return Component.translatable("affix.examplemod.mana_cost.desc",
                Component.translatable(schoolTranslationKey).withStyle(school.getDisplayName().getStyle()), // 派系名称带颜色
                fmt(reduction * 100)); // 将小数转换为百分比格式
    }

    /**
     * 获取在重铸台或附魔界面显示的增强文本。
     * 通常会显示数值的变动范围。
     * 例如："火系法力消耗 -10% [5% - 15%]"
     */
    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        MutableComponent comp = this.getDescription(inst, ctx);

        // 计算当前稀有度下的最小值 (level 0) 和 最大值 (level 1)
        float minReduction = this.getReductionPercent(inst.getRarity(), 0);
        float maxReduction = this.getReductionPercent(inst.getRarity(), 1);

        Component minComp = Component.translatable("%s%%", fmt(minReduction * 100));
        Component maxComp = Component.translatable("%s%%", fmt(maxReduction * 100));

        // 添加数值范围显示 (例如: [5% - 15%])
        return comp.append(valueBounds(minComp, maxComp));
    }

    public SchoolType getSchool() {
        return school;
    }

    /**
     * 根据稀有度和等级计算具体的减耗百分比。
     * @param level 词条等级 (0.0 到 1.0 之间)
     */
    public float getReductionPercent(LootRarity rarity, float level) {
        StepFunction func = this.values.get(rarity);
        return func != null ? func.get(level) : 0f;
    }
}