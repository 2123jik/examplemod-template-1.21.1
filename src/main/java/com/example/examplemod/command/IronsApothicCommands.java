package com.example.examplemod.command;

import com.example.examplemod.util.SpellDiscoveryUtil;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class IronsApothicCommands {
    @SubscribeEvent
    public static void RegisterCommandsEvent(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("examplemod")
                        .requires(source -> source.hasPermission(2)) // Require OP level 2
                        .then(Commands.literal("spells")
                                .then(Commands.literal("list")
                                        .executes(context -> {
                                            SpellDiscoveryUtil.logAvailableSpells();
                                            context.getSource().sendSuccess(() ->
                                                            Component.literal("Available spells have been logged to the console/log file."),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                        ));
    }
} 