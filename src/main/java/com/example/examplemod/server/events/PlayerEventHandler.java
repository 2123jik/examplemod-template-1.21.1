package com.example.examplemod.server.events;

import com.example.examplemod.register.ModItems;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

import static com.example.examplemod.ExampleMod.MODID;

@EventBusSubscriber(modid = MODID)
public class PlayerEventHandler {


    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // 1. 发放新手礼包逻辑
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag forgeData = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);

            if (!forgeData.getBoolean("hasLoggedInBefore")) {
                ItemStack starterKit = new ItemStack(ModItems.STARTER_KIT.get());
                if (!player.getInventory().add(starterKit)) {
                    player.drop(starterKit, false);
                }
                // 冷却保护
                player.getCooldowns().addCooldown(ModItems.STARTER_KIT.get(), 600);

                forgeData.putBoolean("hasLoggedInBefore", true);
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, forgeData);
            }

        }
    }

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        Player entity = event.getEntity();
        if (entity.level().isClientSide() || entity instanceof FakePlayer || !(entity instanceof ServerPlayer player)) {
            return;
        }

        // 过滤掉只是为了显示进度的空成就（可选）
        if (event.getAdvancement().value().display().isEmpty()) {
            return;
        }

        player.giveExperiencePoints(10*(player.experienceLevel+1));
    }

    // 跨维度、重生时需要同步数据
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {

        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;

        if (event.getTarget() instanceof Villager villager) {
            // 建议：此处最好检查 NBT 标记，或者使用 String plain text 对比时忽略样式
            Component name = villager.getCustomName();
            // 使用 getString() 去除颜色代码进行比较，或者包含特定 Key
            if (name != null && name.getString().contains("技能升级")) { // 放宽一点匹配条件

                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);

                if (event.getEntity() instanceof ServerPlayer player) {
                }
            }
        }
    }
}