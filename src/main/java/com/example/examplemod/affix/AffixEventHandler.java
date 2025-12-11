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
     * 修复版：支持傀儡等从属生物触发
     */
    @SubscribeEvent
    public void hookSpellDamageAffix(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return; // 仅在服务端运行

        // --- 核心修复开始 ---
        LivingEntity caster = null;
        DamageSource source = event.getSpellDamageSource();
        Entity directEntity = source.getDirectEntity(); // 直接造成伤害的实体（如火球、魔法飞弹）
        Entity ownerEntity = source.getEntity();       // 伤害归属者（通常被系统判定为玩家）

        // 策略1：如果是弹射物/法术实体，尝试直接获取发射者 (Shooter)
        // 在 Minecraft 底层，Projectile.getOwner() 通常指向发射它的物理实体（傀儡），
        // 而不是被伤害系统重定向后的逻辑主人（玩家）。
        if (directEntity instanceof Projectile projectile) {
            Entity shooter = projectile.getOwner();
            if (shooter instanceof LivingEntity livingShooter) {
                caster = livingShooter;
            }
        }

        // 策略2：如果策略1失败（不是弹射物，或者是瞬发法术），回退使用归属者
        if (caster == null && ownerEntity instanceof LivingEntity livingOwner) {
            caster = livingOwner;
        }

        // 如果还是找不到施法者，直接返回
        if (caster == null) return;
        // --- 核心修复结束 ---

        // 调试建议：如果你想验证，可以取消下面这行的注释。
        // 成功时，你应该看到 "施法者: MetalGolemEntity"，而不是 "施法者: Player"
        // System.out.println("DEBUG: 法术伤害触发 - 施法者: " + caster.getName().getString() + " | 手持: " + caster.getMainHandItem());

        // 遍历施法者所有装备槽位
        for (ItemStack stack : caster.getAllSlots()) {
            LivingEntity finalCaster = caster;
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                // 处理 SpellEffectAffix (施加药水效果等)
                if (inst.getAffix() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_DAMAGE_TARGET) {
                        // 目标是受害者
                        affix.applyEffectInternal(event.getEntity(), inst);
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_DAMAGE_SELF) {
                        // 目标是施法者自己
                        affix.applyEffectInternal(finalCaster, inst);
                    }
                }
                // 处理 SpellTriggerAffix (触发连锁法术)
                else if (inst.getAffix() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_DAMAGE) {
                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case SELF -> finalCaster; // 这里现在会正确指向傀儡
                        case TARGET -> event.getEntity();
                    }).orElse(event.getEntity());

                    affix.triggerSpell(finalCaster, target, inst);
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