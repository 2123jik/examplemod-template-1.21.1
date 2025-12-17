package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class CustomSpellBarRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "textures/gui/icons.png");

    private static final int ICON_SIZE = 22; // 法术栏背景框是 22x22
    private static final int GAP_X = 24;     // 间隔
    private static final int GAP_Y = 24;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;

        // 获取法术数据
        var ssm = ClientMagicData.getSpellSelectionManager();
        if (ssm.getSpellCount() <= 0) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        
        // 1. 准备渲染环境 (透明度等)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getRendertypeTranslucentShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // 2. 计算起始位置 (为了与你的 Effect 对称)
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Y轴: 保持与你的 Effect 一致 (屏幕底部 - 固定高度)
        // 注意：RenderCustomEffect 中是 currentY -= GAP_Y，这里我们固定一行
        int startY = screenHeight - (20 + ICON_SIZE);

        // X轴: 屏幕中心向右
        // 你的 Effect 是 (Width/2) - ICON_SIZE 向左
        // 这里我们从 (Width/2) 开始，加一点间距，向右
        int startX = (screenWidth / 2) -100;

        List<SpellData> spells = ssm.getAllSpells().stream().map(slot -> slot.spellData).toList();
        int selectedSpellIndex = ssm.getGlobalSelectionIndex();

        // 3. 循环渲染 (X 轴变化)
        for (int i = 0; i < spells.size(); i++) {
            SpellData spellData = spells.get(i);
            
            // 关键：位置计算 (线性向右)
            int currentX = startX + (i * (GAP_X-2));
            int currentY = startY;

            // --- A. 绘制背景框 ---
            // 66, 84 是 Iron 原版纹理中的背景框坐标
            guiGraphics.blit(TEXTURE, currentX, currentY, 66, 84, 22, 22);

            // --- B. 绘制法术图标 ---
            // 图标是 16x16，背景是 22x22，所以偏移 +3 居中
            guiGraphics.blit(spellData.getSpell().getSpellIconResource(), currentX + 3, currentY + 3, 0, 0, 16, 16, 16, 16);

            // --- C. 绘制边框 (未选中时) ---
            if (i != selectedSpellIndex) {
                // 判断是法术书还是卷轴，边框不同
                boolean isSpellBookSlot = ssm.getAllSpells().get(i).slot.equals(Curios.SPELLBOOK_SLOT);
                int u = 22 + (!isSpellBookSlot ? 110 : 0); // 卷轴边框在纹理右侧
                guiGraphics.blit(TEXTURE, currentX, currentY, u, 84, 22, 22);
            }

            // --- D. 绘制冷却 (Cooldown) ---
            float f = ClientMagicData.getCooldownPercent(spellData.getSpell());
            if (f > 0) {
                int pixels = (int) (16 * f + 1f);
                // 47, 87 是冷却遮罩纹理
                guiGraphics.blit(TEXTURE, currentX + 3, currentY + 19 - pixels, 47, 87, 16, pixels);
            }

            // --- E. 绘制选中高亮框 ---
            if (i == selectedSpellIndex) {
                // 0, 84 是选中高亮纹理
                guiGraphics.blit(TEXTURE, currentX, currentY, 0, 84, 22, 22);
            }
        }

        // 恢复渲染状态
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}