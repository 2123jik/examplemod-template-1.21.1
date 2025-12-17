package com.example.examplemod;

import com.example.examplemod.affix.*;
import com.example.examplemod.category.LootCategories;
import com.example.examplemod.category.SlotGroups;
import com.example.examplemod.client.config.CustomScaleConfig;
import com.example.examplemod.client.gui.InventoryModelRenderer;

import com.example.examplemod.client.particle.ModParticles;
import com.example.examplemod.component.ModDataComponents;
import com.example.examplemod.datagen.SpellTriggerAffixJsonGenerator;
import com.example.examplemod.register.*;
import com.example.examplemod.recipe.ModRecipes;
import com.example.examplemod.server.damage.DamageIndicatorListener;
import com.example.examplemod.server.loot.ModLootModifiers;
import com.example.examplemod.spells.SpellRegistries;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.xkmc.l2damagetracker.contents.attack.AttackEventHandler;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import fuzs.puzzleslib.api.network.v3.NetworkHandler;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
//import net.minecraft.client.Minecraft;
//
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.fml.loading.FMLEnvironment;
//import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    public static final String MODID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        ModLootModifiers.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModEntities.register(modEventBus);
        SlotGroups.register(modEventBus);
        ModAttachments.register(modEventBus);
        LootCategories.register(modEventBus);
        AffixEventHandler.register();
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModItems.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);
        SpellRegistries.register(modEventBus);
        ModDataComponents.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.CLIENT, InventoryModelRenderer.ClientConfig.SPEC, ExampleMod.MODID + "_1");
        modContainer.registerConfig(ModConfig.Type.CLIENT, InventoryModelRenderer.LayerConfig.SPEC, ExampleMod.MODID + "_2");
        modContainer.registerConfig(ModConfig.Type.CLIENT, CustomScaleConfig.SPEC);
        ModConstructor.construct(MODID, () -> new ModConstructor() {
            @Override
            public void onConstructMod() {
            }
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        AffixRegistry.INSTANCE.registerCodec(loc("spell_effect"), SpellEffectAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("magic_telepathic"), MagicTelepathicAffix.CODEC);
        AttackEventHandler.register(1,new DamageIndicatorListener());
        AffixRegistry.INSTANCE.registerCodec(loc("spell_level"), SpellLevelAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_trigger"), SpellTriggerAffix.CODEC);

    }
// 假设 LOGGER 已经定义


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
//        AffixJsonGenerator.generate();
//        LOGGER.info("HELLO from server starting - 开始导出数据任务");
//
//        // 获取服务器实例（对于获取 LootData 至关重要）
//        MinecraftServer server = event.getServer();
//
//        // 1. 导出战利品表 (需要 server 实例来解析 Codec 和获取数据)
//        EntityLootTableExporter lootExporter = new EntityLootTableExporter();
//        // 这一步是耗时操作，建议仅在开发环境运行
//        lootExporter.exportAllEntityLootTables(server);
//
//        // 2. 导出属性
//        EntityAttributeExporter attributeExporter = new EntityAttributeExporter();
//        attributeExporter.exportAllEntityAttributes();
//
//        LOGGER.info("数据导出任务结束");
    }
//    private static final AtomicBoolean IS_EXPORTING = new AtomicBoolean(false);
//    //    @SubscribeEvent
//    public  void onTagsUpdated(TagsUpdatedEvent event) {
//        // 仅在客户端进入世界且数据包同步完成后运行
//
//        if (Minecraft.getInstance().level == null || FMLEnvironment.dist != Dist.CLIENT) return;
//        if (IS_EXPORTING.get()) return;
//
//        new Thread(() -> {
//            if (!IS_EXPORTING.compareAndSet(false, true)) return;
//            try {
//                // 稍微延迟，确保其他 Mod 初始化完毕
//                Thread.sleep(3000);
//                LOGGER.info(">>> [Graph] 开始全息图谱导出任务...");
//                dumpData();
//            } catch (Exception e) {
//                LOGGER.error(">>> [Graph] 导出严重错误", e);
//            } finally {
//                IS_EXPORTING.set(false);
//            }
//        }, "MC-Graph-Exporter").start();
//    }

    public static ResourceLocation loc(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }
    @SubscribeEvent
    public void onServerStarted(ServerStartingEvent event)
    {
//        SpellTriggerAffixJsonGenerator.generate();
//        System.out.println("zwfy");
//        Collection<AbstractSpell> spells = SpellRegistry.getEnabledSpells();
//        for (AbstractSpell spell : spells)
//        {
//            ResourceLocation spellId = spell.getSpellResource();
//            String guideKey = String.format("spell.%s.%s.guide", spellId.getNamespace(), spellId.getPath());
//            Component guideComponent = Component.translatable(guideKey);
//
//            System.out.println(spellId+Language.getInstance().getOrDefault(guideKey));
//        }

    }

    /**
     * 遍历服务器加载的所有成就，并提取关键信息。
     * @param server Minecraft 服务器实例
     * @return 包含所有成就信息的列表
     */
    public static List<AdvancementInfo> getAllAdvancementsInfo(MinecraftServer server) {
        ServerAdvancementManager manager = server.getAdvancements();

        // 2. 使用 manager 提供的 getAllAdvancements() 方法
        Collection<AdvancementHolder> allHolders = manager.getAllAdvancements();

        List<AdvancementInfo> advancementList = new ArrayList<>();


        return advancementList;
    }

    /**
     * 简单的记录类，用于存储提取的成就信息。
     */
    private record AdvancementInfo(String id, String title, String description) {}
}