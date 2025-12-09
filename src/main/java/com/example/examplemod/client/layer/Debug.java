package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
import dev.xkmc.l2core.capability.player.PlayerCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
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
import net.minecraft.network.chat.Component; // 重要：引入 Component
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.examplemod.util.AttributeHelper.getL2HostilityLevel;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class Debug {

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
            List<Component> debugLines = new ArrayList<>();

            // --- 1. FPS ---
            // 对应 key: gui.examplemod.debug.fps -> "[ FPS: %s ]"
            debugLines.add(Component.translatable("gui." + ExampleMod.MODID + ".debug.fps", mc.getFps())
                    .withStyle(ChatFormatting.GOLD));

            // --- 2. 坐标 (X, Y, Z) ---
            // 我们先格式化数字为字符串 (保留2位小数)，再传给翻译组件
            String xVal = String.format(Locale.ROOT, "%.2f", player.getX());
            String yVal = String.format(Locale.ROOT, "%.2f", player.getY());
            String zVal = String.format(Locale.ROOT, "%.2f", player.getZ());

            debugLines.add(Component.translatable("gui." + ExampleMod.MODID + ".debug.x", xVal).withStyle(ChatFormatting.RED));
            debugLines.add(Component.translatable("gui." + ExampleMod.MODID + ".debug.y", yVal).withStyle(ChatFormatting.GREEN));
            debugLines.add(Component.translatable("gui." + ExampleMod.MODID + ".debug.z", zVal).withStyle(ChatFormatting.BLUE));

            // --- 3. 方向 (Facing) ---
            // 技巧：直接利用原版的翻译键。原版方向键格式为 "direction.minecraft.north" 等
            // player.getDirection().getName() 会返回 "north", "east" 等
            String directionKey = "direction.minecraft." + player.getDirection().getName();

            // 组合： "朝向: " + "北"
            MutableComponent facingLine = Component.translatable("gui." + ExampleMod.MODID + ".debug.facing",
                    Component.translatable(directionKey).withStyle(ChatFormatting.WHITE) // 将方向作为参数传入
            ).withStyle(ChatFormatting.YELLOW);

            debugLines.add(facingLine);

            // --- 4. 生物群系 (Biome) ---
            BlockPos pos = player.blockPosition();
            Holder<Biome> biomeHolder = mc.level.getBiome(pos);

            // 获取 Biome 的翻译组件。如果有注册名会自动查找 "biome.minecraft.plains"，如果是 Mod 的也会尝试查找对应的 key
            Component biomeName = Component.literal(biomeHolder.unwrapKey().map(k -> k.location().toString()).orElse("Unknown"));
            // 尝试获取更友好的翻译名 (如果有)
            if (biomeHolder.unwrapKey().isPresent()) {
                biomeName = Component.translatable(Util.makeDescriptionId("biome", biomeHolder.unwrapKey().get().location()));
            }

            debugLines.add(Component.translatable("gui." + ExampleMod.MODID + ".debug.biome",
                    biomeName.copy().withStyle(ChatFormatting.WHITE)
            ).withStyle(ChatFormatting.AQUA));
                debugLines.add(Component.literal("恶意等级："+ ((PlayerDifficulty)((PlayerCapabilityHolder) LHMiscs.PLAYER.type()).getOrCreate(player)).getLevel(player).getStr()).withStyle(ChatFormatting.YELLOW));
            int startX = 10;
            int startY = 400;
            int lineHeight = font.lineHeight + 2;
            int padding = 2;
            int backgroundColor = 0x80000000;

            for (Component line : debugLines) {
                int textWidth = font.width(line);

                guiGraphics.fill(startX - padding, startY - padding, startX + textWidth + padding, startY + font.lineHeight + padding - 1, backgroundColor);

                guiGraphics.drawString(font, line, startX, startY, 0xFFFFFFFF, true);


                startY += lineHeight;
            }
        }
    }
}