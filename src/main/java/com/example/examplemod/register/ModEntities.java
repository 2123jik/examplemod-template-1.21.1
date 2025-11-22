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

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ExampleMod.MODID);

    public static final Supplier<EntityType<GoldenGateEntity>> GOLDENGATEENTITY=
            ENTITY_TYPES.register("goldengateentity",()->EntityType.Builder.of(GoldenGateEntity::new, MobCategory.MISC)
                    .sized(0.5f, 1.15f).build("goldengateentity"));
    public static final Supplier<EntityType<SwordProjectileEntity>> SWORD_PROJECTILE = ENTITY_TYPES.register("sword_projectile",
            () -> EntityType.Builder.<SwordProjectileEntity>of(SwordProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F) // 碰撞箱大小：宽0.5，高0.5
                    .clientTrackingRange(4) // 追踪距离（区块）
                    .updateInterval(20)     // 更新频率
                    .build("sword_projectile"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}