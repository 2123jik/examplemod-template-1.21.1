package com.example.examplemod.affix;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import static com.example.examplemod.server.util.ServerEventUtils.forEachItem;
import static com.example.examplemod.util.SpellCastUtil.getTotalSpellLevelBonus;

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
        Entity directEntity = src.getDirectEntity();
        Entity causingEntity = src.getEntity();

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
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity caster = null;
        DamageSource source = event.getSpellDamageSource();
        Entity directEntity = source.getDirectEntity();
        Entity ownerEntity = source.getEntity();

        // 1. 确定施法者
        if (directEntity instanceof Projectile projectile) {
            Entity shooter = projectile.getOwner();
            if (shooter instanceof LivingEntity livingShooter) {
                caster = livingShooter;
            }
        }
        if (caster == null && ownerEntity instanceof LivingEntity livingOwner) {
            caster = livingOwner;
        }
        if (caster == null) return;

        final LivingEntity finalCaster = caster;

        // 2. 只有当受害者是生物时才触发 (防止 NullPointerException)
        Entity victimEntity = event.getEntity();
        if (!(victimEntity instanceof LivingEntity livingVictim)) return;

        forEachItem(caster, (stack) -> {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.getAffix() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_DAMAGE) {

                    // 3. 安全的目标选择逻辑
                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case TARGET -> livingVictim; // 确保使用已经转型的 livingVictim
                        case SELF -> finalCaster;
                    }).orElse(livingVictim);

                    // 4. 只有目标不为空时才触发
                    if (target != null) {
                        affix.triggerSpell(finalCaster, target, inst);
                    }
                }
            });
        });
    }

    /**
     * 处理法术治疗时的词缀触发
     * 逻辑与伤害事件类似，但针对治疗事件
     */
    @SubscribeEvent
    public void hookSpellHealAffix(SpellHealEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity caster = event.getEntity();
        LivingEntity healedEntity = event.getTargetEntity();

        // 如果被治疗者为空，直接跳过
        if (healedEntity == null) return;

        forEachItem(caster, (stack) -> {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.getAffix() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_HEAL) {

                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case TARGET -> healedEntity;
                        case SELF -> caster;
                    }).orElse(healedEntity);

                    if (target != null) {
                        affix.triggerSpell(caster, target, inst);
                    }
                }
            });
        });
    }



    /**
     * 处理法术等级修改事件 (Iron's Spells 原生系统)
     * 累加装备和饰品提供的法术等级加成
     */
    @SubscribeEvent
    public void onSpellLevel(ModifySpellLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        // 调用公共逻辑计算加成
        int bonus = getTotalSpellLevelBonus(living, event.getSpell());

        if (bonus > 0) {
            event.addLevels(bonus);
        }
    }
}