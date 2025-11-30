package com.example.examplemod.register;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.SkyDolphin;
import com.example.examplemod.register.ModEntities;
import net.minecraft.client.renderer.entity.DolphinRenderer; // 使用原版海豚渲染器
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ModEventBusEvents {

    // 1. 注册实体属性 (解决启动崩溃问题)
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SKY_DOLPHIN.get(), SkyDolphin.createSkyDolphinAttributes().build());
    }

    // 2. 注册实体渲染器 (解决看不见实体的问题)
    // 这一步必须在客户端执行，因此加上 value = Dist.CLIENT
    @EventBusSubscriber(modid = ExampleMod.MODID,value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // 直接复用原版 DolphinRenderer，这样它看起来就和普通海豚一样
            event.registerEntityRenderer(ModEntities.SKY_DOLPHIN.get(), DolphinRenderer::new);
        }
    }
}