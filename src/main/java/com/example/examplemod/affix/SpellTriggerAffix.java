package com.example.examplemod.affix;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.util.SpellCastUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.*;

import static com.example.examplemod.util.SpellCastUtil.getTotalSpellLevelBonus;

public class SpellTriggerAffix extends Affix {

    // 【修改点1】使用 Set 记录正在触发的词条ID，允许不同词条连锁，只防止同类词条自我嵌套
    private static final ThreadLocal<Set<ResourceLocation>> ACTIVE_EXECUTIONS = ThreadLocal.withInitial(HashSet::new);

    public static final Codec<SpellTriggerAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.affixDef(),
                    SpellRegistry.REGISTRY.holderByNameCodec().fieldOf("spell").forGetter(a -> a.spell),
                    TriggerType.CODEC.fieldOf("trigger").forGetter(a -> a.trigger),
                    LootRarity.mapCodec(TriggerData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types),
                    TargetType.CODEC.optionalFieldOf("target").forGetter(a -> a.target))
            .apply(inst, SpellTriggerAffix::new));

    protected final Holder<AbstractSpell> spell;
    protected final TriggerType trigger;
    protected final Map<LootRarity, TriggerData> values;
    protected final Set<LootCategory> types;
    protected final Optional<TargetType> target;

    public SpellTriggerAffix(AffixDefinition definition, Holder<AbstractSpell> spell, TriggerType trigger,
                             Map<LootRarity, TriggerData> values, Set<LootCategory> types,
                             Optional<TargetType> target) {
        super(definition);
        this.spell = spell;
        this.trigger = trigger;
        this.values = values;
        this.types = types;
        this.target = target;
    }

    public void triggerSpell(LivingEntity caster, LivingEntity target, AffixInstance inst) {
        triggerSpell(caster, target, inst.rarity().get(), inst.level());
    }

    private void triggerSpell(LivingEntity caster, LivingEntity target, LootRarity rarity, float level) {
        if (caster == null || target == null) {
            return;
        }
        ResourceLocation affixId = this.id();

        // 【修改点2】基于 ID 的递归保护
        Set<ResourceLocation> active = ACTIVE_EXECUTIONS.get();
        if (active.contains(affixId)) {
            return;
        }

        TriggerData data = this.values.get(rarity);
        if (data == null || caster.level().isClientSide()) return;

        int baseLevel = data.level().getInt(level);
        AbstractSpell spellInstance = this.spell.value();
        int bonusLevel = getTotalSpellLevelBonus(caster, spellInstance);
        int finalSpellLevel = baseLevel + bonusLevel;

        int cooldown = data.cooldown();

        // 【修改点3】严格的冷却检查
        // 移除了 !hasActiveRecast 的判断。词条触发是强制性的额外效果，
        // 不应该因为玩家手动施法状态(Recast)而获得无冷却特权。
        if (cooldown > 0 && Affix.isOnCooldown(affixId, cooldown, caster)) {
            return;
        }

        try {
            // 锁定当前词条 ID
            active.add(affixId);

            try {
                SpellCastUtil.castSpell(caster, spellInstance, finalSpellLevel, target);
            } catch (Exception e) {
                ExampleMod.LOGGER.error("Failed to cast spell via affix: " + affixId, e);
            }

            // 【修改点4】无论施法是否成功进入 Recast 状态，词条本身必须进入冷却
            // 这可以防止瞬间高频触发（例如持续性伤害每tick都触发）
            if (cooldown > 0) {
                Affix.startCooldown(affixId, caster);
            }
        } finally {
            // 解锁
            active.remove(affixId);
        }
    }

    // ... (GetCodec, canApplyTo, getDescription 等方法保持不变) ...
    // ... (doPostAttack, doPostHurt 等钩子方法保持不变) ...

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (this.types.isEmpty() || this.types.contains(cat)) && this.values.containsKey(rarity);
    }

    // ... 省略 getDescription, getAugmentingText 实现以节省篇幅，逻辑不需要变 ...
    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        TriggerData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();
        String triggerKey = "affix.examplemod.trigger." + this.trigger.name().toLowerCase();
        AbstractSpell spellInstance = this.spell.value();
        int spellLevel = data.level().getInt(inst.level());
        Component coloredSpellName = spellInstance.getDisplayName(null).copy()
                .append(" ")
                .append(Component.translatable("enchantment.level." + spellLevel))
                .withStyle(spellInstance.getSchoolType().getDisplayName().getStyle());
        boolean isSelfCast = this.target.map(t -> t == TargetType.SELF).orElse(false);
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;
        MutableComponent comp = Component.translatable(finalKey, coloredSpellName);
        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }
        return comp;
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        TriggerData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();
        int currentLevel = data.level().getInt(inst.level());
        AbstractSpell spellInstance = this.spell.value();
        Component coloredSpellName = spellInstance.getDisplayName(null).copy()
                .append(" ")
                .append(Component.translatable("enchantment.level." + currentLevel))
                .withStyle(spellInstance.getSchoolType().getDisplayName().getStyle());
        boolean isSelfCast = this.target.map(t -> t == TargetType.SELF).orElse(false);
        String triggerKey = "affix.examplemod.trigger." + this.trigger.name().toLowerCase();
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;
        MutableComponent comp = Component.translatable(finalKey, coloredSpellName);
        int minLevel = data.level().getInt(0);
        int maxLevel = data.level().getInt(1);
        if (minLevel != maxLevel) {
            Component minComp = Component.translatable("enchantment.level." + minLevel);
            Component maxComp = Component.translatable("enchantment.level." + maxLevel);
            comp.append(Affix.valueBounds(minComp, maxComp));
        }
        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }
        return comp;
    }

    // --- Hooks ---

    @Override
    public void doPostAttack(AffixInstance inst, LivingEntity user, Entity target) {
        if (this.trigger == TriggerType.MELEE_HIT && target instanceof LivingEntity livingTarget) {
            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> user;
                case TARGET -> livingTarget;
            }).orElse(livingTarget);
            triggerSpell(user, actualTarget, inst);
        }
    }

    @Override
    public void doPostHurt(AffixInstance inst, LivingEntity user, DamageSource source) {
        if (this.trigger == TriggerType.HURT && source.getEntity() instanceof LivingEntity attacker) {
            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> user;
                case TARGET -> attacker;
            }).orElse(user);
            triggerSpell(user, actualTarget, inst);
        }
    }

    @Override
    public void onProjectileImpact(float level, LootRarity rarity, Projectile proj, HitResult res, HitResult.Type type) {
        // 1. 基础检查：必须是弹射物触发类型，且投射物必须有发射者（作为施法源）
        if (this.trigger != TriggerType.PROJECTILE_HIT) return;

        if (!(proj.getOwner() instanceof LivingEntity owner)) return;

        LivingEntity actualTarget = null;

        // 2. 获取目标策略，如果未配置默认为 TARGET (攻击命中目标)
        TargetType targetType = this.target.orElse(TargetType.TARGET);

        if (targetType == TargetType.SELF) {
            // 【关键修复】：如果是对自己释放（例如命中回血），不需要管击中的是墙壁还是怪物
            // 只要发生了碰撞(Impact)，目标就是发射者自己
            actualTarget = owner;

        } else {
            // 如果是针对目标释放（例如命中释放火球），则必须击中生物
            if (type == HitResult.Type.ENTITY
                    && res instanceof EntityHitResult entityHit
                    && entityHit.getEntity() instanceof LivingEntity hitEntity
            ) {

                actualTarget = hitEntity;
            }
        }

        // 3. 执行施法
        if (actualTarget != null) {
            System.out.println("GOOD");
            triggerSpell(owner, actualTarget, rarity, level);
        }
    }

    @Override
    public float onShieldBlock(AffixInstance inst, LivingEntity entity, DamageSource source, float amount) {
        if (this.trigger == TriggerType.SHIELD) {
            Entity attacker = source.getEntity();
            LivingEntity actualTarget = this.target.map(t -> {
                if (t == TargetType.SELF) return entity;
                if (t == TargetType.TARGET && attacker instanceof LivingEntity le) return le;
                return entity;
            }).orElse(entity);
            if (actualTarget != null) {
                this.triggerSpell(entity, actualTarget, inst);
            }
        }
        return amount;
    }

    @Override
    public Component getName(boolean prefix) {
        return Component.translatable("spell." + spell.value().getSpellResource().getNamespace() + "." + spell.value().getSpellResource().getPath());
    }

    public enum TriggerType {
        SPELL_DAMAGE, SPELL_HEAL, MELEE_HIT, PROJECTILE_HIT, HURT, SHIELD;
        public static final Codec<TriggerType> CODEC = PlaceboCodecs.enumCodec(TriggerType.class);
    }

    public enum TargetType {
        SELF, TARGET;
        public static final Codec<TargetType> CODEC = PlaceboCodecs.enumCodec(TargetType.class);
    }

    public record TriggerData(StepFunction level, int cooldown) {
        private static final Codec<TriggerData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        StepFunction.CODEC.optionalFieldOf("level", StepFunction.constant(1)).forGetter(TriggerData::level),
                        Codec.INT.optionalFieldOf("cooldown", 0).forGetter(TriggerData::cooldown))
                .apply(inst, TriggerData::new));
    }
}