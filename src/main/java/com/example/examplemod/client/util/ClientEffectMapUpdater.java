package com.example.examplemod.client.util;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.MobEffectStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientEffectMapUpdater {

    // 存储：实体UUID -> {MobEffect ResourceLocation 集合}
    private static final Map<UUID, Set<ResourceLocation>> ENTITY_RENDER_EFFECT_IDS = new ConcurrentHashMap<>();

    // 接收并处理数据包
    public static void handleMobEffectStatusPacket(MobEffectStatusPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // 在客户端主线程执行更新
        mc.execute(() -> {
            UUID entityUuid = packet.entityUuid();
            ResourceLocation effectId = packet.effectId();

            ENTITY_RENDER_EFFECT_IDS.compute(entityUuid, (uuid, effectSet) -> {
                // 如果集合不存在，创建一个新的 ConcurrentHashSet
                Set<ResourceLocation> set = (effectSet != null) ? effectSet : ConcurrentHashMap.newKeySet();

                if (packet.action() == MobEffectStatusPacket.Action.ADD) {
                    set.add(effectId);
                    // 【新增日志】
                    ExampleMod.LOGGER.info("Client | State Update: ADDED effect {} to {}. Current size: {}",
                            effectId, uuid, set.size());
                } else if (packet.action() == MobEffectStatusPacket.Action.REMOVE) {
                    set.remove(effectId);
                    // 【新增日志】
                    ExampleMod.LOGGER.info("Client | State Update: REMOVED effect {} from {}. Current size: {}",
                            effectId, uuid, set.size());
                }

                // 如果集合为空，返回 null，ConcurrentHashMap 会移除这个键
                return set.isEmpty() ? null : set;
            });
        });
    }

    // 提供给渲染逻辑：获取所有需要渲染的实体 UUID
    public static Set<UUID> getEntitiesWithEffects() {
        return ENTITY_RENDER_EFFECT_IDS.keySet();
    }

    // 提供给渲染逻辑：获取某个实体拥有的 MobEffect IDs
    public static Set<ResourceLocation> getEntityEffectIds(UUID entityUuid) {
        return ENTITY_RENDER_EFFECT_IDS.getOrDefault(entityUuid, Collections.emptySet());
    }
}