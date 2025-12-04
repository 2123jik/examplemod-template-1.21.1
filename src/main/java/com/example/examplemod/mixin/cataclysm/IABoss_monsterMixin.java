package com.example.examplemod.mixin.cataclysm;

import com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.IABoss_monster;
import net.minecraft.world.entity.LivingEntity; // 引入 LivingEntity
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.example.examplemod.util.AttributeHelper.getL2HostilityLevel;

@Mixin(IABoss_monster.class)
public class IABoss_monsterMixin {

    /**
     * 目标：拦截 hurt 方法中 Math.min(float, float) 的调用
     * 原代码逻辑：damage = Math.min(this.DamageCap(), damage);
     */
    @Redirect(
            method = "hurt",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;min(FF)F"
            )
    )
    private float modifyDamageCapLogic(float cap, float damage) {
        // 修复点：将 (LLibrary_Boss_Monster) 改为 (LivingEntity)
        // IABoss_monster 也是 LivingEntity，所以这是安全的
        return min(cap + getL2HostilityLevel(((LivingEntity)(Object)this)).orElse(1), damage);
    }

    private static float min(float a, float b) {
        if (a != a)
            return a;   // a is NaN
        if ((a == 0.0f) &&
                (b == 0.0f) &&
                (Float.floatToRawIntBits(b) == Float.floatToRawIntBits(-0.0f))) {
            return b;
        }
        return (a <= b) ? a : b;
    }
}