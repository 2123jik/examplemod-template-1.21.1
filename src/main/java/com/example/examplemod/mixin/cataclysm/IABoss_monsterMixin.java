package com.example.examplemod.mixin.cataclysm;

import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.LLibrary_Boss_Monster;
import com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.IABoss_monster;
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
                    target = "Ljava/lang/Math;min(FF)F" // 指向静态方法 Math.min
            )
    )
    private float modifyDamageCapLogic(float cap, float damage) {

        return min(cap+getL2HostilityLevel(((LLibrary_Boss_Monster)(Object)this)).orElse(1), damage);
    }
    private static float min(float a, float b) {
        if (a != a)
            return a;   // a is NaN
        if ((a == 0.0f) &&
                (b == 0.0f) &&
                (Float.floatToRawIntBits(b) ==Float.floatToRawIntBits(-0.0f))) {
            // Raw conversion ok since NaN can't map to -0.0.
            return b;
        }
        return (a <= b) ? a : b;
    }
}
