package com.example.examplemod.client.screen;

import com.example.examplemod.ExampleMod;
import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.Objects;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ChestScreenOverlay {
    @SubscribeEvent
    public static void onRenderGuiForeground(ContainerScreenEvent.Render.Foreground event) {
        var guiGraphics = event.getGuiGraphics();
        var screen = event.getContainerScreen();

        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem() && slot.getItem().has(Apoth.Components.RARITY)) {
                // 1. 获取 DynamicHolder
                // 警告: 这里假设 Apoth.Components.RARITY 的类型是 DynamicHolder<LootRarity>
                var rarityHolder = slot.getItem().get(Apoth.Components.RARITY);

                // 2. 检查 DynamicHolder 是否已绑定（即是否已加载）
                // DynamicHolder 源码中有一个 isBound() 方法
                if (rarityHolder != null && rarityHolder.isBound()) {
                    // 3. 只有在已绑定时才调用 get()
                    int argbColor = rarityHolder.get().color().getValue() | 0xFF000000;
                    int x = slot.x;
                    int y = slot.y;
                    int size = 2;
                    guiGraphics.fill(x, y, x + size, y + size, 0, argbColor);
                }
            }

            // PURITY 部分保持不变
            if (slot.hasItem() && slot.getItem().has(Apoth.Components.PURITY)) {
                int argbColor = Objects.requireNonNull(slot.getItem().get(Apoth.Components.PURITY)).getColor().getValue();
                int x = slot.x;
                int y = slot.y;
                int size = 2;
                guiGraphics.fill(x, y, x + size, y + size, 0, argbColor);
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        // 确保是客户端玩家
        if (!(event.getPlayer() instanceof AbstractClientPlayer)) {
            return;
        }
        event.setNewFovModifier(1f);
    }

}