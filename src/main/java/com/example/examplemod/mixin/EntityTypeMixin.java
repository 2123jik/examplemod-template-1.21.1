package com.example.examplemod.mixin;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin {

    /**
     * 这个 Mixin 使用 @ModifyArg 注解，精准地拦截了对 EntityType.Builder.updateInterval(int) 方法的调用。
     * 它只会在 EntityType 类的静态初始化块 (<clinit>) 中，为 ARROW 实体进行初始化时生效，
     * 并将其中的参数 `20` 修改为 `1`。
     *
     * @param originalInterval 原始的 updateInterval 值 (在这里就是 20)
     * @return 修改后的新值 (在这里是 1)
     */
    @ModifyArg(
            // method: 目标是静态初始化块 <clinit>
            method = "<clinit>",
            // at: 注入点是调用 updateInterval 方法的地方
            at = @At(
                    value = "INVOKE",
                    // target: 精确指向 EntityType.Builder 的 updateInterval(int) 方法
                    target = "Lnet/minecraft/world/entity/EntityType$Builder;updateInterval(I)Lnet/minecraft/world/entity/EntityType$Builder;"
            ),
            index = 0
    )
    private static int modifyArrowUpdateInterval(int originalInterval) {
        if (originalInterval == 20) {
            System.out.println("Mixin applied: Changing Arrow update interval from 20 to 1!"); // 调试日志
            return 1; // 返回新值
        }
        return originalInterval;
    }
}