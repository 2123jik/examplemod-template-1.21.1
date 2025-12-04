package com.example.examplemod.mixin.vanilla;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobEffects.class)
public class MobEffectsMixin {

    /**
     * 拦截 MobEffects 静态初始化期间所有的 addAttributeModifier 调用。
     * 我们通过判断 ResourceLocation 的 ID 来确定当前正在注册哪个效果。
     */
    @Redirect(
            method = "<clinit>", // 目标是静态初始化块
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffect;addAttributeModifier(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceLocation;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Lnet/minecraft/world/effect/MobEffect;"
            )
    )
    private static MobEffect modifyAttributeModifiers(
            MobEffect instance,
            Holder<Attribute> attribute,
            ResourceLocation id,
            double amount,
            AttributeModifier.Operation operation
    ) {
        // 判断是否是 "力量" (strength) 效果的 ID
        // 原版代码中写的是: ResourceLocation.withDefaultNamespace("effect.strength")
        if (id.getPath().equals("effect.strength")) {
            
            // 在这里修改数值和算法
            
            // 示例 1: 将原版的 +3.0 攻击力改为 +10.0
            double newAmount = 0.125;
            
            // 示例 2: 将算法从 ADD_VALUE (直接加算) 改为 ADD_MULTIPLIED_TOTAL (总乘算，即百分比)
            // 如果改为乘算，通常数值要小一点，比如 0.5 代表 +50%
            AttributeModifier.Operation newOperation = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL; 

            // 返回修改后的调用
            return instance.addAttributeModifier(attribute, id, newAmount, newOperation);
        }

        // 判断是否是 "虚弱" (weakness)
        if (id.getPath().equals("effect.weakness")) {
            // 比如把虚弱改成减少 50% 攻击力，而不是固定的 -4.0
            return instance.addAttributeModifier(attribute, id, -0.166, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        // 对于其他所有未修改的效果，保持原样返回
        return instance.addAttributeModifier(attribute, id, amount, operation);
    }
}