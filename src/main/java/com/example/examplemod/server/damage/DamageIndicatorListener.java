package com.example.examplemod.server.damage;

import com.example.examplemod.bridge.ApothCritBridge;
import com.example.examplemod.network.DamageIndicatorPayload;
import com.example.examplemod.network.NetworkHandler; // 假设你有这个
import dev.xkmc.l2damagetracker.contents.attack.AttackListener;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2damagetracker.contents.attack.PlayerAttackCache;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

public class DamageIndicatorListener implements AttackListener {

    @Override
    public void onDamageFinalized(DamageData.DefenceMax data) {
        if (data.getAttacker() instanceof ServerPlayer player) {
            float amount = data.getDamageFinal();
            if (amount < 0.1f) return;

            // 1. 获取神化层数
            int apothLayers = ApothCritBridge.consumeCritLayers(player);
            boolean isApoth = apothLayers > 0;

            // 2. 获取 L2 / 原版判定
            boolean isL2Crit = false;
            boolean isVanillaJump = false;

            PlayerAttackCache cache = data.getPlayerData();
            if (cache != null) {
                CriticalHitEvent event = cache.getCriticalHitEvent();
                if (event != null && event.isCriticalHit()) {
                    if (event.isVanillaCritical()) {
                        isVanillaJump = true;
                    } else {
                        isL2Crit = true; // 属性暴击
                    }
                }
            }

            // 3. 定义类型
            // 0: 普通, 1: 原版跳劈, 2: L2暴击, 3: 神化暴击
            int critType = 0;

            if (isApoth) {
                critType = 3;
                // 神化暴击发生时，通常也隐含了基础暴击，但神化更重要，所以覆盖
            } else if (isL2Crit) {
                critType = 2;
            } else if (isVanillaJump) {
                critType = 1;
            }

            // 发包 (注意：只有神化类型时 layers 才有效，其他时候 layers 传 0 即可)
            NetworkHandler.sendToPlayer(player, amount, critType, apothLayers);
        }
    }
}