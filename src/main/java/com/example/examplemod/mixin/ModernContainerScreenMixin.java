package com.example.examplemod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ModernContainerScreenMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    /**
     * 修复点：
     * 1. method 目标改为 "renderBackground"。这是一个实实在在的方法，不是抽象的。
     * 2. at 目标改为 INVOKE 调用 "renderBg"。
     * 3. 即使 renderBg 是 protected，在 target 字符串中引用它是合法的，只要签名对即可。
     */
    @Inject(
            method = "renderBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"
            )
    )
    private void renderModernShadow(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // 此时：
        // 1. 屏幕已经变暗了 (renderTransparentBackground 已执行)
        // 2. GUI 纹理还没画 (renderBg 即将执行)
        // 所以我们直接在这里画阴影，不需要搞复杂的 Z轴 (translate)，直接画就行。

        int shadowColorStart = 0x70000000; // 黑色，半透明 (0x70 约 40% 不透明度)
        int shadowColorEnd   = 0x00000000; // 透明
        int range = 8; // 阴影扩散范围，越大越柔和

        // 1. 绘制右侧阴影 (从左到右渐变)
        guiGraphics.fillGradient(
                leftPos + imageWidth, topPos,
                leftPos + imageWidth + range, topPos + imageHeight,
                shadowColorStart, shadowColorEnd
        );

        // 2. 绘制底部阴影 (从上到下渐变)
        guiGraphics.fillGradient(
                leftPos, topPos + imageHeight,
                leftPos + imageWidth, topPos + imageHeight + range,
                shadowColorStart, shadowColorEnd
        );

        // 3. 绘制右下角角落 (对角线渐变比较难模拟，这里用两个矩形叠加模拟圆角阴影)
        guiGraphics.fillGradient(
                leftPos + imageWidth, topPos + imageHeight,
                leftPos + imageWidth + range, topPos + imageHeight + range,
                shadowColorStart, shadowColorEnd
        );

        // 4. (可选) 在整个 GUI 背后画一个纯黑衬底，防止有些半透明材质穿帮
        // guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
    }

    /**
     * 现代化的槽位高亮
     * 保持不变，这段逻辑是独立的
     */
    @Overwrite
    public static void renderSlotHighlight(GuiGraphics guiGraphics, int x, int y, int blitOffset, int color) {
        // 纯白色高亮，不透明
        int highlightColor = 0xFFFFFFFF;

        // 绘制 1像素宽 的空心边框
        // Top
        guiGraphics.fill(x, y, x + 16, y + 1, blitOffset, highlightColor);
        // Bottom
        guiGraphics.fill(x, y + 15, x + 16, y + 16, blitOffset, highlightColor);
        // Left
        guiGraphics.fill(x, y, x + 1, y + 16, blitOffset, highlightColor);
        // Right
        guiGraphics.fill(x + 15, y, x + 16, y + 16, blitOffset, highlightColor);

        // 内部填充极淡的白色，增加选中质感
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, blitOffset, 0x15FFFFFF);

        // 确保混合模式正确，防止影响后续渲染
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }
}