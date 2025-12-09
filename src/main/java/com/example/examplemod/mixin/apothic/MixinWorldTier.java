package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import dev.xkmc.l2core.capability.player.PlayerCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WorldTier.class)
public class MixinWorldTier {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static boolean isUnlocked(Player player, WorldTier tier) {
        // 原有的进度检查
        boolean hasAdvancement = ApothMiscUtil.hasAdvancement(player, tier.getUnlockAdvancement());

        if (!hasAdvancement) return false;
        int level= ((PlayerDifficulty)((PlayerCapabilityHolder) LHMiscs.PLAYER.type()).getOrCreate(player)).getLevel(player).getLevel();
            // 添加玩家等级要求
        return switch (tier) {
            case HAVEN -> level >= 0 && player.experienceLevel >= 0;
            case FRONTIER -> level >= 80 && player.experienceLevel >= 100;
            case ASCENT -> level >= 180 && player.experienceLevel >= 200;
            case SUMMIT -> level >= 280 && player.experienceLevel >= 300;
            case PINNACLE -> level >= 380 && player.experienceLevel >= 400;
            default -> true;
        };

    }
}
