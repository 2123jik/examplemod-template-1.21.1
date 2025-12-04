package com.example.examplemod.mixin.cataclysm;

import com.example.examplemod.util.EffectUtils;
import com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.NewNetherite_Monstrosity.Netherite_Monstrosity_Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Netherite_Monstrosity_Entity.class)
public class NetheriteMonstrosityMixin {

    /**
     * 修改目标：aiStep 方法
     * 目的：在咆哮时给玩家施加效果
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        Netherite_Monstrosity_Entity boss = (Netherite_Monstrosity_Entity) (Object) this;
        
        // 只有在服务器端运行
        if (boss.level().isClientSide) return;

        int state = boss.getAttackState();
        int tick = boss.attackTicks;

        // 定义什么时候算“咆哮”。根据源码：
        // State 4 (二阶段转场): tick 16, 18, 20 有咆哮粒子
        // State 9 (Overpower/压制): tick 32, 34, 36 有咆哮粒子
        boolean isRoaring = (state == 2 && (tick == 2 ));

        if (isRoaring) {
            double range = 20.0; // 咆哮影响范围
            AABB area = boss.getBoundingBox().inflate(range);
            List<LivingEntity> targets = boss.level().getEntitiesOfClass(LivingEntity.class, area);

            for (LivingEntity target : targets) {
                // 排除 Boss 自己和队友
                if (target != boss && !boss.isAlliedTo(target)) {
                    // 或者你要加原版的虚弱/缓慢等
                     target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    EffectUtils.applyCombustionCurse(target, boss, 200, 3);
                }
            }
        }
    }

    /**
     * 修改目标：aiStep 方法中的 heal 调用
     * 目的：State 7 (Drain/汲取) 时的回血改为最大生命值的百分比
     * 原理：拦截 heal 方法的参数
     */
    @ModifyArg(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lcom/github/L_Ender/cataclysm/entity/InternalAnimationMonster/IABossMonsters/NewNetherite_Monstrosity/Netherite_Monstrosity_Entity;heal(F)V"),
        index = 0 // heal(float amount) 的第一个参数
    )
    private float modifyDrainHealAmount(float originalAmount) {
        Netherite_Monstrosity_Entity boss = (Netherite_Monstrosity_Entity) (Object) this;

        // 确保只在 State 7 (汲取岩浆) 时修改
        if (boss.getAttackState() == 7) {
            // 原版数值是 15，基础血量 600，占比 2.5%
            // 这里我们改为：最大生命值的 2.5% (或者你可以改为 5% 等)
            // 这样如果你在配置里把血量改成 10000，它回血也会相应变成 250
            return boss.getMaxHealth() * 0.025F; 
        }


        return originalAmount; // 其他情况下的治疗保持原样
    }
}