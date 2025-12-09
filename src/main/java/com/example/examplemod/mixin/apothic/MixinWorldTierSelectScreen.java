package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.client.WorldTierSelectScreen;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WorldTierSelectScreen.class)
public class MixinWorldTierSelectScreen {
    @Inject(method = "tierLocked", at = @At("TAIL"), cancellable = true)
    private static void addLevelRequirements(WorldTier tier, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> list = cir.getReturnValue();

        // 添加等级要求
        list.add(Component.literal(""));

        int hostilityReq = switch (tier) {
            case HAVEN -> 0;
            case FRONTIER -> 80;
            case ASCENT -> 180;
            case SUMMIT -> 280;
            case PINNACLE -> 380;
        };

        int expReq = switch (tier) {
            case HAVEN -> 0;
            case FRONTIER -> 100;
            case ASCENT -> 200;
            case SUMMIT -> 300;
            case PINNACLE -> 400;
        };

        list.add(Component.literal("需要恶意等级: " + hostilityReq + ", 玩家等级: " + expReq)
                .withStyle(ChatFormatting.YELLOW));


        cir.setReturnValue(list);
    }
}
