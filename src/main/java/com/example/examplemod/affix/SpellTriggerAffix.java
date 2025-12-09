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
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 自定义词条：法术触发词条 (SpellTriggerAffix)。
 * <p>
 * 作用：赋予装备在特定条件下自动施法的能力。
 * 例如：
 * - 攻击时有几率释放"雷电术"。
 * - 受伤时释放"治疗术"。
 * - 格挡时释放"震荡波"。
 */
public class SpellTriggerAffix extends Affix {

    // 线程局部变量，用于防止无限递归触发。
    // 例如：法术造成伤害 -> 触发 SPELL_DAMAGE -> 再次施法 -> 再次造成伤害...
    private static final ThreadLocal<Boolean> IS_TRIGGERING = ThreadLocal.withInitial(() -> false);

    // 定义 JSON 数据结构 (Codec)
    public static final Codec<SpellTriggerAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.affixDef(), // 基础 ID
                    SpellRegistry.REGISTRY.holderByNameCodec().fieldOf("spell").forGetter(a -> a.spell), // 要触发的法术 ID
                    TriggerType.CODEC.fieldOf("trigger").forGetter(a -> a.trigger), // 触发条件 (攻击/受伤/格挡等)
                    LootRarity.mapCodec(TriggerData.CODEC).fieldOf("values").forGetter(a -> a.values), // 不同稀有度的数值 (法术等级、冷却)
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types), // 允许生成的物品类型
                    TargetType.CODEC.optionalFieldOf("target").forGetter(a -> a.target)) // 目标类型 (自己/敌人)，可选
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

    /**
     * 公共触发入口，通常由外部事件调用。
     */
    public void triggerSpell(LivingEntity caster, LivingEntity target, AffixInstance inst) {
        triggerSpell(caster, target, inst.rarity().get(), inst.level());
    }

    /**
     * 核心逻辑：执行法术施放。
     */
    private void triggerSpell(LivingEntity caster, LivingEntity target, LootRarity rarity, float level) {
        // 1. 递归保护：如果当前线程正在处理触发逻辑，直接返回，防止死循环
        if (IS_TRIGGERING.get()) {
            return;
        }

        // 2. 获取当前稀有度的配置数据
        TriggerData data = this.values.get(rarity);
        if (data == null || caster.level().isClientSide()) return; // 仅在服务端执行

        int spellLevel = data.level().getInt(level); // 计算法术等级
        AbstractSpell spellInstance = this.spell.value();
        String spellId = spellInstance.getSpellId();

        // 3. 检查玩家数据，看是否是多重施法 (Recast) 状态
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        boolean hasActiveRecast = magicData.getPlayerRecasts().hasRecastForSpell(spellId);

        // 4. 冷却检查
        // 如果不是处于 Recast 状态，且配置了冷却时间，检查该词条是否还在冷却中
        int cooldown = data.cooldown();
        if (!hasActiveRecast && cooldown != 0 && Affix.isOnCooldown(this.id(), cooldown, caster)) {
            return;
        }

        try {
            // 设置正在触发标志位，防止后续逻辑再次触发本方法
            IS_TRIGGERING.set(true);
// 这是一个安全措施，防止因为缺少 SpellCastUtil 导致整个服务器崩溃
            try {
            // 5. 执行施法 (调用工具类)
            SpellCastUtil.castSpell(caster, spellInstance, spellLevel, target);
        } catch (NoClassDefFoundError | Exception e) {
            // 打印错误但不要让服务器崩溃
            ExampleMod.LOGGER.error("Failed to cast spell via affix. Is Iron's Spells installed?", e);
        }
            // 6. 施法成功后，启动冷却 (如果是 Recast 状态通常不消耗冷却)
            if (!hasActiveRecast && cooldown != 0) {
                Affix.startCooldown(this.id(), caster);
            }
        } finally {
            // 无论成功与否，清除标志位
            IS_TRIGGERING.set(false);
        }
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    // 检查词条是否能应用到物品上 (基于配置的 types 和 rarity)
    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (this.types.isEmpty() || this.types.contains(cat)) && this.values.containsKey(rarity);
    }

    // 获取物品提示文本 (Tooltip)
    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        TriggerData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        // 构建本地化键，例如 "affix.examplemod.trigger.melee_hit"
        String triggerKey = "affix.examplemod.trigger." + this.trigger.name().toLowerCase();
        AbstractSpell spellInstance = this.spell.value();
        int spellLevel = data.level().getInt(inst.level());

        // 构建带颜色的法术名称组件 (颜色取决于魔法派系)
        Component coloredSpellName = spellInstance.getDisplayName(null).copy()
                .append(" ")
                .append(Component.translatable("enchantment.level." + spellLevel))
                .withStyle(spellInstance.getSchoolType().getDisplayName().getStyle());

        // 如果配置了 self 目标，使用 .self 后缀的翻译键
        boolean isSelfCast = this.target.map(t -> t == TargetType.SELF).orElse(false);
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;

        MutableComponent comp = Component.translatable(finalKey, coloredSpellName);

        // 如果有冷却时间，追加显示冷却信息
        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }

        return comp;
    }

    // 获取重铸/附魔时的增强文本 (显示数值范围)
    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        // ... (逻辑与 getDescription 类似，但额外显示了最小/最大等级范围)
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

        // 显示等级波动范围 (例如: [等级 I - III])
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

    // --- 事件钩子：根据触发类型执行逻辑 ---

    /**
     * 近战攻击后触发 (MELEE_HIT)
     */
    @Override
    public void doPostAttack(AffixInstance inst, LivingEntity user, Entity target) {
        if (this.trigger == TriggerType.MELEE_HIT && target instanceof LivingEntity livingTarget) {
            // 根据配置决定目标：如果是 SELF 则是攻击者(玩家)，否则是被攻击者(怪物)
            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> user;
                case TARGET -> livingTarget;
            }).orElse(livingTarget); // 默认为被攻击者

            triggerSpell(user, actualTarget, inst);
        }
    }

    /**
     * 玩家攻击/受伤后触发 (HURT)
     * 注意：Apotheosis 的 doPostHurt 通常是指"佩戴者受到伤害后"
     */
    @Override
    public void doPostHurt(AffixInstance inst, LivingEntity user, DamageSource source) {
        if (this.trigger == TriggerType.HURT && source.getEntity() instanceof LivingEntity attacker) {
            // 目标逻辑：SELF = 受伤者(自己)，TARGET = 攻击来源(敌人)
            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> user;
                case TARGET -> attacker;
            }).orElse(user); // 默认为自己 (例如触发护盾术)

            triggerSpell(user, actualTarget, inst);
        }
    }

    /**
     * 弹射物命中后触发 (PROJECTILE_HIT)
     * 例如弓箭射中敌人时
     */
    @Override
    public void onProjectileImpact(float level, LootRarity rarity, Projectile proj, HitResult res, HitResult.Type type) {
        if (this.trigger == TriggerType.PROJECTILE_HIT && type == HitResult.Type.ENTITY &&
                res instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity hitEntity &&
                proj.getOwner() instanceof LivingEntity owner) { // 必须有发射者 (Owner)

            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> owner; // 目标是射箭的人
                case TARGET -> hitEntity; // 目标是中箭的人
            }).orElse(hitEntity);

            triggerSpell(owner, actualTarget, rarity, level);
        }
    }

    /**
     * 盾牌格挡后触发 (SHIELD)
     */
    @Override
    public float onShieldBlock(AffixInstance inst, LivingEntity entity, DamageSource source, float amount) {
        // 只有当这个词缀配置为 SHIELD 触发时才执行
        if (this.trigger == TriggerType.SHIELD) {
            Entity attacker = source.getEntity();

            // 判定目标逻辑：
            // SELF -> 持盾者 (如回血)
            // TARGET -> 攻击者 (如反伤)
            LivingEntity actualTarget = this.target.map(t -> {
                if (t == TargetType.SELF) return entity;
                if (t == TargetType.TARGET && attacker instanceof LivingEntity le) return le;
                return entity; // 默认回退到自己
            }).orElse(entity);

            // 触发法术
            if (actualTarget != null) {
                this.triggerSpell(entity, actualTarget, inst);
            }
        }
        return amount;
    }

    /**
     * 枚举：定义的触发时机类型
     */
    public enum TriggerType {
        SPELL_DAMAGE, // 法术造成伤害时
        SPELL_HEAL,   // 法术治疗时
        MELEE_HIT,    // 近战攻击时
        PROJECTILE_HIT, // 弹射物命中时
        HURT,         // 受伤时
        SHIELD;       // 举盾格挡时
        public static final Codec<TriggerType> CODEC = PlaceboCodecs.enumCodec(TriggerType.class);
    }

    /**
     * 枚举：法术释放的目标类型
     */
    public enum TargetType {
        SELF,   // 对自己释放
        TARGET; // 对目标释放

        public static final Codec<TargetType> CODEC = PlaceboCodecs.enumCodec(TargetType.class);
    }

    /**
     * 记录类：存储触发相关的数值配置
     * @param level 法术等级函数 (根据词条稀有度和随机值计算)
     * @param cooldown 冷却时间 (Ticks)
     */
    public record TriggerData(StepFunction level, int cooldown) {
        private static final Codec<TriggerData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        StepFunction.CODEC.optionalFieldOf("level", StepFunction.constant(1)).forGetter(TriggerData::level),
                        Codec.INT.optionalFieldOf("cooldown", 0).forGetter(TriggerData::cooldown))
                .apply(inst, TriggerData::new));
    }
}