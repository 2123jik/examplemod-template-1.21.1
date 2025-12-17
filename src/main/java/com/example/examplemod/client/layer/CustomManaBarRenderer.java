package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.joml.Matrix4f;

import static com.example.examplemod.client.layer.RenderCustomHealth.CIRCLE_VERTICES;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class CustomManaBarRenderer {


    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;

        // 1. 获取数值
        float maxMana = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
        float currentMana = ClientMagicData.getPlayerMana();

        // 如果没有最大蓝量（或者你想要隐藏的逻辑），直接返回
        if (maxMana <= 0) return;

        // 2. 准备绘制参数
        float pct = (currentMana / maxMana);
        if (pct < 0.0F||pct>1f) return;
        // 你原本的逻辑是 scale = 20 * pct，这意味着蓝量越少圆越小？
        // 还是说这是用来做类似血球的效果？保持你的原逻辑：
        float dy=30;
        float scale = dy * pct;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        // 3. 开始渲染状态
        guiGraphics.pose().pushPose();
        
        // --- 位置计算 ---
        // 放在屏幕宽度的 3/4 处 (右侧)，高度在底部上方 20 像素
        // 这里的 translate 是圆心的位置
        guiGraphics.pose().translate(screenWidth * 0.75f, screenHeight - dy, 0);
        
        // 缩放 (控制圆的大小)
        guiGraphics.pose().scale(scale, scale, scale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest(); // 禁用深度测试以确保它画在最上层
        RenderSystem.disableCull();      // 禁用剔除，保证正反面都能看到（虽然2D不需要）
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 4. 构建顶点
        Tesselator tesselator = Tesselator.getInstance();
        // 注意：1.21 写法变更，begin 返回 BufferBuilder
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = guiGraphics.pose().last().pose();

        // 遍历顶点数组
        // 假设数组结构是 [x, y, z] 为一组
        // 你的逻辑是：每次取 2 个外圈点 (i+3, i) 和 1 个圆心点 (0,0,0) 组成三角形
        for (int i = 0; i < CIRCLE_VERTICES.length - 5; i = i + 3) {
            // 顶点 1: 外圈点 B (蓝色)
            buffer.addVertex(matrix, CIRCLE_VERTICES[i + 3], CIRCLE_VERTICES[i + 4], CIRCLE_VERTICES[i + 5])
                  .setColor(0.0f, 0.0f, 0.8f, 1.0f);
            
            // 顶点 2: 外圈点 A (蓝色)
            buffer.addVertex(matrix, CIRCLE_VERTICES[i], CIRCLE_VERTICES[i + 1], CIRCLE_VERTICES[i + 2])
                  .setColor(0.0f, 0.0f, 0.8f, 1.0f);

            // 顶点 3: 圆心 (深蓝灰/半透明)
            buffer.addVertex(matrix, 0f, 0f, 0f)
                  .setColor(0.3f, 0.3f, 0.5f, 0.6f);
        }

        // 5. 提交绘制 (1.21 写法)
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // 6. 恢复状态
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }
}