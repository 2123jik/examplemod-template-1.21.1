package com.example.examplemod.mixin;

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

        return Math.min(cap, damage)+getL2HostilityLevel(((IABoss_monster)(Object)this)).orElse(1);
    }
}
