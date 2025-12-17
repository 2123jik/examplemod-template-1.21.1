package com.example.examplemod.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry; // 新增
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;     // 新增
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpellLevelAffix extends Affix {
    public static final Codec<SpellLevelAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Affix.affixDef(),
                    // 改动1: 将 school 设为可选
                    ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a -> Optional.ofNullable(a.school).map(SchoolType::getId)),
                    // 改动2: 新增 spell 字段，也是可选
                    ResourceLocation.CODEC.optionalFieldOf("spell").forGetter(a -> Optional.ofNullable(a.spell).map(AbstractSpell::getSpellResource)),
                    LootRarity.mapCodec(LevelData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.validTypes)
            ).apply(inst, SpellLevelAffix::new));

    @Nullable protected final SchoolType school;
    @Nullable protected final AbstractSpell spell; // 新增字段
    protected final Map<LootRarity, LevelData> values;
    protected final Set<LootCategory> validTypes;

    // 构造函数逻辑更新
    public SpellLevelAffix(AffixDefinition definition, Optional<ResourceLocation> schoolId, Optional<ResourceLocation> spellId, Map<LootRarity, LevelData> values, Set<LootCategory> types) {
        super(definition);

        // 逻辑检查：必须二选一，且不能同时存在
        if (schoolId.isPresent() && spellId.isPresent()) {
            throw new IllegalArgumentException("Affix cannot define both a School and a Spell!");
        }
        if (schoolId.isEmpty() && spellId.isEmpty()) {
            throw new IllegalArgumentException("Affix must define either a School or a Spell!");
        }

        if (schoolId.isPresent()) {
            this.school = SchoolRegistry.getSchool(schoolId.get());
            this.spell = null;
            if (this.school == null) throw new IllegalArgumentException("Invalid school ID: " + schoolId.get());
        } else {
            this.spell = SpellRegistry.getSpell(spellId.get());
            this.school = null;
            if (this.spell == null) throw new IllegalArgumentException("Invalid spell ID: " + spellId.get());
        }

        this.values = values;
        this.validTypes = types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        return getTooltip(inst, false);
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        return getTooltip(inst, true);
    }

    // 统一处理 Tooltip 逻辑
    private MutableComponent getTooltip(AffixInstance inst, boolean augmenting) {
        LevelData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        int current = data.level().getInt(inst.level());

        // 获取目标名称（学派名 或 法术名）
        MutableComponent targetName;
        if (this.spell != null) {
            // 法术名称，例如 "火球术"
            targetName = Component.translatable("spell."+spell.getSpellResource().getNamespace()+"."+spell.getSpellResource().getPath());
        } else {
            // 学派名称，例如 "火系"
            String key = "school." + school.getId().getNamespace() + "." + school.getId().getPath();
            targetName = Component.translatable(key).withStyle(school.getDisplayName().getStyle());
        }

        // 翻译键建议："affix.examplemod.spell_bonus.desc": "+%2$s %1$s 等级"
        // 参数1: 目标名, 参数2: 数值
        MutableComponent comp = Component.translatable("affix.examplemod.spell_bonus.desc", targetName, current);

        // 如果是附魔台查看模式，且有浮动范围，显示范围 [min-max]
        if (augmenting) {
            int min = data.level().getInt(0);
            int max = data.level().getInt(1);
            if (min != max) {
                comp.append(Affix.valueBounds(Component.literal(String.valueOf(min)), Component.literal(String.valueOf(max))));
            }
        }

        return comp;
    }

    // 外部调用API：获取加成目标类型
    public boolean isSpellBonus() { return this.spell != null; }
    public AbstractSpell getSpell() { return this.spell; }
    public SchoolType getSchool() { return this.school; }

    // 获取数值
    public int getBonusLevel(LootRarity rarity, float level) {
        LevelData data = this.values.get(rarity);
        return data != null ? data.level().getInt(level) : 0;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat.isNone() || !this.values.containsKey(rarity)) return false;
        return this.validTypes.isEmpty() || this.validTypes.contains(cat);
    }
    @Override
    public Component getName(boolean prefix) {
        // 1. 如果是具体法术，直接借用法术原本的翻译
        if (this.spell != null) {
            // spell.getSpellName() 通常返回 "spell.irons_spellbooks.fireball" 这种key
            // 直接拿来翻译，不用自己造 key
            return  Component.translatable("spell."+spell.getSpellResource().getNamespace()+"."+spell.getSpellResource().getPath());
        }

        // 2. 如果是学派，直接用学派的显示名称
        if (this.school != null) {
            String key = "school." + school.getId().getNamespace() + "." + school.getId().getPath();
            return Component.translatable(key).withStyle(school.getDisplayName().getStyle());
        }

        return super.getName(prefix);
    }
    public record LevelData(StepFunction level) {
        public static final Codec<LevelData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(StepFunction.CODEC.optionalFieldOf("level", StepFunction.constant(1)).forGetter(LevelData::level))
                .apply(inst, LevelData::new));
    }
}