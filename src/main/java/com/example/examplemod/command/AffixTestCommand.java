package com.example.examplemod.command;

import com.example.examplemod.util.AffixCompatibilityTester;
import com.example.examplemod.util.CompatibilityReport;
import com.example.examplemod.util.SpellCastUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.whisent.powerful_dummy.Powerful_dummy;
import com.whisent.powerful_dummy.entity.TestDummyEntity;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static com.whisent.powerful_dummy.entity.DummyEntityRegistry.TEST_DUMMY;
@EventBusSubscriber
public class AffixTestCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("test_affix_compatibility")
                .requires(s -> s.hasPermission(2)) // 需要OP权限
                .executes(AffixTestCommand::runTest));
    }

    private static int runTest(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        ctx.getSource().sendSuccess(() -> Component.literal("正在初始化队列测试器..."), true);

        // 启动分时测试
        AffixCompatibilityTester.start(player);

        return 1;
    }
}