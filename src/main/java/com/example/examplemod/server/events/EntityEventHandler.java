package com.example.examplemod.server.events;

import com.example.examplemod.server.util.ServerEventUtils;
import com.example.examplemod.util.AttributeHelper;
import dev.xkmc.l2hostility.events.HostilityInitEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.necromancer.NecromancerEntity;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.WitherSkullProjectile;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.examplemod.ExampleMod.MODID;
import static com.example.examplemod.component.ModDataComponents.WAEDREN_BOSS;
import static net.minecraft.resources.ResourceLocation.withDefaultNamespace;

@EventBusSubscriber(modid = MODID)
public class EntityEventHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        // AI 修改
        if (event.getEntity() instanceof NecromancerEntity necromancer) {
            modifyNecromancerAI(necromancer);
        }

        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        // Warden Boss化
        if (entity instanceof Warden warden) {
            handleWardenSpawn(warden);
        }

        // 随机纹饰
        addRandomArmorTrim(entity);


    }

    @SubscribeEvent
    public static void onWardenHostilityInit(HostilityInitEvent.Post event) {
        if (event.getEntity() instanceof Warden warden) {
            ItemStack mainHand = warden.getMainHandItem();
            if (mainHand.has(WAEDREN_BOSS)) {
                int baseLevel = (int) (event.getData().getLevel() * 1.28);
                event.getData().setLevel(warden, baseLevel);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        var nbt = entity.getPersistentData();
        if (nbt.contains("KillDelay")) {
            int timer = nbt.getInt("KillDelay");
            timer--;
            if (timer <= 0) {
                entity.discard();
            } else {
                nbt.putInt("KillDelay", timer);
            }
        }
    }

    // --- Helpers ---

    private static void modifyNecromancerAI(NecromancerEntity necromancer) {
        List<Goal> goalsToRemove = new ArrayList<>();
        necromancer.goalSelector.getAvailableGoals().forEach(wrappedGoal -> {
            if (wrappedGoal.getGoal() instanceof WizardAttackGoal) {
                goalsToRemove.add(wrappedGoal.getGoal());
            }
        });
        goalsToRemove.forEach(necromancer.goalSelector::removeGoal);

        WizardAttackGoal customMagicGoal = new WizardAttackGoal(necromancer, 1.25F, 35, 80)
                .setSpells(
                        List.of(SpellRegistry.FIREBALL_SPELL.get(), SpellRegistry.LIGHTNING_LANCE_SPELL.get()),
                        List.of(SpellRegistry.SHIELD_SPELL.get()),
                        List.of(),
                        List.of()
                )
                .setDrinksPotions();
        necromancer.goalSelector.addGoal(4, customMagicGoal);
    }

    private static void handleWardenSpawn(Warden warden) {
        if (Math.random() < (double) AttributeHelper.getL2HostilityLevel(warden).getAsInt() / 12288) {
            if (warden.getMainHandItem().has(WAEDREN_BOSS)) return;
            ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
            sword.set(WAEDREN_BOSS, 7);
            warden.setItemSlot(EquipmentSlot.MAINHAND, sword);
        }
    }

    private static void addRandomArmorTrim(LivingEntity entity) {
        var registryAccess = entity.level().registryAccess();
        var patternRegistry = registryAccess.registryOrThrow(Registries.TRIM_PATTERN);
        var materialRegistry = registryAccess.registryOrThrow(Registries.TRIM_MATERIAL);

        List<Holder.Reference<TrimPattern>> allPatterns = patternRegistry.holders().toList();
        List<Holder.Reference<TrimMaterial>> allMaterials = materialRegistry.holders().toList();

        if (allPatterns.isEmpty() || allMaterials.isEmpty()) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

            ItemStack equippedItem = entity.getItemBySlot(slot);
            if (!equippedItem.isEmpty() && equippedItem.getItem() instanceof ArmorItem armorItem) {
                if (!equippedItem.has(DataComponents.TRIM)) {
                    Holder.Reference<TrimPattern> randomPattern = allPatterns.get(RANDOM.nextInt(allPatterns.size()));
                    List<Holder.Reference<TrimMaterial>> potentialMaterials = getPotentialMaterials(randomPattern, allMaterials);
                    Ingredient repairIngredient = armorItem.getMaterial().value().repairIngredient().get();

                    List<Holder.Reference<TrimMaterial>> validMaterials = potentialMaterials.stream()
                            .filter(material -> !repairIngredient.test(new ItemStack(material.value().ingredient().value())))
                            .toList();

                    if (!validMaterials.isEmpty()) {
                        Holder.Reference<TrimMaterial> randomMaterial = validMaterials.get(RANDOM.nextInt(validMaterials.size()));
                        equippedItem.set(DataComponents.TRIM, new ArmorTrim(randomMaterial, randomPattern).withTooltip(false));
                    }
                }
                if (!equippedItem.has(DataComponents.DYED_COLOR)) {
                    equippedItem.set(DataComponents.DYED_COLOR, new DyedItemColor((int) (Math.random() * (1 << 24)), false));
                }
            }
        }
    }

    private static List<Holder.Reference<TrimMaterial>> getPotentialMaterials(Holder.Reference<TrimPattern> pattern, List<Holder.Reference<TrimMaterial>> allMaterials) {
        ResourceLocation flowTrim = withDefaultNamespace("flow");
        ResourceLocation boltTrim = withDefaultNamespace("bolt");
        if (pattern.key().location().equals(flowTrim) || pattern.key().location().equals(boltTrim)) {
            return allMaterials.stream()
                    .filter(material -> ServerEventUtils.SPECIAL_TRIM_MATERIALS.contains(material.key().location()))
                    .toList();
        }
        return allMaterials;
    }
}