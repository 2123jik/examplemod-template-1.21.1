package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.loot.LootController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Random;

@Mixin(LootController.class)
public class MixinLootController {
    @Redirect(
            method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/loot/LootCategory;Ldev/shadowsoffire/apotheosis/loot/LootRarity;Ldev/shadowsoffire/apotheosis/tiers/GenContext;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Collections;shuffle(Ljava/util/List;Ljava/util/Random;)V"
            )
    )
    private static void yourModId_preventShuffle(List<?> list, Random rnd) {
    }
}
