package com.example.examplemod.server.events;

import com.example.examplemod.accessors.SpellHealEventAccessor;
import com.example.examplemod.component.SpellBonusData;
import com.example.examplemod.server.util.ServerEventUtils;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.spells.lightning.ChainLightningSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;
import java.util.Optional;

import static com.example.examplemod.ExampleMod.MODID;
import static com.example.examplemod.component.ModDataComponents.SPELL_BONUSES;
import static com.example.examplemod.server.util.ServerEventUtils.SPELL_CAST_COUNT_TAG;

@EventBusSubscriber(modid = MODID)
public class MagicEventHandler {

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getEntity().level().isClientSide() || !event.getBlocked()) return;

        Entity sourceEntity = event.getDamageSource().getEntity();
        if (sourceEntity instanceof TraceableEntity traceable) {
            sourceEntity = traceable.getOwner();
        }

        if (sourceEntity instanceof LivingEntity actualAttacker) {
            ChainLightningSpell spell = new ChainLightningSpell();
            int spellLevel = 1;
            MagicData magicData = MagicData.getPlayerMagicData(event.getEntity());
            magicData.setAdditionalCastData(new TargetEntityCastData(actualAttacker));

            if (spell.checkPreCastConditions(event.getEntity().level(), spellLevel, event.getEntity(), magicData)) {
                spell.onCast(event.getEntity().level(), spellLevel, event.getEntity(), CastSource.SWORD, magicData);
            }
        }
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag forgeData = persistentData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
            CompoundTag spellCounts = forgeData.getCompound(SPELL_CAST_COUNT_TAG);

            int newCount = spellCounts.getInt(event.getSpellId()) + 1;
            spellCounts.putInt(event.getSpellId(), newCount);

            forgeData.put(SPELL_CAST_COUNT_TAG, spellCounts);
            persistentData.put(ServerPlayer.PERSISTED_NBT_TAG, forgeData);
        }
        event.setManaCost(event.getManaCost() / 2);
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        LivingEntity attacker = (LivingEntity) event.getSpellDamageSource().getEntity();
        if (attacker != null && !event.getSpellDamageSource().is(ALObjects.Tags.CANNOT_CRITICALLY_STRIKE)) {

            int spellCastCount = getProficiencyForSpell(attacker, event.getSpellDamageSource().spell().getSpellId());
            double castCountBonus = calculateSCurveBonus(spellCastCount, 2.0, 100, 0.01);

            double critChance = attacker.getAttributeValue(ALObjects.Attributes.CRIT_CHANCE) * (1 + castCountBonus);
            float critDmgMultiplier = (float) attacker.getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE);

            RandomSource rand = attacker.getRandom();
            float totalDamage = event.getAmount();
            float baseDamage = event.getAmount();

            boolean didCrit = false;
            while (rand.nextFloat() < critChance) {
                didCrit = true;
                critChance -= 1.0;
                totalDamage += baseDamage * (critDmgMultiplier - 1.0F);
                critDmgMultiplier *= 0.85F;
            }

            if (didCrit && !attacker.level().isClientSide) {
                PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) attacker.level(), event.getEntity().chunkPosition(), new CritParticlePayload(event.getEntity().getId()));
            }
            event.setAmount(totalDamage);
        }
    }

    @SubscribeEvent
    public static void onSpellHeal(SpellHealEvent event) {
        if (event.getEntity() instanceof LivingEntity caster) {
            int schoolProficiency = getProficiencyForSchool(caster, event.getSchoolType());
            float proficiencyBonus = (float) (1.0F + calculateSCurveBonus(schoolProficiency, 2.0, 100, 0.01));
            ((SpellHealEventAccessor) event).setHealAmount(event.getHealAmount() * proficiencyBonus);
        }
    }

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        ResourceLocation spellId = event.getSpell().getSpellResource();
        SchoolType schoolType = event.getSpell().getSchoolType();
        int totalBonus = 0;

        totalBonus += getBonusFromStack(entity.getMainHandItem(), spellId, schoolType);
        totalBonus += getBonusFromStack(entity.getOffhandItem(), spellId, schoolType);
        for (ItemStack armorStack : entity.getArmorSlots()) {
            totalBonus += getBonusFromStack(armorStack, spellId, schoolType);
        }

        totalBonus += getBonusFromCurios(entity, spellId, schoolType);

        if (totalBonus > 0) {
            event.setLevel(event.getLevel() + totalBonus);
        }
    }

    // --- Helpers ---

    private static int getProficiencyForSpell(LivingEntity caster, String spellId) {
        CompoundTag spellCounts = caster.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG).getCompound(SPELL_CAST_COUNT_TAG);
        return spellCounts.getInt(spellId);
    }

    private static int getProficiencyForSchool(LivingEntity caster, SchoolType targetSchool) {
        CompoundTag spellCounts = caster.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG).getCompound(SPELL_CAST_COUNT_TAG);
        int totalProficiency = 0;
        for (String spellId : spellCounts.getAllKeys()) {
            AbstractSpell spell = SpellRegistry.getSpell(spellId);
            if (spell != null && spell.getSchoolType() == targetSchool) {
                totalProficiency += spellCounts.getInt(spellId);
            }
        }
        return totalProficiency;
    }

    private static double calculateSCurveBonus(double currentValue, double maxValue, double midPoint, double steepness) {
        return maxValue / (1 + Math.exp(-steepness * (currentValue - midPoint)));
    }

    private static int getBonusFromStack(ItemStack stack, ResourceLocation spellId, SchoolType schoolType) {
        if (stack != null && !stack.isEmpty() && stack.has(SPELL_BONUSES)) {
            SpellBonusData bonusData = stack.get(SPELL_BONUSES);
            if (bonusData != null) {
                return bonusData.getTotalBonusFor(spellId, schoolType);
            }
        }
        return 0;
    }

    private static int getBonusFromCurios(LivingEntity entity, ResourceLocation spellId, SchoolType schoolType) {
        if (ModList.get().isLoaded("curios")) {
            Optional<ICuriosItemHandler> inventoryOptional = CuriosApi.getCuriosInventory(entity);
            if (inventoryOptional.isPresent()) {
                int curiosBonus = 0;
                List<SlotResult> equippedCurios = inventoryOptional.get().findCurios(stack -> true);
                for (SlotResult slotResult : equippedCurios) {
                    curiosBonus += getBonusFromStack(slotResult.stack(), spellId, schoolType);
                }
                return curiosBonus;
            }
        }
        return 0;
    }
}