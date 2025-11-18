package com.example.examplemod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record SpellBonusData(
    int universalBonus,
    Map<SchoolType, Integer> schoolBonuses,
    Map<ResourceLocation, Integer> spellBonuses
) {
    public static final SpellBonusData EMPTY = new SpellBonusData(0, Map.of(), Map.of());

    public static final Codec<SpellBonusData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("universal_bonus", 0).forGetter(SpellBonusData::universalBonus),
        Codec.unboundedMap(SchoolRegistry.REGISTRY.byNameCodec(), Codec.INT).optionalFieldOf("school_bonuses", Map.of()).forGetter(SpellBonusData::schoolBonuses),
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("spell_bonuses", Map.of()).forGetter(SpellBonusData::spellBonuses)
    ).apply(instance, SpellBonusData::new));

    /**
     * 计算给定法术的总等级加成。
     * 优先级：特定法术 > 学派 > 全局
     * @param spellId 要检查的法术ID
     * @param schoolType 要检查的法术学派
     * @return 最终的等级加成值
     */
    public int getTotalBonusFor(ResourceLocation spellId, SchoolType schoolType) {
        // 优先使用特定法术的加成
        if (spellBonuses.containsKey(spellId)) {
            return spellBonuses.get(spellId);
        }
        // 其次使用学派加成
        if (schoolBonuses.containsKey(schoolType)) {
            return schoolBonuses.get(schoolType);
        }
        // 最后使用全局加成
        return universalBonus;
    }

    // (可选) 创建一个 Builder 模式，方便在代码中构建实例
    public static class Builder {
        private int universalBonus = 0;
        private final Map<SchoolType, Integer> schoolBonuses = new java.util.HashMap<>();
        private final Map<ResourceLocation, Integer> spellBonuses = new java.util.HashMap<>();

        public Builder setUniversal(int bonus) {
            this.universalBonus = bonus;
            return this;
        }

        public Builder addSchoolBonus(SchoolType school, int bonus) {
            this.schoolBonuses.put(school, bonus);
            return this;
        }

        public Builder addSpellBonus(ResourceLocation spellId, int bonus) {
            this.spellBonuses.put(spellId, bonus);
            return this;
        }

        public SpellBonusData build() {
            return new SpellBonusData(universalBonus, schoolBonuses, spellBonuses);
        }
    }
}