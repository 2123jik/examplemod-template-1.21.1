package com.example.examplemod;

import com.example.examplemod.affix.*;
import com.example.examplemod.category.LootCategories;
import com.example.examplemod.category.SlotGroups;
import com.example.examplemod.client.gui.InventoryModelRenderer;

import com.example.examplemod.client.particle.ModParticles;
import com.example.examplemod.component.ModDataComponents;
import com.example.examplemod.register.ModEffects;
import com.example.examplemod.network.ServerboundOpenBlockInHandMessage;
import com.example.examplemod.recipe.ModRecipes;
import com.example.examplemod.register.ModEntities;
import com.example.examplemod.register.ModItems;
import com.example.examplemod.register.ModMenus;
import com.example.examplemod.server.damage.DamageIndicatorListener;
import com.example.examplemod.server.loot.ModLootModifiers;
import com.example.examplemod.spells.SpellRegistries;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.xkmc.l2damagetracker.contents.attack.AttackEventHandler;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import fuzs.puzzleslib.api.network.v3.NetworkHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
//import net.minecraft.client.Minecraft;
//
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.fml.loading.FMLEnvironment;
//import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
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

import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.examplemod.server.data.ApotheosisDataDumper.dumpData;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    public static final String MODID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final NetworkHandler NETWORK_HANDLER = NetworkHandler.builder(MODID)
            .registerSerializer(ServerboundOpenBlockInHandMessage.class, ServerboundOpenBlockInHandMessage.STREAM_CODEC)
            .registerServerbound(ServerboundOpenBlockInHandMessage.class);

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        ModLootModifiers.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModEntities.register(modEventBus);
        SlotGroups.register(modEventBus);
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

        ModConstructor.construct(MODID, () -> new ModConstructor() {
            @Override
            public void onConstructMod() {
            }
        });
    }
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册上面的监听器
            AttackEventHandler.register(10000, new DamageIndicatorListener());
        });
        AffixRegistry.INSTANCE.registerCodec(loc("attribute"), SchoolAttributeAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_effect"), SpellEffectAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("magic_telepathic"), MagicTelepathicAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_level"), SpellLevelAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_trigger"), SpellTriggerAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("mana_cost"), ManaCostAffix.CODEC);

    }
// 假设 LOGGER 已经定义


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {


        LOGGER.info("HELLO from server starting");
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

}