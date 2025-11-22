package com.example.examplemod.command;

import com.example.examplemod.util.SpellDiscoveryUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class IronsApothicCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
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
                )
        );
    }
} 