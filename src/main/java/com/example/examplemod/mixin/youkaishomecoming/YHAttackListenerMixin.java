package com.example.examplemod.mixin.youkaishomecoming;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.youkaishomecoming.events.YHAttackListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.example.examplemod.util.AttributeHelper.getL2HostilityLevel;

// 记得导入你的 Attribute 类
// import com.yourname.yourmod.init.YourAttributes;

@Mixin(YHAttackListener.class)
public class YHAttackListenerMixin {

    /**
     * 使用 Redirect 接管 DamageModifier.add 的调用
     *
     * 方法签名必须包含：
     * 1. 原方法的参数: (float value, ResourceLocation loc)
     * 2. 外层方法(onDamage)的参数: (DamageData.Defence data) - 用于获取实体
     */
    @Redirect(
            method = "onDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/xkmc/l2damagetracker/contents/attack/DamageModifier;add(FLnet/minecraft/resources/ResourceLocation;)Ldev/xkmc/l2damagetracker/contents/attack/DamageModifier;"
            )
    )
    private DamageModifier redirectReduction(float originalValue, ResourceLocation loc, DamageData.Defence data) {
        LivingEntity entity = data.getTarget();

        var factor=
        (LHConfig.SERVER.damageFactor.getAsDouble()*(double) getL2HostilityLevel(entity).getAsInt()+1);
        originalValue*= (float) factor;

        return DamageModifier.add(originalValue, loc);
    }
}