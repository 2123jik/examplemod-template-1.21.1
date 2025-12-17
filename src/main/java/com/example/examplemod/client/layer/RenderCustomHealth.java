package com.example.examplemod.client.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

public class RenderCustomHealth {
    public final static float[] CIRCLE_VERTICES = new float[] {
            // --- 圆心 ---
            0.0000000f, 0.0000000f, 0.0000000f,

            // --- 圆周点 (从0度逆时针旋转) ---
            1.0000000f, 0.0000000f, 0.0000000f,  // 0度
            0.9951847f, 0.0980171f, 0.0000000f,
            0.9807853f, 0.1950903f, 0.0000000f,
            0.9569403f, 0.2902847f, 0.0000000f,
            0.9238795f, 0.3826834f, 0.0000000f,
            0.8819213f, 0.4713967f, 0.0000000f,
            0.8314696f, 0.5555702f, 0.0000000f,
            0.7730105f, 0.6343933f, 0.0000000f,
            0.7071068f, 0.7071068f, 0.0000000f,  // 45度
            0.6343933f, 0.7730105f, 0.0000000f,
            0.5555702f, 0.8314696f, 0.0000000f,
            0.4713967f, 0.8819213f, 0.0000000f,
            0.3826834f, 0.9238795f, 0.0000000f,
            0.2902847f, 0.9569403f, 0.0000000f,
            0.1950903f, 0.9807853f, 0.0000000f,
            0.0980171f, 0.9951847f, 0.0000000f,
            0.0000000f, 1.0000000f, 0.0000000f,  // 90度
            -0.0980171f, 0.9951847f, 0.0000000f,
            -0.1950903f, 0.9807853f, 0.0000000f,
            -0.2902847f, 0.9569403f, 0.0000000f,
            -0.3826834f, 0.9238795f, 0.0000000f,
            -0.4713967f, 0.8819213f, 0.0000000f,
            -0.5555702f, 0.8314696f, 0.0000000f,
            -0.6343933f, 0.7730105f, 0.0000000f,
            -0.7071068f, 0.7071068f, 0.0000000f,
            -0.7730105f, 0.6343933f, 0.0000000f,
            -0.8314696f, 0.5555702f, 0.0000000f,
            -0.8819213f, 0.4713967f, 0.0000000f,
            -0.9238795f, 0.3826834f, 0.0000000f,
            -0.9569403f, 0.2902847f, 0.0000000f,
            -0.9807853f, 0.1950903f, 0.0000000f,
            -0.9951847f, 0.0980171f, 0.0000000f,
            -1.0000000f, 0.0000000f, 0.0000000f, // 180度
            -0.9951847f, -0.0980171f, 0.0000000f,
            -0.9807853f, -0.1950903f, 0.0000000f,
            -0.9569403f, -0.2902847f, 0.0000000f,
            -0.9238795f, -0.3826834f, 0.0000000f,
            -0.8819213f, -0.4713967f, 0.0000000f,
            -0.8314696f, -0.5555702f, 0.0000000f,
            -0.7730105f, -0.6343933f, 0.0000000f,
            -0.7071068f, -0.7071068f, 0.0000000f,
            -0.6343933f, -0.7730105f, 0.0000000f,
            -0.5555702f, -0.8314696f, 0.0000000f,
            -0.4713967f, -0.8819213f, 0.0000000f,
            -0.3826834f, -0.9238795f, 0.0000000f,
            -0.2902847f, -0.9569403f, 0.0000000f,
            -0.1950903f, -0.9807853f, 0.0000000f,
            -0.0980171f, -0.9951847f, 0.0000000f,
            -0.0000000f, -1.0000000f, 0.0000000f, // 270度
            0.0980171f, -0.9951847f, 0.0000000f,
            0.1950903f, -0.9807853f, 0.0000000f,
            0.2902847f, -0.9569403f, 0.0000000f,
            0.3826834f, -0.9238795f, 0.0000000f,
            0.4713967f, -0.8819213f, 0.0000000f,
            0.5555702f, -0.8314696f, 0.0000000f,
            0.6343933f, -0.7730105f, 0.0000000f,
            0.7071068f, -0.7071068f, 0.0000000f,
            0.7730105f, -0.6343933f, 0.0000000f,
            0.8314696f, -0.5555702f, 0.0000000f,
            0.8819213f, -0.4713967f, 0.0000000f,
            0.9238795f, -0.3826834f, 0.0000000f,
            0.9569403f, -0.2902847f, 0.0000000f,
            0.9807853f, -0.1950903f, 0.0000000f,
            0.9951847f, -0.0980171f, 0.0000000f,

            // --- 闭合点 (必须回到起点) ---
            1.0000000f, 0.0000000f, 0.0000000f
    };
    public static void renderCustomHealth(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null||player.isCreative()) return;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;

        float c=(player.getHealth()/player.getMaxHealth())+0.01F;
        float dy=30;
        float scale=dy*c;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        graphics.pose().pushPose();
        graphics.pose().translate(screenWidth/4, screenHeight-dy, 0);
        graphics.pose().scale(scale,scale,scale);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        for (int i = 0; i < CIRCLE_VERTICES.length-5; i=i+3) {
            buffer.addVertex(matrix, CIRCLE_VERTICES[i+3], CIRCLE_VERTICES[i+4], CIRCLE_VERTICES[i+5]).setColor(0.8f,0,0,1f);
            buffer.addVertex(matrix, CIRCLE_VERTICES[i], CIRCLE_VERTICES[i+1], CIRCLE_VERTICES[i+2]).setColor(0.8f,0,0,1f);

            buffer.addVertex(matrix,0f,0f,0f).setColor(0.6f,0.3F,0.3F,0.6f);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        graphics.pose().popPose();

    }
}