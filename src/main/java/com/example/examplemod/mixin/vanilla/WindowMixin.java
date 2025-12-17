package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.client.config.CustomScaleConfig;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Window.class)
public class WindowMixin {

    @ModifyVariable(method = "setGuiScale", at = @At("HEAD"), argsOnly = true)
    private double onSetGuiScale(double originalScale) {
        // 获取配置值
        // 注意：在极早期启动时可能需要判空，但在 setGuiScale 调用时通常 Config 已经加载完毕
        try {
            double custom = CustomScaleConfig.INSTANCE.scaleValue.get();
            // 如果大于 0，说明用户开启了自定义缩放
            if (custom > 0) {
                return custom;
            }
        } catch (Exception e) {
            // 防止 Config 未加载导致崩溃，虽然不太可能发生
        }

        return originalScale;
    }
}