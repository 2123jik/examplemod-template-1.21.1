package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ServerboundOpenBlockInHandMessage;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Unique
    private static final Set<Item> INTERACTIVE_BLOCK_ITEMS = Set.of(
            Items.CRAFTING_TABLE,
            Items.SMITHING_TABLE,
            Items.STONECUTTER,
            Items.LOOM,
            Items.CARTOGRAPHY_TABLE
    );

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (screen.getMenu().getCarried().isEmpty()) {
                if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                    Item clickedItem = this.hoveredSlot.getItem().getItem();
                    if (INTERACTIVE_BLOCK_ITEMS.contains(clickedItem)) {
                        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(clickedItem);
                        ExampleMod.NETWORK_HANDLER.sendToServer(new ServerboundOpenBlockInHandMessage(itemId));
                    }
                }
            }
        }
    }
}