package com.example.examplemod;

import com.example.examplemod.affix.*;
import com.example.examplemod.category.LootCategories;
import com.example.examplemod.category.SlotGroups;
import com.example.examplemod.client.gui.InventoryModelRenderer;

import com.example.examplemod.client.particle.ModParticles;
import com.example.examplemod.component.ModDataComponents;
import com.example.examplemod.init.ModEffects;
import com.example.examplemod.network.ServerboundOpenBlockInHandMessage;
import com.example.examplemod.recipe.ModRecipes;
import com.example.examplemod.register.ModEntities;
import com.example.examplemod.register.ModTraits;
import com.example.examplemod.spells.SpellRegistries;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import fuzs.puzzleslib.api.network.v3.NetworkHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import static dev.shadowsoffire.apotheosis.Apoth.BuiltInRegs.LOOT_CATEGORY;

// 定义 Mod 的主入口类，MODID 需要与 mods.toml 文件中的 ID 一致
@Mod(ExampleMod.MODID)
public class ExampleMod {

    // Mod 的唯一标识符
    public static final String MODID = "examplemod";
    // 日志记录器，用于在控制台输出调试信息
    public static final Logger LOGGER = LogUtils.getLogger();

    // 使用 PuzzlesLib 创建网络处理器，用于处理客户端和服务端的数据包通信
    public static final NetworkHandler NETWORK_HANDLER = NetworkHandler.builder(MODID)
            // 注册一个服务端接收的数据包：ServerboundOpenBlockInHandMessage
            .registerSerializer(ServerboundOpenBlockInHandMessage.class, ServerboundOpenBlockInHandMessage.STREAM_CODEC)
            .registerServerbound(ServerboundOpenBlockInHandMessage.class);

    /**
     * Mod 主构造函数
     * @param modEventBus Mod总线，用于注册生命周期事件和注册表项
     * @param modContainer Mod容器，包含元数据和配置注册方法
     */
    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        // 1. 注册生命周期事件监听器
        // 注册通用设置阶段（CommonSetup）的监听器
        modEventBus.addListener(this::commonSetup);
//        ModTraits.register();
//        ModTraits.REGISTRATE.registerEventListeners(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        // 2. 注册延迟注册表 (DeferredRegister)
        // 注册实体
        ModEntities.register(modEventBus);
        // 注册插槽组（可能与 Apotheosis 或 Curios 兼容相关）
        SlotGroups.register(modEventBus);
        // 注册战利品类别
        LootCategories.register(modEventBus);

        // 注册词缀事件处理器（静态注册，可能用于监听伤害、掉落等事件）
        AffixEventHandler.register();

        // 注册药水效果/Mob Effects
        ModEffects.MOB_EFFECTS.register(modEventBus);

        // 3. 将当前类实例注册到 NeoForge 游戏总线
        // 这样可以使用 @SubscribeEvent 注解监听游戏运行时的事件（如 onServerStarting）
        NeoForge.EVENT_BUS.register(this);

        // 4. 继续注册其他内容
        // 注册配方序列化器
        ModRecipes.register(modEventBus);
        // 注册法术（可能与 Iron's Spells 相关）
        SpellRegistries.register(modEventBus);
        // 注册数据组件（Minecraft 1.20.5+ 的 Data Components）
        ModDataComponents.register(modEventBus);

        // 5. 注册 Mod 配置文件
        // 注册客户端配置，用于渲染物品栏模型
        modContainer.registerConfig(ModConfig.Type.CLIENT, InventoryModelRenderer.ClientConfig.SPEC, ExampleMod.MODID + "_1");
        modContainer.registerConfig(ModConfig.Type.CLIENT, InventoryModelRenderer.LayerConfig.SPEC, ExampleMod.MODID + "_2");

        // 6. 使用 PuzzlesLib 进行 Mod 构建初始化
        ModConstructor.construct(MODID, () -> new ModConstructor() {
            @Override
            public void onConstructMod() {
                // 这里可以放置 PuzzlesLib 特定的初始化逻辑
            }
        });
    }

    /**
     * 通用设置阶段 (FMLCommonSetupEvent)
     * 用于执行不需要区分客户端/服务端的初始化任务，例如注册网络包、世界生成特性等。
     * 在这里主要用于向 Apotheosis (神化 Mod) 注册自定义词缀 (Affix)。
     */
    private void commonSetup(FMLCommonSetupEvent event) {

        // 注册自定义词缀的编解码器 (Codec) 到 Apotheosis 的注册表中
        // 属性词缀
        AffixRegistry.INSTANCE.registerCodec(modrl("attribute"), SchoolAttributeAffix.CODEC);
        // 法术效果词缀
        AffixRegistry.INSTANCE.registerCodec(modrl("spell_effect"), SpellEffectAffix.CODEC);
        // 心灵感应/魔法感应词缀
        AffixRegistry.INSTANCE.registerCodec(modrl("magic_telepathic"), MagicTelepathicAffix.CODEC);
        // 法术等级词缀
        AffixRegistry.INSTANCE.registerCodec(modrl("spell_level"), SpellLevelAffix.CODEC);
        // 法术触发词缀
        AffixRegistry.INSTANCE.registerCodec(modrl("spell_trigger"), SpellTriggerAffix.CODEC);
        // 法力消耗词缀
        AffixRegistry.INSTANCE.registerCodec(modrl("mana_cost"), ManaCostAffix.CODEC);
        System.out.println("=== 属性导出开始 ===");
        System.out.println("ID, 描述/翻译键");
       LOOT_CATEGORY.entrySet().forEach(entry -> {
            String id = entry.getKey().location().toString();
            System.out.println(id);
        });
        System.out.println("=== 属性导出结束 ===");

    }

    /**
     * 服务器启动事件
     * 当单人游戏或专用服务器启动时触发。
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    /**
     * 辅助方法：生成当前 Mod 命名空间下的 ResourceLocation
     * @param id 资源路径 ID
     * @return ResourceLocation 对象 (examplemod:id)
     */
    public static ResourceLocation modrl(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }

}