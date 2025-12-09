package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.util.CritArcRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientDamageHandler {

    private static float currentDamage = 0;
    private static long lastHitTime = 0;
    private static int currentCritType = 0;
    private static int currentLayers = 0;

    private static final long RESET_MS = 2000;
    private static final long FADE_MS = 500;

    // --- 视觉配置参数 (在此处调整) ---
    // 15度角非常锐利，看起来像刻度盘或能量刺
    private static final float ARC_ANGLE = 10.0f;
    // 缩放倍率
    private static final float ARC_SCALE = 2.5f;
    // 旋转角度: 0=3点钟方向, -45=右上方, -90=12点钟方向
    // 设为 -45 度让它出现在准星的右上方，或者 135 度在左下方
    private static final float ARC_ROTATION = -55.0f;
    // 距离屏幕中心的偏移量
    private static final float OFFSET_X =100.0f;
    private static final float OFFSET_Y = -100.0f;

    // --- 1. 处理网络包 ---
    public static void handlePacket(float amount, int critType, int layers) {
        long now = System.currentTimeMillis();
        if (now - lastHitTime > RESET_MS) {
            currentDamage = 0;
            currentCritType = 0;
            currentLayers = 0;
        }
        currentDamage += amount;

        if (critType > currentCritType || (critType == currentCritType && layers > currentLayers)) {
            currentCritType = critType;
            currentLayers = layers;
        }

        lastHitTime = now;
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "dmg_hud"), new DamageHudLayer());
    }

    // --- 2. 渲染层 ---
    public static class DamageHudLayer implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
            long now = System.currentTimeMillis();
            long timeAlive = now - lastHitTime;

            if (timeAlive >= RESET_MS || currentDamage <= 0) return;

            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;

            // 屏幕中心
            float cx = mc.getWindow().getGuiScaledWidth() / 2.0f;
            float cy = mc.getWindow().getGuiScaledHeight() / 2.0f;

            // 淡出 Alpha
            float alpha = 1.0f;
            if (timeAlive > (RESET_MS - FADE_MS)) {
                alpha = (float)(RESET_MS - timeAlive) / FADE_MS;
            }

            // 打击感缩放 (pop) - 文字跳动
            float popScale = 1.0f;
            if (timeAlive < 100) {
                popScale = 1.2f - 0.2f * (timeAlive / 100.0f);
            }

            // --- 3. 绘制神化暴击弧 (Crit Arc) ---

            // 逻辑：如果是神化暴击(type3)，传递层数；否则层数为0
            int renderLayers = (currentCritType == 3) ? currentLayers : 0;

            // 逻辑：如果是 L2暴击(type2) 或者 神化层数破格(type3 & layers>=10)，开启金光
            boolean isL2 = (currentCritType == 2) || (currentCritType == 3 && currentLayers >= 10);

            // 计算圆弧位置 (基于准星 + 偏移量)
            float arcX = cx + OFFSET_X;
            float arcY = cy + OFFSET_Y;

            // 调用新的渲染方法，传入配置参数
            // 注意：只有当有层数 或者 是L2暴击时，Renderer 才会绘制，所以这里不用再手动 if 判断
            CritArcRenderer.render(
                    guiGraphics,
                    arcX, arcY,
                    renderLayers,
                    isL2,
                    alpha,
                    ARC_SCALE,
                    ARC_ROTATION,
                    ARC_ANGLE
            );

            // --- 4. 绘制文字 ---
            // 文字位置稍微往下挪一点，避免遮挡圆弧
            String text = String.format("%.1f", currentDamage);
            int color = 0xFFFFFF;

            if (currentCritType == 3) color = 0xFF55FF; // 紫 (神化)
            else if (currentCritType == 2) color = 0xFF5555; // 红 (暴击)
            else if (currentCritType == 1) color = 0xFFFF55; // 黄 (普通)

            int finalColor = ((int)(alpha * 255) << 24) | color;

            guiGraphics.pose().pushPose();
            // 文字放在准星右下方一点，与圆弧形成对角呼应
            guiGraphics.pose().translate(cx +OFFSET_X+ 8, cy +OFFSET_Y+ 8, 0);
            guiGraphics.pose().scale(popScale, popScale, 1f);

            // 绘制文字
            guiGraphics.drawString(font, text, 0, 0, finalColor, true);

            guiGraphics.pose().popPose();
        }
    }
}