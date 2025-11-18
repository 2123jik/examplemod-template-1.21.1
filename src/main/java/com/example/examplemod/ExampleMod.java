package com.example.examplemod;

import com.example.examplemod.component.ModDataComponents;
import com.example.examplemod.init.ModEffects;
import com.example.examplemod.network.ServerboundOpenBlockInHandMessage;
import com.example.examplemod.recipe.ModRecipes;
import com.example.examplemod.register.ModEntities;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import fuzs.puzzleslib.api.network.v3.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ExampleMod.MODID)
public class ExampleMod{

    public static final String MODID = "examplemod";
    public static final ResourceLocation RUNNING_SHOES_SPEED_MODIFIER_UUID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,"YOUR-UNIQUE-UUID-HERE");
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final NetworkHandler NETWORK_HANDLER = NetworkHandler.builder(MODID)
            .registerSerializer(ServerboundOpenBlockInHandMessage.class, ServerboundOpenBlockInHandMessage.STREAM_CODEC)
            .registerServerbound(ServerboundOpenBlockInHandMessage.class);


    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        ModEntities.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        ModRecipes.register(modEventBus);
        ModDataComponents.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModConstructor.construct(MODID, () -> new ModConstructor() {
            @Override
            public void onConstructMod() {}
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");


        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

}
