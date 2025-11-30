package com.example.examplemod.affix;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;

/**
 * 词缀事件处理器
 * 用于处理 Apotheosis 词缀与 Iron's Spells 'n Spellbooks 法术系统之间的交互逻辑
 */
public class AffixEventHandler {

    /**
     * 注册事件监听器到 NeoForge 事件总线
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(new AffixEventHandler());
    }

    /**
     * 处理生物掉落物事件 (最低优先级执行)
     * 实现“心灵感应”(Telepathy) 词缀效果：如果是玩家造成的击杀且持有该词缀，掉落物直接传送到玩家位置。
     * 支持近战攻击和魔法投射物击杀。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropsLowest(LivingDropsEvent e) {
        DamageSource src = e.getSource();
        Entity directEntity = src.getDirectEntity(); // 直接造成伤害的实体（可能是投射物）
        Entity causingEntity = src.getEntity();      // 伤害来源的实体（通常是玩家）

        boolean canTeleport = false;
        Vec3 targetPos = null;
        ItemStack sourceWeapon;

        // 检查情况1：通过法术/投射物击杀 (例如火球术)
        if (directEntity instanceof Projectile spell && spell.getOwner() != null) {
            // 获取发射投射物的武器
            sourceWeapon = AffixHelper.getSourceWeapon(spell);
            if (!sourceWeapon.isEmpty()) {
                // 检查武器是否有开启“心灵感应”的词缀
                canTeleport = AffixHelper.streamAffixes(sourceWeapon).anyMatch(AffixInstance::enablesTelepathy);
                if (canTeleport) {
                    targetPos = spell.getOwner().position();
                }
            }
        }

        // 检查情况2：近战直接击杀，且之前没有触发传送
        if (!canTeleport && causingEntity instanceof LivingEntity living) {
            sourceWeapon = living.getMainHandItem();
            // 检查主手武器是否有开启“心灵感应”的词缀
            canTeleport = AffixHelper.streamAffixes(sourceWeapon).anyMatch(AffixInstance::enablesTelepathy);
            if (canTeleport) {
                targetPos = living.position();
            }
        }

        // 如果满足传送条件，将所有掉落物移动到目标位置
        if (canTeleport) {
            for (ItemEntity item : e.getDrops()) {
                item.setPos(targetPos.x, targetPos.y, targetPos.z);
                item.setPickUpDelay(0); // 设置拾取延迟为0，方便立刻吸入
            }
        }
    }

    /**
     * 处理法术造成伤害时的词缀触发
     * 例如：法术击中敌人时施加中毒，或者触发另一个法术
     */
    @SubscribeEvent
    public void hookSpellDamageAffix(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return; // 仅在服务端运行

        // 获取施法者
        LivingEntity caster = event.getSpellDamageSource().getEntity() instanceof LivingEntity living ? living : null;
        if (caster == null) return;

        // 遍历施法者所有装备槽位
        for (ItemStack stack : caster.getAllSlots()) {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                // 处理 SpellEffectAffix (施加药水效果等)
                if (inst.getAffix() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_DAMAGE_TARGET) {
                        // 目标是受害者
                        affix.applyEffectInternal(event.getEntity(), inst);
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_DAMAGE_SELF) {
                        // 目标是施法者自己
                        affix.applyEffectInternal(caster, inst);
                    }
                }
                // 处理 SpellTriggerAffix (触发连锁法术)
                else if (inst.getAffix() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_DAMAGE) {
                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case SELF -> caster;
                        case TARGET -> event.getEntity();
                    }).orElse(event.getEntity());

                    affix.triggerSpell(caster, target, inst);
                }
            });
        }
    }

    /**
     * 处理法术治疗时的词缀触发
     * 逻辑与伤害事件类似，但针对治疗事件
     */
    @SubscribeEvent
    public void hookSpellHealAffix(SpellHealEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity caster = event.getEntity(); // 治疗法术的施法者

        for (ItemStack stack : caster.getAllSlots()) {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                // 处理 SpellEffectAffix
                if (inst.getAffix() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_HEAL_TARGET) {
                        affix.applyEffectInternal(event.getTargetEntity(), inst);
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_HEAL_SELF) {
                        affix.applyEffectInternal(caster, inst);
                    }
                }
                // 处理 SpellTriggerAffix
                else if (inst.getAffix() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_HEAL) {
                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case SELF -> caster;
                        case TARGET -> event.getTargetEntity();
                    }).orElse(event.getTargetEntity());

                    affix.triggerSpell(caster, target, inst);
                }
            });
        }
    }

    /**
     * 处理法力值(Mana)变更事件，实现法力消耗减少词缀
     */
    @SubscribeEvent
    public void onChangeMana(ChangeManaEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        MagicData magicData = event.getMagicData();
        SpellData castingSpell = magicData.getCastingSpell();

        // 如果没有正在释放的法术，或者法力值是在增加（而不是消耗），则跳过
        if (castingSpell == null || event.getNewMana() >= event.getOldMana()) {
            return;
        }

        AbstractSpell spell = castingSpell.getSpell();
        if (spell == null) return;

        SchoolType spellSchool = spell.getSchoolType(); // 获取当前法术的学派

        // 仅检查主手物品 (例如法杖或近战武器)
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        float totalReduction = 0f;

        // 获取主手物品上的所有词缀
        Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(mainHand);
        for (AffixInstance instance : affixes.values()) {
            if (instance.isValid() && instance.affix().isBound()) {
                Affix affix = instance.getAffix();
                // 检查是否为法力减耗词缀 (ManaCostAffix) 且学派匹配
                if (affix instanceof ManaCostAffix manaCostAffix) {
                    if (manaCostAffix.getSchool() == spellSchool) {
                        float reduction = manaCostAffix.getReductionPercent(instance.getRarity(), instance.level());
                        totalReduction += reduction;
                    }
                }
            }
        }

        // 应用减免
        if (totalReduction > 0) {
            float manaCost = event.getOldMana() - event.getNewMana();
            // 计算新的消耗，最大减免限制为 90% (0.9f)
            float reducedCost = manaCost * (1 - Math.min(totalReduction, 0.9f));
            float newManaValue = event.getOldMana() - reducedCost;
            event.setNewMana(newManaValue);
        }
    }

    /**
     * 处理法术等级修改事件
     * 累加装备和饰品(Curios)提供的法术等级加成
     */
    @SubscribeEvent
    public void onSpellLevel(ModifySpellLevelEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;
        AbstractSpell spell = event.getSpell();
        SchoolType school = spell.getSchoolType();

        int totalBonus = 0;

        // 1. 遍历所有装备栏 (盔甲、手持物品)
        for (ItemStack stack : livingEntity.getAllSlots()) {
            totalBonus += getSpellLevelBonus(stack, school);
        }

        // 2. 遍历 Curios 饰品栏
        int curiosBonus = CuriosApi.getCuriosInventory(livingEntity)
                .map(curiosHandler -> {
                    int bonus = 0;
                    for (var slotResult : curiosHandler.findCurios(stack -> !stack.isEmpty())) {
                        bonus += getSpellLevelBonus(slotResult.stack(), school);
                    }
                    return bonus;
                })
                .orElse(0);

        totalBonus += curiosBonus;

        // 如果有加成，增加法术等级
        if (totalBonus > 0) {
            event.addLevels(totalBonus);
        }
    }

    /**
     * 辅助方法：计算单个物品提供的法术等级加成
     * @param stack 物品堆
     * @param school 目标法术学派
     * @return 增加的等级数
     */
    private int getSpellLevelBonus(ItemStack stack, SchoolType school) {
        int bonus = 0;
        Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(stack);
        for (AffixInstance instance : affixes.values()) {
            if (instance.isValid() && instance.affix().isBound()) {
                Affix affix = instance.getAffix();
                // 检查是否为法术等级词缀 (SpellLevelAffix) 且学派匹配
                if (affix instanceof SpellLevelAffix spellLevelAffix) {
                    if (spellLevelAffix.getSchool() == school) {
                        bonus += spellLevelAffix.getBonusLevel(instance.getRarity(), instance.level());
                    }
                }
            }
        }
        return bonus;
    }
}