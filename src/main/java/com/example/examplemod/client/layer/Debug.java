package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
import dev.xkmc.l2core.capability.player.PlayerCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import mod.azure.azurelib.core.object.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class Debug {
    public static double scale=-1;
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "debug"), new DebugHudLayer());
    }

    public static class DebugHudLayer implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.options.hideGui) {
                return;
            }
            LocalPlayer player = mc.player;
            Font font = mc.font;
            int currentFps = mc.getFps();

            String xVal = String.format(Locale.ROOT, "%.1f", player.getX());
            String yVal = String.format(Locale.ROOT, "%.1f", player.getY());
            String zVal = String.format(Locale.ROOT, "%.1f", player.getZ());

            String difficulty = ((PlayerDifficulty)((PlayerCapabilityHolder) LHMiscs.PLAYER.type()).getOrCreate(player)).getLevel(player).getStr();

            String facing = player.getDirection().getName().toUpperCase();

            BlockPos pos = player.blockPosition();
            Holder<Biome> biomeHolder = mc.level.getBiome(pos);
            Component biomeName = Component.literal(biomeHolder.unwrapKey().map(k -> k.location().getPath()).orElse("Unknown"));
            if (biomeHolder.unwrapKey().isPresent()) {
                biomeName = Component.translatable(Util.makeDescriptionId("biome", biomeHolder.unwrapKey().get().location()));
            }

            MutableComponent fullText = Component.empty();
            var experienceLevel=player.experienceLevel;
            var experienceProgress=Math.floor(player.experienceProgress*100);

            fullText.append(entry("饥饿值", player.getFoodData().getFoodLevel() + "/" + 40));
            fullText.append(separator());
            fullText.append(entry("等级",String.valueOf(experienceLevel)));
            fullText.append(separator());
            fullText.append(entry("离升级还差", experienceProgress + "%"));
            fullText.append(separator());
            fullText.append(entry("当前", String.valueOf(currentFps)));
            fullText.append(separator());
            fullText.append(entry("x", xVal));
            fullText.append(separator());
            fullText.append(entry("y", yVal));
            fullText.append(separator());
            fullText.append(entry("z", zVal));
            fullText.append(separator());
            fullText.append(Component.literal("群系:").withStyle(ChatFormatting.GRAY))
                    .append(biomeName.copy().withStyle(ChatFormatting.WHITE)); // 群系名字特殊处理
            fullText.append(separator());
            fullText.append(entry("难度", difficulty));
            fullText.append(separator());
            fullText.append(entry("朝向", facing));

            // --- 3. 渲染逻辑 (紧贴右上角) ---

            int screenWidth = guiGraphics.guiWidth();
            int textWidth = font.width(fullText);

            // 边距设置
            int marginX = 2; // 距离右边缘的像素
            int marginY = 2; // 距离顶部的像素
            int padding = 2; // 背景黑框的内边距

            // 计算左上角起始 X 坐标：屏幕宽 - 文本宽 - 边距
            int startX = screenWidth - textWidth - marginX;
            int startY = marginY;

            // 绘制背景 (黑色半透明)
            guiGraphics.fill(startX - padding, startY - padding, startX + textWidth + padding, startY + font.lineHeight + padding - 1, Color.ofRGBA(0.5f,0.5f,0.5f,0.2f).getColor());

            // 绘制文本
            guiGraphics.drawString(font, fullText, startX, startY, 0xFFFFFFFF, true);
        }

        // 辅助函数：生成 "标签:数值" 格式
        private static MutableComponent entry(String label, String value) {
            return Component.literal(label + ":").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
        }

        // 辅助函数：生成分隔符 " | "
        private static Component separator() {
            return Component.literal(" | ").withStyle(ChatFormatting.GRAY);
        }
    }
}