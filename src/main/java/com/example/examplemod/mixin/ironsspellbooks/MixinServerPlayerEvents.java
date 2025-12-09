package com.example.examplemod.mixin.ironsspellbooks;

import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEvents.class)
public class MixinServerPlayerEvents {

    // =========================================================================
    // 1. 修正施法打断逻辑 (RPG 包必备)
    // 原版逻辑：只要不是 DoT 伤害，任何伤害都会打断施法。
    // 修改目标：只有单次伤害超过玩家最大生命值的 X% (例如 5%) 时才打断。
    // =========================================================================

    /**
     * 拦截 onLivingIncomingDamage 中的 serverSideCancelCast 调用。
     * 我们在这里判断伤害量是否足以打断施法。
     */
    @Redirect(
            method = "onLivingIncomingDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;serverSideCancelCast(Lnet/minecraft/server/level/ServerPlayer;)V"
            ),
            remap = false
    )
    private static void conditionalCancelCast(ServerPlayer player, LivingIncomingDamageEvent event) {
        // --- RPG 数值配置 ---
        // 设定打断阈值：例如 2% (0.02)。如果玩家 1000 血，只有受到的伤害 > 20 才会被打断。
        float interruptThreshold = 0.02F; 

        float damageAmount = event.getAmount(); // 获取本次伤害值
        float maxHealth = player.getMaxHealth();

        // 只有当伤害足以造成威胁时，才执行原版的打断逻辑
        if (damageAmount > maxHealth * interruptThreshold) {
            io.redspace.ironsspellbooks.api.util.Utils.serverSideCancelCast(player);
        }
        // 否则忽略打断，玩家可以顶着小怪伤害继续读条
    }

}