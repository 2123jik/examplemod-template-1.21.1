package com.example.examplemod.register;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GoldenGateEntity;
import com.example.examplemod.entity.SwordProjectileEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 实体注册类 (ModEntities)
 * <p>
 * 负责统一管理和注册 Mod 中的所有实体类型（EntityType）。
 * 使用 NeoForge 的 DeferredRegister 机制进行延迟注册。
 */
public class ModEntities {

    // 创建一个 DeferredRegister 对象，绑定到原版实体类型注册表 (ENTITY_TYPE)
    // 参数 1: 注册表类型 (这里是实体类型)
    // 参数 2: Mod ID (命名空间)
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ExampleMod.MODID);

    /**
     * 注册 GoldenGateEntity 实体
     * <p>
     * 这是一个基础的实体注册示例。
     */
    public static final Supplier<EntityType<GoldenGateEntity>> GOLDENGATEENTITY =
            ENTITY_TYPES.register("goldengateentity", () -> EntityType.Builder.of(GoldenGateEntity::new, MobCategory.MISC)
                    .sized(0.5f, 1.15f) // 设置碰撞箱大小：宽 0.5 米，高 1.15 米
                    .build("goldengateentity")); // 构建实体类型，ID 需要与注册名一致

    /**
     * 注册 SwordProjectileEntity 实体（例如：剑气、飞剑等投射物）
     * <p>
     * 包含了更详细的网络同步设置。
     */
    public static final Supplier<EntityType<SwordProjectileEntity>> SWORD_PROJECTILE = ENTITY_TYPES.register("sword_projectile",
            () -> EntityType.Builder.<SwordProjectileEntity>of(SwordProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)          // 碰撞箱大小：宽0.5，高0.5
                    .clientTrackingRange(8)     // 客户端追踪距离：8个区块（128米）。超过这个距离客户端将不会渲染或同步该实体。
                    .updateInterval(20)         // 更新间隔：每 20 tick (1秒) 同步一次位置/数据到客户端（投射物通常设得比较低，如 1-10，这里 20 可能略高，视需求而定）
                    .build("sword_projectile"));

    /**
     * 将实体注册表注册到事件总线
     * <p>
     * 必须在主类 (ExampleMod) 的构造函数中调用此方法，
     * 否则实体将不会被加载到游戏中。
     *
     * @param eventBus Mod 的主事件总线
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}