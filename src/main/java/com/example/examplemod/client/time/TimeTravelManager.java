package com.example.examplemod.client.time;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(value = Dist.CLIENT)
public class TimeTravelManager {

    private static final Map<UUID, LinkedList<PlayerStateSnapshot>> HISTORY = new HashMap<>();
    public static boolean isRenderingEcho = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null) return;

        LinkedList<PlayerStateSnapshot> list = HISTORY.computeIfAbsent(mc.player.getUUID(), k -> new LinkedList<>());
        list.add(new PlayerStateSnapshot(mc.player));

        // 保留稍多于0.8秒的数据 (20tps * 1.5s = 30帧足够)
        if (list.size() > 40) list.removeFirst();
    }

    public static PlayerStateSnapshot getSnapshot(UUID uuid, float secondsAgo) {
        LinkedList<PlayerStateSnapshot> list = HISTORY.get(uuid);
        if (list == null || list.isEmpty()) return null;

        long targetTime = System.currentTimeMillis() - (long)(secondsAgo * 1000);
        // 倒序查找最近的一帧
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).timestamp <= targetTime) return list.get(i);
        }
        return list.getFirst(); // 如果没找到更早的，就返回最早的一帧
    }
}

// 必须更新 PlayerStateSnapshot 以包含 ISS 数据
class PlayerStateSnapshot {
    public final long timestamp;
    public final double x, y, z;
    public final float yRot, xRot, yHeadRot, yBodyRot;
    public final float limbSwing, limbSwingAmount, attackAnim;
    public final Pose pose;
    public final boolean isSneaking, isSprinting, isElytraFlying, isSwimming;
    public final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    
    // ISS 数据
    public final boolean isCasting;
    public final String castSpellId;
    public final int castLevel;

    public PlayerStateSnapshot(AbstractClientPlayer player) {
        this.timestamp = System.currentTimeMillis();
        this.x = player.getX(); this.y = player.getY(); this.z = player.getZ();
        this.yRot = player.getYRot(); this.xRot = player.getXRot();
        this.yHeadRot = player.yHeadRot; this.yBodyRot = player.yBodyRot;
        this.limbSwing = player.walkAnimation.position();
        this.limbSwingAmount = player.walkAnimation.speed();
        this.attackAnim = player.getAttackAnim(1.0f);
        this.pose = player.getPose();
        this.isSneaking = player.isShiftKeyDown();
        this.isSprinting = player.isSprinting();
        this.isElytraFlying = player.isFallFlying();
        this.isSwimming = player.isSwimming();
        for (EquipmentSlot slot : EquipmentSlot.values()) equipment.put(slot, player.getItemBySlot(slot).copy());

        // 捕获 ISS
        var data = ClientMagicData.getSyncedSpellData(player);
        this.isCasting = data.isCasting();
        this.castSpellId = data.getCastingSpellId();
        this.castLevel = data.getCastingSpellLevel();
    }
}