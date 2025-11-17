package com.example.examplemod.register;

import com.example.examplemod.command.ModCommands;
import dev.xkmc.modulargolems.content.entity.metalgolem.MetalGolemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber
public class Register {
    @SubscribeEvent
    public static void onServerStarting(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}
