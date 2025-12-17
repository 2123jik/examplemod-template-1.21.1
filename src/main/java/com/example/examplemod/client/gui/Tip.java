package com.example.examplemod.client.gui;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber
public class Tip {
    @SubscribeEvent
    public static void tiprender(RenderGuiEvent.Pre event)
    {
//        event.getGuiGraphics().drawStringWithBackdrop(
//                Minecraft.getInstance().font,
//                Component.literal("按 G 键 打开 百科全书 查看 详细 说明"),
//                20,20,20, Color.ofARGB(0.1f,0.9f,0.2f,0.1f).getColor()
//        );
    }
}
