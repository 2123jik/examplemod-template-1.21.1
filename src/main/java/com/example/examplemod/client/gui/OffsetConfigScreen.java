package com.example.examplemod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class OffsetConfigScreen extends Screen {

    public OffsetConfigScreen() {
        super(Component.literal("Item Render Config"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // X 轴滑块
        this.addRenderableWidget(new OffsetSlider(
                centerX - 100, centerY - 40, 200, 20,
                Component.literal("X Offset"),
                RenderOffsetConfig.x, -2.0D, 2.0D,
                (val) -> RenderOffsetConfig.x = val
        ));

        // Y 轴滑块
        this.addRenderableWidget(new OffsetSlider(
                centerX - 100, centerY - 10, 200, 20,
                Component.literal("Y Offset"),
                RenderOffsetConfig.y, -2.0D, 2.0D,
                (val) -> RenderOffsetConfig.y = val
        ));

        // Z 轴滑块
        this.addRenderableWidget(new OffsetSlider(
                centerX - 100, centerY + 20, 200, 20,
                Component.literal("Z Offset"),
                RenderOffsetConfig.z, -2.0D, 2.0D,
                (val) -> RenderOffsetConfig.z = val
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // >>>>>>>>>> 修复点在这里 <<<<<<<<<<
        // 旧版本写法: this.renderBackground(guiGraphics);
        // 新版本/NeoForge写法 (需要4个参数):
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // 自定义滑块内部类
    private static class OffsetSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final java.util.function.Consumer<Double> onApply;
        private final Component prefix;

        public OffsetSlider(int x, int y, int width, int height, Component prefix, double currentValue, double min, double max, java.util.function.Consumer<Double> onApply) {
            // 父类构造函数：x, y, width, height, text, initialValue(0.0-1.0)
            super(x, y, width, height, Component.empty(), 0);
            this.prefix = prefix;
            this.min = min;
            this.max = max;
            this.onApply = onApply;

            // 计算滑块进度的初始值 (0.0 到 1.0 之间)
            this.value = (Mth.clamp(currentValue, min, max) - min) / (max - min);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            double actualValue = this.min + (this.max - this.min) * this.value;
            String valStr = String.format("%.2f", actualValue);
            this.setMessage(Component.literal("").append(prefix).append(": " + valStr));
        }

        @Override
        protected void applyValue() {
            double actualValue = this.min + (this.max - this.min) * this.value;
            this.onApply.accept(actualValue);
        }
    }
}