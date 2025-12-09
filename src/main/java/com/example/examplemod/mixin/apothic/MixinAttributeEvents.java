package com.example.examplemod.mixin.apothic;

import com.example.examplemod.bridge.ApothCritBridge;
import dev.shadowsoffire.apothic_attributes.api.ALObjects; // 确保导入正确
import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = AttributeEvents.class, remap = false)
public class MixinAttributeEvents {

    /**
     * @author YourName
     * @reason 统计神化暴击层数并传递给 HUD
     */
    @Overwrite
    @SubscribeEvent(
            priority = EventPriority.HIGH
    )
    public void apothCriticalStrike(LivingIncomingDamageEvent e) {
        Entity source = e.getSource().getEntity();
        LivingEntity attacker = source instanceof LivingEntity le ? le : null;

        if (attacker != null && !e.getSource().is(ALObjects.Tags.CANNOT_CRITICALLY_STRIKE)) {

            double critChance = attacker.getAttributeValue(ALObjects.Attributes.CRIT_CHANCE);
            float critDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE);
            RandomSource rand = e.getEntity().getRandom();

            float originalAmount = e.getAmount();
            float damage = originalAmount;

            // --- 计数器 ---
            int layers = 0;

            // --- 核心循环 ---
            while (rand.nextFloat() <= critChance && critDmg > 1.0F) {
                critChance--;
                damage += originalAmount * (critDmg - 1.0F);
                critDmg *= 0.85F;
                layers++; // 记录层数
            }

            if (damage > originalAmount) {
                // 发送神化自带粒子
                if (!attacker.level().isClientSide) {
                    PacketDistributor.sendToPlayersTrackingChunk(
                            (ServerLevel) attacker.level(),
                            e.getEntity().chunkPosition(),
                            new CritParticlePayload(e.getEntity().getId())
                    );
                }

                // --- 存入 Bridge ---
                if (attacker instanceof ServerPlayer player) {
                    ApothCritBridge.recordLayers(player, layers);
                }
            }

            e.setAmount(damage);
        }
    }
}