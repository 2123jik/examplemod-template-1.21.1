package com.example.examplemod.server.events;

import com.example.examplemod.server.effect.MakenPowerEffect;
import com.example.examplemod.server.util.ServerEventUtils;
import com.example.examplemod.util.AttributeHelper;
import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.capabilities.magic.SpellContainer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.EnumMap;
import java.util.Map;

import static com.example.examplemod.ExampleMod.MODID;
import static com.example.examplemod.component.ModDataComponents.MAKEN_ARMOR;
import static com.example.examplemod.component.ModDataComponents.MAKEN_SWORD;
import static com.example.examplemod.component.ModDataComponents.WAEDREN_BOSS;
import static com.example.examplemod.register.ModEffects.MAKEN_POWER;
import static dev.shadowsoffire.apothic_attributes.api.ALObjects.DamageTypes.*;
import static io.redspace.ironsspellbooks.damage.ISSDamageTypes.*;
import static io.redspace.ironsspellbooks.registries.ComponentRegistry.SPELL_CONTAINER;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL;
import static net.minecraft.world.effect.MobEffects.LEVITATION;

@EventBusSubscriber(modid = MODID)
public class CombatEventHandler {

    @SubscribeEvent
    public static void onPreDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;
        DamageSource originalSource = event.getSource();
        RegistryAccess registryAccess = event.getEntity().level().registryAccess();

        if (originalSource.is(FIRE_DAMAGE)) {
            replaceDamageSource(event, registryAccess, FIRE_MAGIC);
        } else if (originalSource.is(COLD_DAMAGE)) {
            replaceDamageSource(event, registryAccess, ICE_MAGIC);
        } else if (originalSource.is(CURRENT_HP_DAMAGE)) {
            replaceDamageSource(event, registryAccess, ELDRITCH_MAGIC);
        }
    }

    @SubscribeEvent
    public static void onDamageWithMakenSword(LivingDamageEvent.Pre event) {
        // 攻击者逻辑
        if (event.getSource().getEntity() instanceof LivingEntity attacker && !attacker.level().isClientSide()) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.has(MAKEN_SWORD) && attacker.hasEffect(MAKEN_POWER)) {
                float absorptionCap = attacker.getMaxHealth() * 0.5f;
                float currentAbsorption = attacker.getAbsorptionAmount();
                if (currentAbsorption < absorptionCap) {
                    float absorptionToGain = event.getOriginalDamage() * 0.125f;
                    attacker.setAbsorptionAmount(Math.min(currentAbsorption + absorptionToGain, absorptionCap));
                }
            }
        }

        // 受害者逻辑
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack weapon = player.getMainHandItem();
            if (weapon.has(MAKEN_SWORD.get())) {
                if (player.getHealth() <= (player.getMaxHealth() * 0.1f) && !player.getCooldowns().isOnCooldown(weapon.getItem())) {
                    event.setNewDamage(1);
                    player.heal(player.getMaxHealth() * 0.3f);
                    player.addEffect(new MobEffectInstance(MAKEN_POWER, 400, 0));
                    player.getCooldowns().addCooldown(weapon.getItem(), 3600);
                    player.sendSystemMessage(Component.literal("§cYour Maken Sword awakens to protect you!"));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWardenHurtForLastStand(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Warden warden && event.getEntity().level() instanceof ServerLevel serverLevel) {
            ItemStack mainHand = warden.getMainHandItem();
            if (mainHand.has(WAEDREN_BOSS)) {
                int livesLeft = mainHand.get(WAEDREN_BOSS);
                if (livesLeft > 0 && (warden.getHealth() - event.getOriginalDamage()) <= 0) {
                    event.setNewDamage(0);
                    mainHand.set(WAEDREN_BOSS, livesLeft - 1);
                    warden.setHealth(warden.getMaxHealth());
                    warden.removeAllEffects();
                    serverLevel.playSound(null, warden.blockPosition(), SoundEvents.WARDEN_ROAR, warden.getSoundSource(), 2.0F, 0.5F);
                    serverLevel.playSound(null, warden.blockPosition(), SoundEvents.WITHER_SPAWN, warden.getSoundSource(), 1.0F, 1.0F);
                    warden.setPose(Pose.ROARING);

                    ((GeneralCapabilityHolder) LHMiscs.MOB.type()).getExisting(warden).ifPresent(capObject -> {
                        MobTraitCap cap = (MobTraitCap) capObject;
                        cap.reinit(warden, cap.lv, true);
                    });
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        DamageSource source = event.getSource();

        if (deadEntity instanceof Player player) {
            player.sendSystemMessage(Component.literal("Death Position: " + player.position().toString()));
            handlePlayerDeathWithMakenSword(player);
        }

        if (deadEntity instanceof Skeleton && deadEntity.hasEffect(LEVITATION) && Math.abs(Math.abs(deadEntity.getXRot()) - 180) < 45) {
            handleLevitatingSkeletonDeath(deadEntity);
        }

        if (source.getEntity() instanceof Player player) {
            handlePlayerKill(player, deadEntity);
        }
    }

    // --- Helpers ---

    private static void replaceDamageSource(LivingDamageEvent.Pre event, RegistryAccess registry, ResourceKey<net.minecraft.world.damagesource.DamageType> newTypeKey) {
        event.setNewDamage(0);
        DamageSource newSource = new DamageSource(
                registry.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(newTypeKey),
                event.getSource().getDirectEntity(),
                event.getSource().getEntity()
        );
        event.getEntity().hurt(newSource, event.getOriginalDamage());
    }

    private static void handlePlayerDeathWithMakenSword(Player player) {
        if (player.getMainHandItem().has(MAKEN_SWORD.get()) && !player.level().isClientSide) {
            double bonusValue = player.getMainHandItem().getOrDefault(MAKEN_SWORD.get(), 0.0);
            float explosionRadius = (float) (player.getMaxHealth() + bonusValue);
            Explosion explosion = new Explosion(player.level(), player, player.getX(), player.getY(), player.getZ(), explosionRadius, false, Explosion.BlockInteraction.KEEP);
            explosion.explode();
            explosion.finalizeExplosion(true);
        }
    }

    private static void handleLevitatingSkeletonDeath(LivingEntity skeleton) {
        if (skeleton.level().isClientSide) return;
        SpellSlot spellSlot = SpellSlot.of(new SpellData(SpellRegistry.FIREBOLT_SPELL.get(), 0), 0);
        SpellContainer spellContainer = new SpellContainer(1, false, false, true, new SpellSlot[]{spellSlot});
        ItemStack scroll = new ItemStack(SCROLL.get());
        scroll.set(SPELL_CONTAINER, spellContainer);
        skeleton.level().addFreshEntity(new ItemEntity(skeleton.level(), skeleton.getX(), skeleton.getY(), skeleton.getZ(), scroll));
    }

    private static void handlePlayerKill(Player player, LivingEntity killedEntity) {
        if (player.hasEffect(MAKEN_POWER)) {
            MakenPowerEffect.onKill(player);
        }
        if (killedEntity instanceof AbstractGolemEntity golem) {
            Level level = golem.level();
            if (!level.isClientSide) {
                level.addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level, golem.getX(), golem.getY(), golem.getZ(), 5));
                level.addFreshEntity(new ItemEntity(level, golem.getX(), golem.getY(), golem.getZ(), new ItemStack(Items.IRON_INGOT, 5)));
            }
        }
        if (player.getMainHandItem().has(MAKEN_SWORD.get())) {
            handleMakenGrowth(player, killedEntity);
        }
    }

    private static void handleMakenGrowth(Player player, LivingEntity killedEntity) {
        float entityHP = killedEntity.getMaxHealth();
        float playerHP = player.getMaxHealth();
        if (Math.abs(entityHP - playerHP) <= 1f) return;

        if (entityHP < playerHP) {
            if (killedEntity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                double playerDmg = Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
                double damageToAdd = 0.5 * AttributeHelper.getAttackDamageValue(killedEntity) / playerDmg;
                ServerEventUtils.addOrUpdateAttribute(player.getMainHandItem(), MAKEN_SWORD, Attributes.ATTACK_DAMAGE, damageToAdd, EquipmentSlotGroup.MAINHAND, AttributeModifier.Operation.ADD_VALUE, ServerEventUtils.ATTACK_DAMAGE_GROWTH_ID);
            }
        } else {
            float totalHealthBonus = 0.125f * (entityHP - playerHP);
            distributeHealthBonus(player, totalHealthBonus);
        }
    }

    private static void distributeHealthBonus(Player player, float totalHealthBonus) {
        Map<EquipmentSlot, Integer> ratios = Map.of(
                EquipmentSlot.HEAD, 5, EquipmentSlot.CHEST, 8,
                EquipmentSlot.LEGS, 7, EquipmentSlot.FEET, 4
        );
        Map<EquipmentSlot, ItemStack> equippedMakenArmor = new EnumMap<>(EquipmentSlot.class);
        int totalWeight = 0;

        for (var entry : ratios.entrySet()) {
            ItemStack armor = player.getItemBySlot(entry.getKey());
            if (!armor.isEmpty() && armor.has(MAKEN_ARMOR.get())) {
                equippedMakenArmor.put(entry.getKey(), armor);
                totalWeight += entry.getValue();
            }
        }

        if (totalWeight > 0) {
            final float finalTotalWeight = totalWeight;
            equippedMakenArmor.forEach((slot, stack) -> {
                float bonus = totalHealthBonus * (ratios.get(slot) / finalTotalWeight);
                ResourceLocation id = ServerEventUtils.HEALTH_GROWTH_IDS.get(slot);
                if (id != null) {
                    ServerEventUtils.addOrUpdateAttribute(stack, MAKEN_ARMOR, Attributes.MAX_HEALTH, bonus, EquipmentSlotGroup.bySlot(slot), AttributeModifier.Operation.ADD_VALUE, id);
                }
            });
        }
    }
}