package com.example.examplemod.mixin.apothic;

import com.example.examplemod.client.tooltip.TooltipResolver;
import dev.shadowsoffire.apothic_attributes.client.AttributesGui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(AttributesGui.class)
public abstract class AttributesGuiIconMixin {

    // 线程局部变量：存储当前条目是否有图标，用于调整名称渲染位置
    private static final ThreadLocal<Boolean> hasIcon = ThreadLocal.withInitial(() -> false);

    // 图标尺寸：9x9，与 TooltipRenderer 保持一致
    private static final int ICON_DISPLAY_SIZE = 9;
    // 文本与图标之间的间距
    private static final int ICON_PADDING = 2;
    // 总偏移量
    private static final int X_SHIFT = ICON_DISPLAY_SIZE + ICON_PADDING;

    /**
     * 第一部分：注入图标绘制逻辑。
     * 目标：绘制条目背景之后。
     */
    @Inject(method = "renderEntry",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void apothicattributes$injectIcon(GuiGraphics gfx, AttributeInstance inst, int x, int y, int mouseX, int mouseY, CallbackInfo ci, boolean hover) {

        // 1. 获取属性的未本地化键
        String unlocalizedKey = inst.getAttribute().value().getDescriptionId();

        // 2. 获取图标绘制器 (通过 INSTANCE 调用，触发懒加载)
        TooltipResolver.IconDrawer drawer = TooltipResolver.INSTANCE.getDrawer(unlocalizedKey);

        if (drawer != null) {
            hasIcon.set(true);

            // 3. 计算图标位置 (在条目左侧，垂直居中)
            // 条目高度通常是 22。 垂直居中：y + (22 - ICON_DISPLAY_SIZE) / 2
            int iconY = y + (22 - ICON_DISPLAY_SIZE) / 2; // 22 - 9 = 13. 13 / 2 = 6.5 -> 6
            int iconX = x + 2; // 距离左侧 2 像素

            PoseStack stack = gfx.pose();
            stack.pushPose();

            // 提高 Z 轴，确保在 GUI 元素之上渲染
            stack.translate(0, 0, 100);

            // 4. 使用 IconDrawer 绘制图标
            drawer.draw(gfx, iconX, iconY, ICON_DISPLAY_SIZE);

            stack.popPose();
        } else {
            hasIcon.set(false);
        }
    }

    /**
     * 第二部分：重定向属性名称的 X 坐标。
     * 目标：捕获并修改绘制属性名称的 drawString 调用中的 X 坐标参数。
     */
    @Redirect(method = "renderEntry",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
                    ordinal = 0)) // 假设 ordinal 0 是绘制属性名称的调用
    private int apothicattributes$redirectNameX(GuiGraphics instance, Font font, Component component, int x, int y, int color, boolean dropShadow) {

        // 如果上一步绘制了图标，则向右偏移
        if (hasIcon.get()) {
            x += X_SHIFT;
        }

        // 执行原始的 drawString 方法
        return instance.drawString(font, component, x, y, color, dropShadow);
    }

    /**
     * 第三部分：清理 ThreadLocal 状态。
     * 目标：在 renderEntry 结束时清理状态，防止状态泄露到下一个条目。
     */
    @Inject(method = "renderEntry", at = @At("RETURN"))
    private void apothicattributes$cleanup(GuiGraphics gfx, AttributeInstance inst, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        hasIcon.remove();
    }
}