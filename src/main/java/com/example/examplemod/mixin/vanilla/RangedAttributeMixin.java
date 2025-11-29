package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RangedAttribute.class)
public class RangedAttributeMixin {

    /**
     * 这个 Mixin 会拦截 RangedAttribute 的构造函数。
     * 它在构造函数的最开始 (HEAD) 注入，修改名为 'max' 的局部变量（即构造函数的第四个参数）。
     * 这样，当游戏创建 Attributes.MAX_HEALTH 时，它的上限就从 1024 变成了 1024000。
     */
    @ModifyVariable(
            method = "<init>",      // 目标是构造函数
            at = @At("HEAD"),       // 在构造函数入口处注入，此时参数刚传入，可以修改
            ordinal = 2,            // 目标是第三个 double 类型的参数 (ordinal=0 是 defaultValue, ordinal=1 是 min)
            argsOnly = true         // 确保我们只在参数中查找变量，而不是所有局部变量
    )
    private static double modifyMaxValueInConstructor(double max) {
        // 如果原来的上限是 1024.0 (最大生命值)，我们就把它放大。
        if (
                max == 1024.0 ||
                max == 2048.0
        ) {
            return max * 1000.0;
        }
        // 对于其他属性，保持原样
        return max;
    }
}