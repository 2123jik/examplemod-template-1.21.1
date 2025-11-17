package com.example.examplemod.capability;

import com.example.examplemod.ExampleMod; // 导入你的主类以获取 MOD_ID
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {

    // 1. 定义你的能力
    //    - createVoid 表示这个能力没有额外的上下文（比如方向）
    //    - 第一个参数是能力的唯一ID，必须是 "modid:capability_name" 的格式
    //    - 第二个参数是能力的接口
    public static final EntityCapability<IEatenFoods, Void> EATEN_FOODS_CAPABILITY =
            EntityCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "eaten_foods"),
                    IEatenFoods.class
            );

    // 2. 提供一个静态方法来处理注册逻辑
    public static void register(RegisterCapabilitiesEvent event) {
        // 将我们的能力附加到玩家（PLAYER）这个实体类型上。
        // 每当一个新的玩家实体被创建，NeoForge 就会为他 new 一个 EatenFoods() 实例。
        event.registerEntity(
                EATEN_FOODS_CAPABILITY,
                EntityType.PLAYER,
                (player, context) -> new EatenFoods()
        );
    }
}