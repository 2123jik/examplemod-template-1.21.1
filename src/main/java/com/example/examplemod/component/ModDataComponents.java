package com.example.examplemod.component;

import com.example.examplemod.ExampleMod; // 假设这是主类路径
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

import static com.example.examplemod.ExampleMod.MODID;

/**
 * ModDataComponents 类
 * <p>
 * 该类用于注册自定义的 "Data Components" (数据组件)。
 * 自 Minecraft 1.20.5 起，数据组件取代了物品堆 (ItemStack) 上的 NBT 标签，用于存储自定义数据。
 */
public class ModDataComponents {

    // 创建一个 DeferredRegister，用于将 DataComponentType 注册到游戏注册表中
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    /**
     * 注册一个名为 "ms" 的组件 (可能代表 Maken Sword)。
     * 数据类型：Double (双精度浮点数)。
     * - persistent(Codec.DOUBLE): 表示该数据会被保存到磁盘 (NBT)，使用 Double 的编解码器。
     * - networkSynchronized(ByteBufCodecs.DOUBLE): 表示该数据会通过网络同步到客户端 (例如用于渲染显示)。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> MAKEN_SWORD =
            register("ms", builder -> builder
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(ByteBufCodecs.DOUBLE)
            );

    /**
     * 注册一个名为 "ma" 的组件 (可能代表 Maken Armor)。
     * 数据类型：Double。
     * 同样具备持久化存储和网络同步功能。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> MAKEN_ARMOR =
            register("ma", builder -> builder
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(ByteBufCodecs.DOUBLE)
            );

    /**
     * 注册一个名为 "wedren_boss" 的组件。
     * 数据类型：Integer (整数)。
     * 可能用于存储 Boss 的状态、ID 或计数器。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WAEDREN_BOSS =
            register("wedren_boss", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
            );

    /**
     * 注册一个名为 "spell_bonuses" 的组件。
     * 数据类型：SpellBonusData (自定义的复杂对象)。
     *
     * 这里使用了自定义对象 SpellBonusData 内部定义的 CODEC 进行序列化。
     * ByteBufCodecs.fromCodec 允许将基于 JSON 的 Codec 转换为网络缓冲区编解码器。
     */
    public static final java.util.function.Supplier<DataComponentType<SpellBonusData>> SPELL_BONUSES =
            register("spell_bonuses", builder -> builder
                    .persistent(SpellBonusData.CODEC) // 存盘
                    .networkSynchronized(ByteBufCodecs.fromCodec(SpellBonusData.CODEC)) // 网络同步
            );

    /**
     * 辅助私有方法：用于简化组件注册流程。
     *
     * @param name 组件的注册名 (Registry Name)
     * @param builderOperator 用于配置 DataComponentType 构建器的操作符 (如设置是否持久化、是否同步)
     * @param <T> 组件存储的数据类型
     * @return 注册好的组件持有者
     */
    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        // 使用 builder() 创建构建器，应用传入的配置，然后 build() 构建，最后注册到 DeferredRegister
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    /**
     * 初始化方法。
     * 在模组主类的构造函数中调用此方法，将 DeferredRegister 挂载到模组事件总线上。
     *
     * @param eventBus 模组事件总线
     */
    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}