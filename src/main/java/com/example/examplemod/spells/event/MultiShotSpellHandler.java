package com.example.examplemod.spells.event;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile;
import io.redspace.ironsspellbooks.spells.fire.FireboltSpell; // 导入 FireboltSpell 类
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;


@EventBusSubscriber(modid = ExampleMod.MODID)
public class MultiShotSpellHandler {

    @SubscribeEvent
    public static void handleFireboltMultiShot(SpellPreCastEvent event) {
//        Player caster = event.getEntity();
//        String spellId = event.getSpellId();
//        event.setCanceled(true);
//        AbstractSpell abstractSpell = SpellRegistry.getSpell(spellId);
//        if (!(abstractSpell instanceof FireboltSpell fireboltSpell)) {
//            return;
//        }
//
//        // 获取施法参数
//        Level world = caster.level();
//        int spellLevel = event.getSpellLevel();
//
//        // --- 多重射击逻辑 ---
//
//        // 附魔等级决定发射次数 (例如: 1级=3发, 2级=5发, 3级=7发)
//        final int numProjectiles = 1 + 2 * 2;
//
//        // 总偏转角度：30度
//        final float TOTAL_SPREAD_DEGREES = 30.0F;
//
//        // 计算基础伤害 (我们必须复制 FireboltSpell 中的伤害计算逻辑)
//        // FireboltSpell: return this.getSpellPower(spellLevel, entity) * 0.5F;
//        float rawPower = fireboltSpell.getSpellPower(spellLevel, caster);
//        float baseDamage = rawPower * 0.5F;
//
//        // 分摊伤害：每发弹丸的伤害
//        float damagePerProjectile = baseDamage /1;
//
//        // 获取初始方向向量
//        Vec3 initialLook = caster.getLookAngle();
//
//        // 角度计算 (转换为弧度)
//        double spreadRad = Math.toRadians(TOTAL_SPREAD_DEGREES);
//        // 如果多于一发，起始角度是总角度的一半负值
//        double startAngleRad = numProjectiles > 1 ? -spreadRad / 2.0 : 0;
//        // 角度增量
//        double angleIncrementRad = numProjectiles > 1 ? spreadRad / (numProjectiles - 1) : 0;
//
//        // --- 循环发射弹丸 ---
//        for (int i = 0; i < numProjectiles; i++) {
//
//            // 计算当前弹丸需要偏转的角度
//            double currentAngleRad = startAngleRad + i * angleIncrementRad;
//
//            // 使用 Vec3.yRot 绕Y轴旋转，实现水平偏转
//            Vec3 newDirection = initialLook.yRot((float)currentAngleRad);
//
//            // 实例化并设置弹丸 (使用原版 FireboltProjectile)
//            FireboltProjectile firebolt = new FireboltProjectile(world, caster);
//
//            // 设置弹丸位置 (复制原版 FireboltSpell 的位置逻辑)
//            firebolt.setPos(caster.position().add(
//                0.0,
//                caster.getEyeHeight() - firebolt.getBoundingBox().getYsize() * 0.5,
//                0.0
//            ));
//
//            firebolt.shoot(newDirection);
//            firebolt.setDamage(damagePerProjectile);
//
//            world.addFreshEntity(firebolt);
//        }

    }
}