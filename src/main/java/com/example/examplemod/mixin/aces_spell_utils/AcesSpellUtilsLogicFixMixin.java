package com.example.examplemod.mixin.aces_spell_utils;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.acetheeldritchking.aces_spell_utils.events.AcesSpellUtilsServerEvents;
import net.acetheeldritchking.aces_spell_utils.registries.ASAttributeRegistry;
import net.acetheeldritchking.aces_spell_utils.utils.ASTags;
import net.acetheeldritchking.aces_spell_utils.utils.AcesSpellUtilsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AcesSpellUtilsServerEvents.class)
public class AcesSpellUtilsLogicFixMixin {

    /**
     * 修复法力窃取 (Mana Steal) 的目标扣蓝逻辑。
     * 原版逻辑错误地使用了攻击者的当前蓝量作为减数。
     */
    @Inject(method = "manaStealEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void fixManaStealLogic(LivingDamageEvent.Post event, CallbackInfo ci) {
        // 取消原版逻辑，完全接管
        ci.cancel();

        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        Entity directEntity = event.getSource().getDirectEntity();

        if (sourceEntity instanceof LivingEntity livingEntity) {
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                if (directEntity != null) {
                    if (directEntity.getType().is(ASTags.MANA_STEAL_WHITELIST) || directEntity.is(serverPlayer)) {
                        AttributeInstance hasManaSteal = serverPlayer.getAttribute(ASAttributeRegistry.MANA_STEAL);
                        if (hasManaSteal != null) {
                            float manaStealAttr = (float) serverPlayer.getAttributeValue(ASAttributeRegistry.MANA_STEAL);
                            int maxAttackerMana = (int) serverPlayer.getAttributeValue(AttributeRegistry.MAX_MANA);
                            MagicData attackerPlayerMagicData = MagicData.getPlayerMagicData(serverPlayer);

                            if (manaStealAttr > 0.0F) {
                                // 攻击者回蓝逻辑 (保持原版设计: 伤害 * 系数)
                                int addMana = (int) Math.min(manaStealAttr * event.getOriginalDamage() + attackerPlayerMagicData.getMana(), (float) maxAttackerMana);
                                attackerPlayerMagicData.setMana((float) addMana);
                                PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(attackerPlayerMagicData));

                                // 目标扣蓝逻辑 (修复部分)
                                if (AcesSpellUtilsConfig.manaStealDrain && target instanceof ServerPlayer) {
                                    ServerPlayer serverTargetPlayer = (ServerPlayer) target;
                                    MagicData targetPlayerMagicData = MagicData.getPlayerMagicData(serverTargetPlayer);
                                    
                                    // 修复: 扣除量 = 属性 * 伤害。从目标当前蓝量中减去。
                                    float manaToDrain = manaStealAttr * event.getOriginalDamage();
                                    float currentTargetMana = targetPlayerMagicData.getMana();
                                    
                                    // 确保不扣成负数
                                    float newTargetMana = Math.max(currentTargetMana - manaToDrain, 0);

                                    targetPlayerMagicData.setMana(newTargetMana);
                                    PacketDistributor.sendToPlayer(serverTargetPlayer, new SyncManaPacket(targetPlayerMagicData));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 修复饥饿窃取 (Hunger Steal) 的目标扣饱食度逻辑。
     * 原版逻辑 (Attr - Current) 会导致产生巨大的负数，直接清空饱食度。
     */
    @Inject(method = "hungerStealEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void fixHungerStealLogic(LivingDamageEvent.Pre event, CallbackInfo ci) {
        ci.cancel();

        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        if (sourceEntity instanceof LivingEntity livingEntity) {
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                AttributeInstance hasHungerSteal = serverPlayer.getAttribute(ASAttributeRegistry.HUNGER_STEAL);
                if (hasHungerSteal != null) {
                    double hungerStealAttr = serverPlayer.getAttributeValue(ASAttributeRegistry.HUNGER_STEAL);
                    if (hungerStealAttr > 0.0F) {
                        FoodData playerFood = serverPlayer.getFoodData();
                        int foodLevel = playerFood.getFoodLevel();
                        
                        // 攻击者回复 (保持原版: 属性值 + 当前值，也就是直接加算)
                        int addFood = (int) Math.min(hungerStealAttr + (double) foodLevel, 20.0D); // 加个上限20防止溢出
                        playerFood.setFoodLevel(addFood);

                        if (target instanceof Player) {
                            Player targetPlayer = (Player) target;
                            FoodData targetFood = targetPlayer.getFoodData();
                            int targetFoodLevel = targetFood.getFoodLevel();
                            
                            // 修复: 目标当前 - 属性值，且最小为0
                            int subFood = (int) Math.max((double) targetFoodLevel - hungerStealAttr, 0.0D);
                            targetFood.setFoodLevel(subFood);
                        }
                    }
                }
            }
        }
    }

    /**
     * 修复法术抗性穿透 (Spell Res Penetration) 的计算逻辑。
     * 原版逻辑会将目标的抗性(Resistance)加到你的增伤倍率里，导致对方越肉受到的伤害越高。
     */
    @Inject(method = "spellResPenetrationEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void fixSpellResPenLogic(LivingIncomingDamageEvent event, CallbackInfo ci) {
        ci.cancel();

        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        if (attacker instanceof LivingEntity livingEntity) {
            AttributeInstance hasSpellResPen = livingEntity.getAttribute(ASAttributeRegistry.SPELL_RES_PENETRATION);
            if (hasSpellResPen != null) {
                double spellResPenAttr = livingEntity.getAttributeValue(ASAttributeRegistry.SPELL_RES_PENETRATION);
                
                // 我们不再获取 victim 的 spellResAttr 并加到伤害里，这很不合理。
                // 如果你想保留 "穿透" 的概念，最简单的做法是把它当作独立增伤。
                // 或者如果你想做真正的减抗，计算会很复杂，这里改为单纯的根据穿透属性增伤。

                if (spellResPenAttr > 0.0F) {
                    if (event.getSource() instanceof SpellDamageSource) {
                        float baseDamage = event.getOriginalAmount();
                        
                        // 修复: 额外伤害只取决于你的穿透属性
                        float bonusDamage = (float) ((double) baseDamage * spellResPenAttr);
                        float totalDamage = baseDamage + bonusDamage;
                        
                        event.setAmount(totalDamage);
                        
                        // 移除调试日志以减少垃圾信息，或者保留
                        // AcesSpellUtils.LOGGER.debug("Fixed Pen Logic - Total Damage: " + event.getAmount());
                    }
                }
            }
        }
    }
}