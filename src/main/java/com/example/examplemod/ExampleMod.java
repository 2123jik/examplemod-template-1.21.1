package com.example.examplemod;

import com.example.examplemod.affix.*;
import com.example.examplemod.category.LootCategories;
import com.example.examplemod.category.SlotGroups;
import com.example.examplemod.client.gui.InventoryModelRenderer;

import com.example.examplemod.command.IronsApothicCommands;
import com.example.examplemod.component.ModDataComponents;
import com.example.examplemod.init.ModEffects;
import com.example.examplemod.network.ServerboundOpenBlockInHandMessage;
import com.example.examplemod.recipe.ModRecipes;
import com.example.examplemod.register.ModEntities;
import com.example.examplemod.spells.SpellRegistries;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import fuzs.puzzleslib.api.network.v3.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
        // 注册命令注册事件的监听器（尽管这里用了 addListener，通常 RegisterCommandsEvent 是在 NeoForge.EVENT_BUS 上触发的，需确认）
        // *修正注*：RegisterCommandsEvent 通常是在 Forge 总线上触发，但这里代码将其挂载在了 modEventBus 上，
        // 如果是 NeoForge 新版写法可能是允许的，或者这是一个自定义的转发逻辑。
        modEventBus.addListener(this::registerCommands);

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
        // 下面这行被注释掉了，原本可能用于注册工具提示的配置
        // modContainer.registerConfig(ModConfig.Type.CLIENT, InventoryTooltipRenderer.TooltipConfig.SPEC,ExampleMod.MODID+"_2");

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
        AffixRegistry.INSTANCE.registerCodec(modResourceLoc("attribute"), SchoolAttributeAffix.CODEC);
        // 法术效果词缀
        AffixRegistry.INSTANCE.registerCodec(modResourceLoc("spell_effect"), SpellEffectAffix.CODEC);
        // 心灵感应/魔法感应词缀
        AffixRegistry.INSTANCE.registerCodec(modResourceLoc("magic_telepathic"), MagicTelepathicAffix.CODEC);
        // 法术等级词缀
        AffixRegistry.INSTANCE.registerCodec(modResourceLoc("spell_level"), SpellLevelAffix.CODEC);
        // 法术触发词缀
        AffixRegistry.INSTANCE.registerCodec(modResourceLoc("spell_trigger"), SpellTriggerAffix.CODEC);
        // 法力消耗词缀
        AffixRegistry.INSTANCE.registerCodec(modResourceLoc("mana_cost"), ManaCostAffix.CODEC);
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
    public static ResourceLocation modResourceLoc(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }

    /**
     * 命令注册事件
     * 用于注册游戏内的指令（如 /examplemod ...）。
     */
    private void registerCommands(RegisterCommandsEvent event) {
        // 注册 IronsApothic 相关的命令到命令调度器
        IronsApothicCommands.register(event.getDispatcher());
        LOGGER.info("Commands registered.");
    }
}