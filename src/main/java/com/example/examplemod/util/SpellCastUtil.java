package com.example.examplemod.util;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.affix.SpellLevelAffix;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.example.examplemod.server.util.ServerEventUtils.forEachItem;

public class SpellCastUtil {

    /**
     * 公共 API：计算实体身上所有装备提供的针对特定法术的等级加成
     * @param entity 实体
     * @param spell 目标法术
     * @return 额外等级总和
     */
    public static int getTotalSpellLevelBonus(LivingEntity entity, AbstractSpell spell) {
        AtomicInteger totalBonus = new AtomicInteger();

        // 1. 遍历所有装备栏 (盔甲、手持物品)
        forEachItem(entity, (stack) -> {
            totalBonus.addAndGet(getSingleItemBonus(stack, spell));
        });

        // 2. 遍历 Curios 饰品栏
        int curiosBonus = CuriosApi.getCuriosInventory(entity)
                .map(curiosHandler -> {
                    int bonus = 0;
                    for (var slotResult : curiosHandler.findCurios(stack -> !stack.isEmpty())) {
                        bonus += getSingleItemBonus(slotResult.stack(), spell);
                    }
                    return bonus;
                })
                .orElse(0);

        totalBonus.addAndGet(curiosBonus);
        return totalBonus.get();
    }

    /**
     * 计算单个物品对特定法术的加成
     */
    private static int getSingleItemBonus(ItemStack stack, AbstractSpell targetSpell) {
        int bonus = 0;
        Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(stack);
        SchoolType targetSchool = targetSpell.getSchoolType();
        String targetSpellId = targetSpell.getSpellId();

        for (AffixInstance instance : affixes.values()) {
            if (instance.isValid() && instance.affix().isBound()) {
                Affix affix = instance.getAffix();
                // 检查是否为 SpellLevelAffix
                if (affix instanceof SpellLevelAffix spellLevelAffix) {

                    // 判定条件1: 精确匹配法术ID
                    if (spellLevelAffix.isSpellBonus() && spellLevelAffix.getSpell() != null) {
                        if (spellLevelAffix.getSpell().getSpellId().equals(targetSpellId)) {
                            bonus += spellLevelAffix.getBonusLevel(instance.getRarity(), instance.level());
                        }
                    }
                    // 判定条件2: 匹配学派 (仅当没有指定特定法术时，或者逻辑上你可以决定是否叠加)
                    else if (!spellLevelAffix.isSpellBonus() && spellLevelAffix.getSchool() == targetSchool) {
                        bonus += spellLevelAffix.getBonusLevel(instance.getRarity(), instance.level());
                    }
                }
            }
        }
        return bonus;
    }

    public static boolean  castSpell(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target) {
        if (caster.level().isClientSide()) {
            return true;
        }

        try{MagicData magicData = MagicData.getPlayerMagicData(caster);
        if (magicData.isCasting()) {
            ExampleMod.LOGGER.debug("SpellTriggerAffix: Entity is still casting {}, forcing spell completion", magicData.getCastingSpellId());
            AbstractSpell oldSpell = magicData.getCastingSpell().getSpell();
            oldSpell.onCast(caster.level(), magicData.getCastingSpellLevel(), caster, magicData.getCastSource(), magicData);
            oldSpell.onServerCastComplete(caster.level(), magicData.getCastingSpellLevel(), caster, magicData, false);
            magicData.resetCastingState();
            magicData = MagicData.getPlayerMagicData(caster);
        }

        ExampleMod.LOGGER.debug("SpellTriggerAffix: Merging target data, target: {}", target.getName().getString());
        updateTargetData(caster, target, magicData, spell, x -> true);

        if (caster instanceof ServerPlayer serverPlayer) {
            ExampleMod.LOGGER.debug("Casting SPELL FOR SERVERPLAYA");
            castSpellForPlayer(spell, spellLevel, serverPlayer, magicData);
        } else if (caster instanceof IMagicEntity magicEntity) {
            magicEntity.initiateCastSpell(spell, spellLevel);
        } else if (caster instanceof LivingEntity) {
            if (spell.checkPreCastConditions(caster.level(), spellLevel, caster, magicData)) {
                spell.onCast(caster.level(), spellLevel, caster, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(caster.level(), spellLevel, caster, magicData, false);
            }
        }
        return true; // 执行成功
    }catch (Throwable t) {
            // 捕获所有异常，包括 NullPointerException, ClassCastException 甚至 StackOverflowError

            String errorMsg = t.getMessage() != null ? t.getMessage() : "Null Message";
            String exceptionName = t.getClass().getSimpleName();

            ExampleMod.LOGGER.error("Compatibility Test FAILED for spell: " + spell.getSpellId(), t);

            // 记录到文件
            CompatibilityReport.log(
                    spell.getSpellResource(),
                    (target == caster) ? "SELF" : "TARGET",
                    false,
                    errorMsg,
                    exceptionName
            );

            return false; // 执行失败
        }

    }

    private static void castSpellForPlayer(AbstractSpell spell, int spellLevel, ServerPlayer serverPlayer, MagicData magicData) {

        // We don't actually care about any of these checks (might make some of them optional affix definition fields)
        /*
        CastResult castResult = spell.canBeCastedBy(spellLevel, CastSource.COMMAND, magicData, serverPlayer);
        if (castResult.message != null) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }
        */

        // Shouldn't happen
        if (magicData.isCasting()) {
            ExampleMod.LOGGER.warn("Attempted to trigger affix-cast while player was already casting");
            return;
        }
        // No precast conditions check here either
        if (serverPlayer.isUsingItem()) {
            serverPlayer.stopUsingItem();
        }

        int effectiveCastTime = 0;
        // TODO: Test Continuous spells
        if (spell.getCastType() == CastType.CONTINUOUS) {
            effectiveCastTime = spell.getEffectiveCastTime(spellLevel, serverPlayer);
        }
        magicData.initiateCast(spell, spellLevel, effectiveCastTime, CastSource.COMMAND, "command");
        magicData.setPlayerCastingItem(ItemStack.EMPTY);

        spell.onServerPreCast(serverPlayer.level(), spellLevel, serverPlayer, magicData);

        PacketDistributor.sendToPlayer(serverPlayer, new UpdateCastingStatePacket(spell.getSpellId(), spellLevel, effectiveCastTime, CastSource.COMMAND, "command"));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new OnCastStartedPacket(serverPlayer.getUUID(), spell.getSpellId(), spellLevel));

        if (magicData.getAdditionalCastData() instanceof TargetEntityCastData targetingData) {
            LivingEntity target = targetingData.getTarget((ServerLevel) serverPlayer.level());
            if (target != null) {
                ExampleMod.LOGGER.debug("Casting Spell {} with target {}", magicData.getCastingSpellId(), target.getName().getString());
            }
        } else {
            ExampleMod.LOGGER.warn("Tried to merge Targeting Data but was overridden. Current cast data for spell {}: {}", magicData.getCastingSpellId(), magicData.getAdditionalCastData());
        }

        // For instant cast spells, we need to execute them immediately
        // For spells with cast time > 0, the normal tick handler will execute them
        if (effectiveCastTime == 0) {
            spell.onCast(serverPlayer.level(), spellLevel, serverPlayer, CastSource.COMMAND, magicData);
            PacketDistributor.sendToPlayer(serverPlayer, new OnClientCastPacket(spell.getSpellId(), spellLevel, CastSource.COMMAND, magicData.getAdditionalCastData()));
        }
    }

    public static void updateTargetData(LivingEntity caster, Entity entityHit, MagicData playerMagicData, AbstractSpell spell, Predicate<LivingEntity> filter) {
        LivingEntity livingTarget = null;
        if (entityHit instanceof LivingEntity livingEntity && filter.test(livingEntity)) {
            livingTarget = livingEntity;
        } else if (entityHit instanceof PartEntity<?> partEntity && partEntity.getParent() instanceof LivingEntity livingParent && filter.test(livingParent)) {
            livingTarget = livingParent;
        }


        if (livingTarget != null) {
            playerMagicData.setAdditionalCastData(new TargetEntityCastData(livingTarget));
            if (caster instanceof ServerPlayer serverPlayer) {
                if (spell.getCastType() != CastType.INSTANT) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(livingTarget, spell));
                }
                // TODO: Wrap with config
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.spell_target_success", livingTarget.getDisplayName().getString(), spell.getDisplayName(serverPlayer)).withStyle(ChatFormatting.GREEN)));
            }
            if (livingTarget instanceof ServerPlayer serverPlayer) {
                // TODO: Wrap with config
                Utils.sendTargetedNotification(serverPlayer, caster, spell);
            }
        } else if (caster instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.cast_error_target").withStyle(ChatFormatting.RED)));
        }
    }

} 