package com.example.examplemod.bridge;

import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ApothCritBridge {
    // 存储：UUID -> 层数
    private static final Map<UUID, Integer> CRIT_LAYERS = new ConcurrentHashMap<>();

    // Mixin 调用：直接存入结果
    public static void recordLayers(ServerPlayer player, int layers) {
        CRIT_LAYERS.put(player.getUUID(), layers);
    }

    // Listener 调用：获取并移除（一次性消费）
    public static int consumeCritLayers(ServerPlayer player) {
        Integer val = CRIT_LAYERS.remove(player.getUUID());
        return val == null ? 0 : val;
    }
}