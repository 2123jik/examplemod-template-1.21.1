package com.example.examplemod.client.layer;

import icyllis.modernui.graphics.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class RenderCustomHealth {
    static void renderCustomHealth(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) return;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthRatio = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0.0f, 1.0f);

        int x = width / 2 - 91;
        int y = height - 30;

        int barWidth = 5; // 血条宽度
        int barHeight = 80;

        int f=Color.argb(0.825f,0.625f,0.8f,0.125f);
        int t=Color.argb(1f,1.0f,0.125f,0.325f);
        graphics.fillGradient(x, y- barHeight, x+barWidth , y,f,  t);

        int filledWidth = (int) (barHeight * healthRatio);
        graphics.fillGradient(x + 1, y- filledWidth+ 1,  x+barWidth - 1, y - 1,f, t);
        String text = currentHealth + " / " + maxHealth;
        int textWidth = mc.font.width(text);
        int textX = x + (barWidth - textWidth) / 2;
        graphics.drawString(mc.font, text, textX, y, 0xFFFFFFFF, false);

    }
}
