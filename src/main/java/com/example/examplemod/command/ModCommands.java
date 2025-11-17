package com.example.examplemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.example.examplemod.server.Server.SpellSystemEvents.SPELL_CAST_COUNT_TAG;


public class ModCommands {

    // 注册我们的指令
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            // 创建一个名为 "proficiency" 的主指令
            Commands.literal("proficiency")
                // 只有玩家才能执行
                .requires(source -> source.isPlayer())
                // 定义指令的具体执行逻辑
                .executes(context -> {
                    // 调用我们的显示熟练度的方法
                    return viewProficiency(context.getSource());
                })
        );
    }

    // 显示熟练度的核心逻辑
    private static int viewProficiency(CommandSourceStack source) throws CommandSyntaxException {
        // 1. 获取执行指令的玩家
        ServerPlayer player = source.getPlayerOrException();

        // 2. 读取玩家的NBT数据（这部分你已经很熟悉了）
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag forgeData = persistentData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        CompoundTag spellCounts = forgeData.getCompound(SPELL_CAST_COUNT_TAG);

        // 3. 检查是否有数据
        if (spellCounts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e你还没有任何法术熟练度。"), false);
            return 1;
        }

        // 4. 发送标题给玩家
        source.sendSuccess(() -> Component.literal("§b--- 法术熟练度列表 ---"), false);

        // 5. 遍历所有法术并逐条发送给玩家
        for (String spellId : spellCounts.getAllKeys()) {
            int count = spellCounts.getInt(spellId);
            
            // 将 "irons_spellbooks:fireball" 格式化成 "Fireball"
            String[] parts = spellId.split(":");
            String spellName = parts.length > 1 ? parts[1].replace('_', ' ') : spellId;
            // 首字母大写
            spellName = Character.toUpperCase(spellName.charAt(0)) + spellName.substring(1);

            // 创建并发送消息
            Component message = Component.literal(String.format("§a%s: §f%d 次", spellName, count));
            source.sendSuccess(() -> message, false); // false表示这条消息不会广播给OP
        }

        return 1; // 代表指令成功执行
    }
}