package com.example.examplemod.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.text.DecimalFormat;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class HeadLayerConfig {

    // === 数据定义 ===
    public static double transX = 0, transY = 0, transZ = 0;
    public static double rotX = 0, rotY = 0, rotZ = 0;
    public static double scaleX = 1.0, scaleY = 1.0, scaleZ = 1.0;

    // === 配置界面 ===
    public static class ConfigScreen extends Screen {
        private static final DecimalFormat DF = new DecimalFormat("0.00");

        public ConfigScreen() {
            super(Component.literal("Head Layer Config"));
        }

        @Override
        protected void init() {
            int y = 40, gap = 25, w = 150, h = 20;
            int col1 = width / 2 - 155, col2 = width / 2 + 5;

            // 平移
            addSlider(col1, y, "Trans X", -2, 2, () -> transX, v -> transX = v);
            addSlider(col2, y, "Trans Y", -2, 2, () -> transY, v -> transY = v);
            addSlider(col1, y + gap, "Trans Z", -2, 2, () -> transZ, v -> transZ = v);

            // 旋转 (下一组)
            y += gap * 2 + 10;
            addSlider(col1, y, "Rot X", -180, 180, () -> rotX, v -> rotX = v);
            addSlider(col2, y, "Rot Y", -180, 180, () -> rotY, v -> rotY = v);
            addSlider(col1, y + gap, "Rot Z", -180, 180, () -> rotZ, v -> rotZ = v);

            // 缩放 (下一组)
            y += gap * 2 + 10;
            addSlider(col1, y, "Scale X", 0.1, 3, () -> scaleX, v -> scaleX = v);
            addSlider(col2, y, "Scale Y", 0.1, 3, () -> scaleY, v -> scaleY = v);
            addSlider(col1, y + gap, "Scale Z", 0.1, 3, () -> scaleZ, v -> scaleZ = v);

            // 重置按钮
            addRenderableWidget(Button.builder(Component.literal("Reset All"), b -> {
                transX = transY = transZ = rotX = rotY = rotZ = 0;
                scaleX = scaleY = scaleZ = 1.0;
                this.rebuildWidgets(); // 刷新滑块位置
            }).bounds(col2, y + gap, w, h).build());
        }

        /**
         * 快速添加滑块的辅助方法
         * @param getter 获取当前值的 Lambda (例如: () -> transX)
         * @param setter 设置新值的 Lambda (例如: v -> transX = v)
         */
        private void addSlider(int x, int y, String label, double min, double max,
                               java.util.function.Supplier<Double> getter, Consumer<Double> setter) {
            double current = getter.get();
            addRenderableWidget(new AbstractSliderButton(x, y, 150, 20, Component.empty(), (current - min) / (max - min)) {
                { updateMessage(); } // 构造块中立即更新文字

                @Override
                protected void updateMessage() {
                    double val = min + (max - min) * value;
                    setMessage(Component.literal(label + ": " + DF.format(val)));
                }

                @Override
                protected void applyValue() {
                    setter.accept(min + (max - min) * value);
                }
            });
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}