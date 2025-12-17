package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class NetworkHandler {


    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ExampleMod.MODID)
                .versioned("1.0.0"); // 建议加上版本号




        registrar.playToServer(
                OpenBlockInHandPayload.TYPE,
                OpenBlockInHandPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        ServerPacketHandler.handleOpenBlockInHand(payload, serverPlayer);
                    }
                })
        );
    }
}