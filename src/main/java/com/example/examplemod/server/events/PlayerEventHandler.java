package com.example.examplemod.server.events;

import com.example.examplemod.register.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import static com.example.examplemod.ExampleMod.MODID;

@EventBusSubscriber(modid = MODID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        CompoundTag forgeData = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);

        if (!forgeData.getBoolean("hasLoggedInBefore")) {

            ItemStack starterKit = new ItemStack(ModItems.STARTER_KIT.get());

            // 1. 发放物品
            if (!player.getInventory().add(6,starterKit)) {
                player.drop(starterKit, false);
            }

            // 2. 【新增】添加冷却时间 (单位：Tick，20 ticks = 1秒)
            // 这里设置 600 ticks = 30秒。
            // 玩家刚上线时，物品会显示冷却遮罩，无法使用，给服务器加载区块缓冲时间。
            player.getCooldowns().addCooldown(ModItems.STARTER_KIT.get(), 600);

            // 3. 标记已领取
            forgeData.putBoolean("hasLoggedInBefore", true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, forgeData);
        }
    }
}