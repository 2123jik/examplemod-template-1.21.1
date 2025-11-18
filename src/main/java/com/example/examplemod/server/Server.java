package com.example.examplemod.server;

import com.example.examplemod.accessors.SpellHealEventAccessor;
import com.example.examplemod.capability.IEatenFoods;
import com.example.examplemod.capability.ModCapabilities;
import com.example.examplemod.component.SpellBonusData;
import com.example.examplemod.server.effect.MakenPowerEffect;
import com.example.examplemod.util.AttributeHelper;
import com.google.common.collect.ImmutableMap;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import dev.shadowsoffire.placebo.events.AnvilLandEvent;
import dev.xkmc.l2archery.init.registrate.ArcheryItems;
import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.events.HostilityInitEvent;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity;
import fuzs.enderzoology.EnderZoology;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.SpellContainer;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.spells.lightning.ChainLightningSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.bobmowzie.mowziesmobs.server.item.ItemHandler.WROUGHT_AXE;
import static com.bobmowzie.mowziesmobs.server.item.ItemHandler.WROUGHT_HELMET;
import static com.example.examplemod.ExampleMod.MODID;
import static com.example.examplemod.component.ModDataComponents.*;
import static com.example.examplemod.init.ModEffects.MAKEN_POWER;
import static dev.shadowsoffire.apothic_attributes.api.ALObjects.DamageTypes.*;
import static dev.xkmc.l2archery.init.registrate.ArcheryItems.STARTER_BOW;
import static fuzs.enderzoology.init.ModRegistry.SOULBOUND_ENCHANTMENT;
import static io.redspace.ironsspellbooks.damage.ISSDamageTypes.*;
import static io.redspace.ironsspellbooks.registries.ComponentRegistry.SPELL_CONTAINER;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL;
import static net.minecraft.core.component.DataComponents.FOOD;
import static net.minecraft.world.effect.MobEffects.LEVITATION;
import static twilightforest.init.TFItems.GIANT_SWORD;

public class Server {

    private static final Random RANDOM = new Random();

    // =================================================================
    //                       通用辅助方法和常量
    // =================================================================

    private static final ResourceLocation ATTACK_DAMAGE_GROWTH = ResourceLocation.fromNamespaceAndPath(MODID, "weapon_growth_bonus");
    private static final Map<EquipmentSlot, ResourceLocation> HEALTH_GROWTH_IDS = ImmutableMap.of(
            EquipmentSlot.HEAD, ResourceLocation.fromNamespaceAndPath(MODID, "max_health_head"),
            EquipmentSlot.CHEST, ResourceLocation.fromNamespaceAndPath(MODID, "max_health_chest"),
            EquipmentSlot.LEGS, ResourceLocation.fromNamespaceAndPath(MODID, "max_health_legs"),
            EquipmentSlot.FEET, ResourceLocation.fromNamespaceAndPath(MODID, "max_health_feet")
    );

    public static <T> Holder.Reference<T> getHolder(ResourceKey<T> resourceKey, Level level) {
        return level.registryAccess().registryOrThrow(resourceKey.registryKey()).getHolderOrThrow(resourceKey);
    }

    public static void setEnchant(ItemStack itemStack, Level level, ResourceKey<Enchantment> enchantmentResourceKey, int tier) {
        itemStack.enchant(getHolder(enchantmentResourceKey, level), tier);
    }

    static void setRarity(ItemStack stack) {
        LootRarity mythicRarity = RarityRegistry.INSTANCE.getValue(Apotheosis.loc("mythic"));
        if (mythicRarity != null) {
            AffixHelper.setRarity(stack, mythicRarity);
        }
    }

    public static void addOrUpdateAttribute(ItemStack itemStack, DeferredHolder<DataComponentType<?>, DataComponentType<Double>> valueComponent, Holder<Attribute> attribute, double amountToAdd, EquipmentSlotGroup slot, AttributeModifier.Operation operation, ResourceLocation id) {
        if (amountToAdd == 0) return;

        double currentStoredAmount = itemStack.getOrDefault(valueComponent, 0.0);
        double newTotalAmount = currentStoredAmount + amountToAdd;

        ItemAttributeModifiers allCurrentModifiers = itemStack.getAttributeModifiers();
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        for (ItemAttributeModifiers.Entry entry : allCurrentModifiers.modifiers()) {
            if (!entry.modifier().id().equals(id)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        AttributeModifier newModifier = new AttributeModifier(id, newTotalAmount, operation);
        builder.add(attribute, newModifier, slot);
        itemStack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
        itemStack.set(valueComponent, newTotalAmount);
    }

    // =================================================================
    //                      事件处理内部类
    // =================================================================

    /**
     * 处理游戏组件和能力注册的事件
     */
    @EventBusSubscriber(modid = MODID)
    public static class RegistrationEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            ModCapabilities.register(event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
            final Consumer<DataComponentPatch.Builder> SET_MAX_STACK = builder -> builder.set(DataComponents.MAX_STACK_SIZE, 64);

            // 匹配潜影盒
            event.modifyMatching(item -> item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock, SET_MAX_STACK);
            // 匹配特定物品类型
            List.of(SaddleItem.class, MinecartItem.class, BoatItem.class, SignItem.class, HangingSignItem.class, BucketItem.class, SnowballItem.class, SolidBucketItem.class, MilkBucketItem.class, MobBucketItem.class, EggItem.class, BundleItem.class, SpyglassItem.class, BedItem.class, EnderpearlItem.class, PotionItem.class, SplashPotionItem.class, LingeringPotionItem.class, EnchantedBookItem.class, ArmorStandItem.class, BannerItem.class, BannerPatternItem.class, InstrumentItem.class, HoneyBottleItem.class)
                    .forEach(itemClass -> event.modifyMatching(itemClass::isInstance, SET_MAX_STACK));
            // 匹配特定物品实例
            Set.of(Items.WRITABLE_BOOK,Items.MUSHROOM_STEW, Items.CAKE, Items.RABBIT_STEW, Items.BEETROOT_SOUP, Items.KNOWLEDGE_BOOK)
                    .forEach(item -> event.modifyMatching(item::equals, SET_MAX_STACK));
            // 匹配稀有唱片
            event.modifyMatching(item -> item.getDefaultInstance().has(DataComponents.JUKEBOX_PLAYABLE) && item.getDefaultInstance().getRarity() == Rarity.RARE, SET_MAX_STACK);
        }
    }

    /**
     * 处理与玩家相关的事件，如登录、重生、使用物品
     */
    @EventBusSubscriber(modid = MODID)
    public static class PlayerLifecycleEvents {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity().level().isClientSide()) return;

            Player player = event.getEntity();
            CompoundTag forgeData = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (!forgeData.getBoolean("hasLoggedInBefore")) {
                player.sendSystemMessage(Component.literal("§eWelcome to this world! §aA starter gift has been placed in your inventory."));
                givePlayerStarterGear(player);
                forgeData.putBoolean("hasLoggedInBefore", true);
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, forgeData);
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            // 此事件在玩家被“克隆”时触发，这发生在死亡或从末地返回时。
            // 我们只想在玩家死亡时复制数据。
            if (event.isWasDeath()) {
                // 从原始（死亡前）的玩家实体获取能力实例
                IEatenFoods oldCap = event.getOriginal().getCapability(ModCapabilities.EATEN_FOODS_CAPABILITY);

                // 从新的（重生后）的玩家实体获取能力实例
                IEatenFoods newCap = event.getEntity().getCapability(ModCapabilities.EATEN_FOODS_CAPABILITY);

                // 确保两个能力实例都存在，然后才进行复制
                if (oldCap != null && newCap != null) {
                    newCap.copyFrom(oldCap);
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
            // 检查实体是否为玩家，以及物品是否为食物
            if (event.getEntity() instanceof Player player && event.getItem().has(FOOD)) {
                // 获取玩家的能力实例
                IEatenFoods cap = player.getCapability(ModCapabilities.EATEN_FOODS_CAPABILITY);

                // 检查能力实例是否存在（不为 null）
                if (cap != null) {
                    // 调用 addFood 方法记录吃掉的食物
                    cap.addFood(event.getItem());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(EntityTickEvent.Post event) {

        }

        private static void givePlayerStarterGear(Player player) {
            Level level = player.level();
            player.getInventory().add(createStarterArmor(new ItemStack(Items.NETHERITE_HELMET), "Limitless Helmet", level));
            player.getInventory().add(createStarterArmor(new ItemStack(Items.NETHERITE_CHESTPLATE), "Limitless Chestplate", level));
            player.getInventory().add(createStarterArmor(new ItemStack(Items.NETHERITE_LEGGINGS), "Limitless Leggings", level));
            player.getInventory().add(createStarterArmor(new ItemStack(Items.NETHERITE_BOOTS), "Limitless Boots", level));

            ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
            setEnchant(sword, level, Enchantments.SHARPNESS, 6);
            setEnchant(sword, level, Enchantments.UNBREAKING, 4);
            setEnchant(sword, level, Enchantments.MENDING, 1);
            sword.set(MAKEN_SWORD.get(), 1.0D);
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("Maken Sword"));
            setRarity(sword);
            player.getInventory().add(sword);
            var recovery_compass=new ItemStack(Items.RECOVERY_COMPASS);
            recovery_compass.enchant(getHolder(SOULBOUND_ENCHANTMENT,level),6);
            player.getInventory().add(recovery_compass);
            var ender_pearl = new ItemStack(Items.ENDER_PEARL,5);
        }

        private static ItemStack createStarterArmor(ItemStack armor, String name, Level level) {
            setEnchant(armor, level, Enchantments.PROTECTION, 5);
            setEnchant(armor, level, Enchantments.UNBREAKING, 4);
            setEnchant(armor, level, Enchantments.MENDING, 1);
            armor.set(MAKEN_ARMOR.get(), 1.0D);
            armor.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            setRarity(armor);
            return armor;
        }
    }

    /**
     * 处理实体生成和初始化相关的事件
     */
    @EventBusSubscriber(modid = MODID)
    public static class EntitySpawningEvents {
        private static final Set<ResourceLocation> SPECIAL_TRIM_MATERIALS = Set.of(
                ResourceLocation.withDefaultNamespace("redstone"), ResourceLocation.withDefaultNamespace("quartz"),
                ResourceLocation.withDefaultNamespace("lapis"), ResourceLocation.withDefaultNamespace("netherite"),
                ResourceLocation.withDefaultNamespace("iron"), ResourceLocation.withDefaultNamespace("emerald"),
                ResourceLocation.withDefaultNamespace("diamond"), ResourceLocation.withDefaultNamespace("copper"),
                ResourceLocation.withDefaultNamespace("amethyst")
        );

        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)) return;

            // 监守者特殊生成逻辑
            if (entity instanceof Warden warden) {
                handleWardenSpawn(warden);
            }

            // 为生成的实体随机添加盔甲纹饰
            addRandomArmorTrim(entity);
        }


        private static void handleWardenSpawn(Warden warden) {
            if (Math.random() < (double) AttributeHelper.getL2HostilityLevel(warden).getAsInt() / 12288) {
                if (warden.getMainHandItem().has(WAEDREN_BOSS)) return;
                ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
                sword.set(WAEDREN_BOSS, 7); // 初始7条命
                warden.setItemSlot(EquipmentSlot.MAINHAND, sword);
            }
        }

        private static void addRandomArmorTrim(LivingEntity entity) {
            var patternRegistry = entity.level().registryAccess().registryOrThrow(Registries.TRIM_PATTERN);
            var materialRegistry = entity.level().registryAccess().registryOrThrow(Registries.TRIM_MATERIAL);
            List<Holder.Reference<TrimPattern>> allPatterns = patternRegistry.holders().toList();
            List<Holder.Reference<TrimMaterial>> allMaterials = materialRegistry.holders().toList();

            if (allPatterns.isEmpty() || allMaterials.isEmpty()) return;

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

                ItemStack equippedItem = entity.getItemBySlot(slot);
                if (!equippedItem.isEmpty() && equippedItem.getItem() instanceof ArmorItem armorItem ) {
                    if(!equippedItem.has(DataComponents.TRIM))
                    {
                        Holder.Reference<TrimPattern> randomPattern = allPatterns.get(RANDOM.nextInt(allPatterns.size()));
                        List<Holder.Reference<TrimMaterial>> potentialMaterials = getPotentialMaterials(randomPattern, allMaterials);
                        final Ingredient repairIngredient = armorItem.getMaterial().value().repairIngredient().get();

                        List<Holder.Reference<TrimMaterial>> validMaterials = potentialMaterials.stream()
                                .filter(material -> !repairIngredient.test(new ItemStack(material.value().ingredient().value())))
                                .toList();

                        if (!validMaterials.isEmpty()) {
                            Holder.Reference<TrimMaterial> randomMaterial = validMaterials.get(RANDOM.nextInt(validMaterials.size()));
                            equippedItem.set(DataComponents.TRIM, new ArmorTrim(randomMaterial, randomPattern));
                        }
                    }
                    if(!equippedItem.has(DataComponents.DYED_COLOR))
                    {
                        equippedItem.set(DataComponents.DYED_COLOR,new DyedItemColor((int)(Math.random()*(1<<24)),false));
                    }
                }
            }
        }

        private static List<Holder.Reference<TrimMaterial>> getPotentialMaterials(Holder.Reference<TrimPattern> pattern, List<Holder.Reference<TrimMaterial>> allMaterials) {
            ResourceLocation flowTrim = ResourceLocation.withDefaultNamespace("flow");
            ResourceLocation boltTrim = ResourceLocation.withDefaultNamespace("bolt");
            if (pattern.key().location().equals(flowTrim) || pattern.key().location().equals(boltTrim)) {
                return allMaterials.stream()
                        .filter(material -> SPECIAL_TRIM_MATERIALS.contains(material.key().location()))
                        .toList();
            }
            return allMaterials;
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
    }

    /**
     * 处理生物受到伤害的事件
     */
    @EventBusSubscriber(modid = MODID)
    public static class LivingDamageEvents {
        @SubscribeEvent
        public static void onDamageWithMakenSword(LivingDamageEvent.Pre event) {
            if (event.getSource().getEntity() instanceof LivingEntity attacker && !attacker.level().isClientSide()) {
                ItemStack weapon = attacker.getMainHandItem();
                if (weapon.has(MAKEN_SWORD) && attacker.hasEffect(MAKEN_POWER)) {
                    float absorptionCap = attacker.getMaxHealth() * 0.5f;
                    float currentAbsorption = attacker.getAbsorptionAmount();
                    if (currentAbsorption < absorptionCap) {
                        float absorptionToGain = event.getOriginalDamage() * 0.125f;
                        float finalAbsorption = Math.min(currentAbsorption + absorptionToGain, absorptionCap);
                        attacker.setAbsorptionAmount(finalAbsorption);
                    }
                }
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                ItemStack weapon = player.getMainHandItem();
                if (weapon.has(MAKEN_SWORD.get())) {
                    if (player.getHealth() <= (player.getMaxHealth() * 0.1f) && !player.getCooldowns().isOnCooldown(weapon.getItem())) {
                        event.setNewDamage(1);
                        player.setHealth(player.getMaxHealth() * 0.3f);
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
    }

    /**
     * 处理生物死亡的事件
     */
    @EventBusSubscriber(modid = MODID)
    public static class LivingDeathEvents {
        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            LivingEntity deadEntity = event.getEntity();
            DamageSource source = event.getSource();

            // 玩家死亡时的逻辑
            if (deadEntity instanceof Player player) {
                player.sendSystemMessage(Component.literal("Death Position: " + player.position().toString()));
                handlePlayerDeathWithMakenSword(player);
            }

            // 骷髅特殊掉落
            if (deadEntity instanceof Skeleton && deadEntity.hasEffect(LEVITATION) && Math.abs(Math.abs(deadEntity.getXRot()) - 180) < 45) {
                handleLevitatingSkeletonDeath(deadEntity);
            }

            // 玩家击杀生物时的逻辑
            if (source.getEntity() instanceof Player player) {
                handlePlayerKill(player, deadEntity);
            }
        }

        private static void handlePlayerDeathWithMakenSword(Player player) {
            if (player.getMainHandItem().has(MAKEN_SWORD.get()) && !player.level().isClientSide) {
                double bonusValue = player.getMainHandItem().getOrDefault(MAKEN_SWORD.get(), 0.0);
                float explosionRadius = (float) (player.getMaxHealth() + bonusValue);
                Explosion explosion = new Explosion(player.level(), player, player.getX(), player.getY(), player.getZ(), explosionRadius, true, Explosion.BlockInteraction.DESTROY);
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
            ItemEntity itemEntity = new ItemEntity(skeleton.level(), skeleton.getX(), skeleton.getY(), skeleton.getZ(), scroll);
            skeleton.level().addFreshEntity(itemEntity);
        }

        private static void handlePlayerKill(Player player, LivingEntity killedEntity) {
            if (player.hasEffect(MAKEN_POWER)) {
                MakenPowerEffect.onKill(player);
            }

            if (killedEntity instanceof AbstractGolemEntity golem) {
                handleGolemKill(golem);
            }

            if (player.getMainHandItem().has(MAKEN_SWORD.get())) {
                handleMakenGrowth(player, killedEntity);
            }
        }

        private static void handleGolemKill(AbstractGolemEntity golem) {
            Level level = golem.level();
            if (level.isClientSide) return;
            ExperienceOrb orb = new ExperienceOrb(level, golem.getX(), golem.getY(), golem.getZ(), 5);
            ItemEntity itemEntity = new ItemEntity(level, golem.getX(), golem.getY(), golem.getZ(), new ItemStack(Items.IRON_INGOT, 5));
            level.addFreshEntity(orb);
            level.addFreshEntity(itemEntity);
        }

        private static void handleMakenGrowth(Player player, LivingEntity killedEntity) {
            float entityEHP = killedEntity.getMaxHealth();
            float playerAHP = player.getMaxHealth();

            if (Math.abs(entityEHP - playerAHP) <= 1f) return;

            if (entityEHP < playerAHP) { // 成长攻击
                if (killedEntity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                    double playerAttackDamage = Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
                    double damageToAdd = 0.5 * AttributeHelper.getAttackDamageValue(killedEntity) / playerAttackDamage;
                    addOrUpdateAttribute(player.getMainHandItem(), MAKEN_SWORD, Attributes.ATTACK_DAMAGE, damageToAdd, EquipmentSlotGroup.MAINHAND, AttributeModifier.Operation.ADD_VALUE, ATTACK_DAMAGE_GROWTH);
                }
            } else { // 成长生命
                float totalHealthBonus = 0.125f * (entityEHP - playerAHP);
                distributeHealthBonus(player, totalHealthBonus);
            }
        }

        private static void distributeHealthBonus(Player player, float totalHealthBonus) {
            Map<EquipmentSlot, Integer> ratios = Map.of(
                    EquipmentSlot.HEAD, 5, EquipmentSlot.CHEST, 8,
                    EquipmentSlot.LEGS, 7, EquipmentSlot.FEET, 4
            );
            Map<EquipmentSlot, ItemStack> equippedArmor = new EnumMap<>(EquipmentSlot.class);
            int totalWeight = 0;

            for (var entry : ratios.entrySet()) {
                ItemStack armor = player.getItemBySlot(entry.getKey());
                if (!armor.isEmpty() && armor.has(MAKEN_ARMOR.get())) {
                    equippedArmor.put(entry.getKey(), armor);
                    totalWeight += entry.getValue();
                }
            }

            if (totalWeight > 0) {
                final float finalTotalWeight = totalWeight;
                equippedArmor.forEach((slot, stack) -> {
                    float bonus = totalHealthBonus * (ratios.get(slot) / finalTotalWeight);
                    ResourceLocation id = HEALTH_GROWTH_IDS.get(slot);
                    if (id != null) {
                        addOrUpdateAttribute(stack, MAKEN_ARMOR, Attributes.MAX_HEALTH, bonus, EquipmentSlotGroup.bySlot(slot), AttributeModifier.Operation.ADD_VALUE, id);
                    }
                });
            }
        }
    }

    /**
     * 处理与方块交互相关的事件
     */
    @EventBusSubscriber(modid = MODID)
    public static class BlockInteractionEvents {
        @SubscribeEvent
        public static void onBedrockPlace(BlockEvent.EntityPlaceEvent event) {
            if (event.getState().is(Blocks.BEDROCK)) {
                ItemStack itemStack = new ItemStack((ItemLike) GIANT_SWORD.get());
                itemStack.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(Component.literal("下次攻击伤害修正+200%，但经验等级-2并持续2分钟虚弱II")));

                if (event.getEntity() instanceof Player player) {
                    if (!player.getInventory().add(itemStack)) {
                        player.drop(itemStack, false);
                    }
                } else if (event.getLevel() instanceof Level level) {
                    level.addFreshEntity(new ItemEntity(level, event.getPos().getX() + 0.5, event.getPos().getY() + 1, event.getPos().getZ() + 0.5, itemStack));
                }
            }
        }

        @SubscribeEvent
        public static void onAnvilLand(AnvilLandEvent event) {
            if (event.getLevel().isClientSide()) return;

            BlockPos pos = event.getPos();
            List<ItemEntity> availableItems = new ArrayList<>(event.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0)));

            while (true) {
                boolean craftedInLoop = false;
                if (tryVectorCraft(availableItems, item -> item.is(WROUGHT_AXE.get()), item -> item.has(MAKEN_SWORD.get()))) {
                    craftedInLoop = true;
                    continue;
                }
                if (tryVectorCraft(availableItems, item -> item.is(WROUGHT_HELMET.get()), item -> item.has(MAKEN_ARMOR.get()))) {
                    craftedInLoop = true;
                    continue;
                }
                if (!craftedInLoop) {
                    break;
                }
            }
        }

        private static boolean tryVectorCraft(List<ItemEntity> availableItems, Predicate<ItemStack> toolMatcher, Predicate<ItemStack> targetMatcher) {
            Optional<ItemEntity> toolEntityOpt = availableItems.stream().filter(e -> toolMatcher.test(e.getItem())).findFirst();
            Optional<ItemEntity> targetEntityOpt = availableItems.stream().filter(e -> targetMatcher.test(e.getItem())).findFirst();

            if (toolEntityOpt.isPresent() && targetEntityOpt.isPresent()) {
                ItemEntity toolEntity = toolEntityOpt.get();
                ItemEntity targetEntity = targetEntityOpt.get();

                targetEntity.getItem().set(DataComponents.UNBREAKABLE, new Unbreakable(true));
                toolEntity.getItem().shrink(1);
                if (toolEntity.getItem().isEmpty()) {
                    toolEntity.discard();
                }

                availableItems.remove(toolEntity);
                availableItems.remove(targetEntity); // 目标物品也被消耗或转换，应从池中移除
                return true;
            }
            return false;
        }
    }

    /**
     * 处理与法术系统（IronsSpellbooks）相关的事件
     */
    @EventBusSubscriber(modid = MODID)
    public static class SpellSystemEvents {
        public static final String SPELL_CAST_COUNT_TAG = "Examplemod_SpellCounts";

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
            if (event.getSpellDamageSource().getEntity() instanceof LivingEntity attacker && !event.getSpellDamageSource().is(ALObjects.Tags.CANNOT_CRITICALLY_STRIKE)) {
                int spellCastCount = getProficiencyForSpell(attacker, event.getSpellDamageSource().spell().getSpellId());
                double castCountBonus = calculateSCurveBonus(spellCastCount, 2.0, 100, 0.01);
                double critChance = attacker.getAttributeValue(ALObjects.Attributes.CRIT_CHANCE) * (1 + castCountBonus);
                float critDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE);

                RandomSource rand = attacker.getRandom();
                float finalDamage = event.getAmount();
                float baseDamage = event.getAmount();

                while (rand.nextFloat() < critChance) {
                    critChance -= 1.0;
                    finalDamage += baseDamage * (critDmg - 1.0F);
                    critDmg *= 0.85F;
                }

                if (finalDamage > baseDamage && !attacker.level().isClientSide) {
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) attacker.level(), event.getEntity().chunkPosition(), new CritParticlePayload(event.getEntity().getId()));
                }
                event.setAmount(finalDamage);
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
    }

    public static float crit(Player player, LivingEntity target) {
        var f2=player.getAttackStrengthScale(0.5F);
        boolean flag4 = f2 > 0.9F;
        boolean flag1 = flag4
                && player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger()
                && !player.isSprinting();
        var critEvent = net.neoforged.neoforge.common.CommonHooks.fireCriticalHit(player,target, flag1, flag1 ? 1.5F : 1.0F);
        flag1 = critEvent.isCriticalHit();
        return critEvent.getDamageMultiplier();
    }

//    // EntityJoinLevelEvent 是一个更可靠的捕捉实体在世界中“出现”的事件
//    @SubscribeEvent
//    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
//
//        if (event.getEntity() instanceof MagicFireball ||event.getEntity() instanceof SmallMagicFireball) {
//            var fireball = event.getEntity();
//            Vec3 originPos = fireball.position();
//            fireball.setPos(originPos.subtract(0,-1,2));
//            // 2. 获取火球的朝向向量 (一个长度为1的向量)
//            Vec3 lookVec = fireball.getLookAngle();
//            // 3. 将位置沿着朝向向前偏移一小段距离（例如1个方块）
//            // 这样涟漪看起来就像是从火球的前方发出
//            double offsetDistance = 1.0;
//            Vec3 finalPos = originPos.add(lookVec.scale(offsetDistance));
//
//            // 4. 使用这个计算出的最终位置来启动效果
//            ScreenRippleRenderer.startEffect(finalPos);
//        }
//    }
@SubscribeEvent
public static void onPreDamage(LivingDamageEvent.Pre event) {
    if (event.getEntity().level().isClientSide()) {
        return;
    }
    DamageSource originalSource = event.getSource();

    if (originalSource.is(FIRE_DAMAGE)) {
        event.setNewDamage(0);
        DamageSource newSource = new DamageSource(
                event.getEntity().level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FIRE_MAGIC),
                originalSource.getDirectEntity(),
                originalSource.getEntity()
        );

        event.getEntity().hurt(newSource, event.getOriginalDamage());
    }
    if (originalSource.is(COLD_DAMAGE)) {
        event.setNewDamage(0);
        DamageSource newSource = new DamageSource(
                event.getEntity().level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ICE_MAGIC),
                originalSource.getDirectEntity(),
                originalSource.getEntity()
        );

        event.getEntity().hurt(newSource, event.getOriginalDamage());
    }
    if (originalSource.is(CURRENT_HP_DAMAGE)) {
        event.setNewDamage(0);
        DamageSource newSource = new DamageSource(
                event.getEntity().level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ELDRITCH_MAGIC),
                originalSource.getDirectEntity(),
                originalSource.getEntity()
        );

        event.getEntity().hurt(newSource, event.getOriginalDamage());
    }

}
    @SubscribeEvent
    public static void onSpellCast(ModifySpellLevelEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) {
            return;
        }

        ResourceLocation spellId = event.getSpell().getSpellResource();
        SchoolType schoolType = event.getSpell().getSchoolType();
        int totalBonus = 0;

        // 1. 检测主手和副手
        totalBonus += getBonusFromStack(entity.getMainHandItem(), spellId, schoolType);
        totalBonus += getBonusFromStack(entity.getOffhandItem(), spellId, schoolType);

        // 2. 检测护甲槽
        for (ItemStack armorStack : entity.getArmorSlots()) {
            totalBonus += getBonusFromStack(armorStack, spellId, schoolType);
        }

        // 3. 检测 Curios API 饰品槽 (已修正)
        totalBonus += getBonusFromCurios(entity, spellId, schoolType);

        if (totalBonus > 0) {
            event.setLevel(event.getLevel() + totalBonus);
        }
    }

    /**
     * 辅助方法：从单个 ItemStack 计算法术加成。 (此方法不变)
     */
    private static int getBonusFromStack(ItemStack stack, ResourceLocation spellId, SchoolType schoolType) {
        if (stack != null && !stack.isEmpty() && stack.has(SPELL_BONUSES)) {
            SpellBonusData bonusData = stack.get(SPELL_BONUSES);
            if (bonusData != null) {
                return bonusData.getTotalBonusFor(spellId, schoolType);
            }
        }
        return 0;
    }

    /**
     * 【已修正】辅助方法：检查所有 Curios 槽位并返回总加成。
     */
    private static int getBonusFromCurios(LivingEntity entity, ResourceLocation spellId, SchoolType schoolType) {
        // 检查 Curios 模组是否已加载
        if (ModList.get().isLoaded("curios")) {
            // CuriosApi.getCuriosInventory 返回 Optional<ICuriosItemHandler>
            Optional<ICuriosItemHandler> inventoryOptional = CuriosApi.getCuriosInventory(entity);

            if (inventoryOptional.isPresent()) {
                ICuriosItemHandler inventory = inventoryOptional.get();
                int curiosBonus = 0;

                // 【正确用法】使用 findCurios 方法获取所有已装备的饰品。
                // 我们传入一个始终为 true 的断言 (predicate)，来匹配所有物品。
                List<SlotResult> equippedCurios = inventory.findCurios(stack -> true);

                for (SlotResult slotResult : equippedCurios) {
                    // 从每个 SlotResult 中获取 ItemStack，并计算加成
                    curiosBonus += getBonusFromStack(slotResult.stack(), spellId, schoolType);
                }
                return curiosBonus;
            }
        }

        // 如果 Curios 未安装或实体没有饰品栏，返回0
        return 0;
    }
}