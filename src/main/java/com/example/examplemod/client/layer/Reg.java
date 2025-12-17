package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import static com.example.examplemod.ExampleMod.loc;
import static com.example.examplemod.client.layer.CustomExperience.renderCustomExperience;
import static com.example.examplemod.client.layer.RenderCustomEffect.renderCustomEffect;
import static com.example.examplemod.client.layer.RenderCustomHealth.renderCustomHealth;
import static com.example.examplemod.client.layer.RenderSelectedItemName.renderSelectedItemName;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class Reg {
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "custom_health_bar"),
                (graphics, delta) -> renderCustomHealth(graphics));
        event.registerAbove(VanillaGuiLayers.EFFECTS,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "custom_effect_hud"),
                (graphics, delta) -> renderCustomEffect(graphics));
        event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "custom_selected_item_name"),
                (graphics, delta) -> renderSelectedItemName(graphics,delta));
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR,
                loc("custom_experience"),((guiGraphics, deltaTracker) -> renderCustomExperience(guiGraphics)));
    }
    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.PLAYER_HEALTH.equals(event.getName())||VanillaGuiLayers.ARMOR_LEVEL.equals(event.getName())||VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())||VanillaGuiLayers.EXPERIENCE_BAR.equals(event.getName())||VanillaGuiLayers.EXPERIENCE_LEVEL.equals(event.getName())||VanillaGuiLayers.EFFECTS.equals(event.getName())||VanillaGuiLayers.SELECTED_ITEM_NAME.equals(event.getName())) {
            event.setCanceled(true);
        }
    }
}
