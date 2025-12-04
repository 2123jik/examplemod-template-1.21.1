package com.example.examplemod.mixin.cataclysm;

import com.github.L_Ender.cataclysm.blockentities.AltarOfFire_Block_Entity;
import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AltarOfFire_Block_Entity.class)
public class AltarOfFireMixin {

    // 我们拦截 ignis.setPos(...) 这个方法的调用
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/L_Ender/cataclysm/entity/AnimationMonster/BossMonsters/Ignis_Entity;setPos(DDD)V"
        )
    )
    public void redirectIgnisSpawnPos(Ignis_Entity ignis, double originalX, double originalY, double originalZ, Level level, BlockPos pos, BlockState state) {
        // 在这里插入上面的“核心计算逻辑”
//        Player targetPlayer = level.getNearestPlayer((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 50.0D, false);
//
//        if (targetPlayer != null) {
//            double distance = 32.0D;
//            double pitchRad = Math.toRadians(45.0D);
//            double hDist = distance * Math.cos(pitchRad);
//            double vDist = distance * Math.sin(pitchRad);
//
//            float yRot = targetPlayer.getYRot();
//            double yRotRad = Math.toRadians(yRot);
//
//            double offsetX = -Math.sin(yRotRad) * hDist;
//            double offsetZ = Math.cos(yRotRad) * hDist;
//
//            ignis.setPos(targetPlayer.getX() + offsetX, targetPlayer.getEyeY() + vDist, targetPlayer.getZ() + offsetZ);
//            ignis.setYBodyRot(yRot+180f);
//            ignis.setYHeadRot(yRot+180f);
//        } else {
//            // 使用原本传入的参数（原版逻辑）
//            ignis.setPos(originalX, originalY, originalZ);
//        }
    }
}