package com.example.examplemod.server;

import com.example.examplemod.accessors.SpellHealEventAccessor;
import com.example.examplemod.capability.IEatenFoods;
import com.example.examplemod.capability.ModCapabilities;
import com.example.examplemod.component.SpellBonusData;
import com.example.examplemod.server.effect.MakenPowerEffect;
import com.example.examplemod.util.AttributeHelper;
import com.google.common.collect.ImmutableMap;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import dev.shadowsoffire.placebo.events.AnvilLandEvent;
import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder;
import dev.xkmc.l2core.capability.player.PlayerCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.events.HostilityInitEvent;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity;
import io.redspace.ironsspellbooks.api.events.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.SpellContainer;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.spells.lightning.ChainLightningSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.bobmowzie.mowziesmobs.server.item.ItemHandler.WROUGHT_AXE;
import static com.bobmowzie.mowziesmobs.server.item.ItemHandler.WROUGHT_HELMET;
import static com.example.examplemod.ExampleMod.MODID;
import static com.example.examplemod.ExampleMod.modResourceLoc;
import static com.example.examplemod.component.ModDataComponents.*;
import static com.example.examplemod.init.ModEffects.MAKEN_POWER;
import static dev.shadowsoffire.apothic_attributes.api.ALObjects.DamageTypes.*;
import static fuzs.enderzoology.init.ModRegistry.SOULBOUND_ENCHANTMENT;
import static io.redspace.ironsspellbooks.damage.ISSDamageTypes.*;
import static io.redspace.ironsspellbooks.registries.ComponentRegistry.SPELL_CONTAINER;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL;
import static net.minecraft.resources.ResourceLocation.withDefaultNamespace;
import static net.minecraft.world.effect.MobEffects.LEVITATION;
import static twilightforest.init.TFItems.GIANT_SWORD;

/**
 * 服务端事件处理聚合类。
 * 负责处理实体逻辑、玩家交互、战斗系统、法术增强以及各Mod兼容逻辑。
 */
public class ServerEventHandler {

    private static final Random RANDOM = new Random();

    // =================================================================
    //                       常量定义
    // =================================================================

    private static final String SPELL_CAST_COUNT_TAG = "Examplemod_SpellCounts";
    private static final ResourceLocation ATTACK_DAMAGE_GROWTH_ID = modResourceLoc( "weapon_growth_bonus");

    // Maken 装备各部位生命值成长 AttributeModifier 的 ID
    private static final Map<EquipmentSlot, ResourceLocation> HEALTH_GROWTH_IDS = ImmutableMap.of(
            EquipmentSlot.HEAD, modResourceLoc( "max_health_head"),
            EquipmentSlot.CHEST, modResourceLoc( "max_health_chest"),
            EquipmentSlot.LEGS, modResourceLoc( "max_health_legs"),
            EquipmentSlot.FEET, modResourceLoc( "max_health_feet")
    );

    // 特殊的盔甲纹饰材料列表（红石、钻石等）
    private static final Set<ResourceLocation> SPECIAL_TRIM_MATERIALS = Set.of(
            withDefaultNamespace("redstone"), withDefaultNamespace("quartz"),
            withDefaultNamespace("lapis"), withDefaultNamespace("netherite"),
            withDefaultNamespace("iron"), withDefaultNamespace("emerald"),
            withDefaultNamespace("diamond"), withDefaultNamespace("copper"),
           withDefaultNamespace("amethyst")
    );

    // =================================================================
    //                       通用工具方法
    // =================================================================

    /**
     * 获取注册表对象的 Holder
     */
    public static <T> Holder.Reference<T> getHolder(ResourceKey<T> resourceKey, Level level) {
        return level.registryAccess().registryOrThrow(resourceKey.registryKey()).getHolderOrThrow(resourceKey);
    }

    /**
     * 辅助附魔方法
     */
    public static void setEnchant(ItemStack itemStack, Level level, ResourceKey<Enchantment> enchantmentResourceKey, int tier) {
        itemStack.enchant(getHolder(enchantmentResourceKey, level), tier);
    }

    /**
     * 设置 Apotheosis 神话稀有度
     */
    static void setApotheosisMythicRarity(ItemStack stack) {
        LootRarity mythicRarity = RarityRegistry.INSTANCE.getValue(Apotheosis.loc("mythic"));
        if (mythicRarity != null) {
            AffixHelper.setRarity(stack, mythicRarity);
        }
    }

    /**
     * 向 ItemStack 添加或更新 AttributeModifier（支持 Data Components）
     */
    public static void addOrUpdateAttribute(ItemStack itemStack,
                                            DeferredHolder<DataComponentType<?>, DataComponentType<Double>> valueComponent,
                                            Holder<Attribute> attribute,
                                            double amountToAdd,
                                            EquipmentSlotGroup slot,
                                            AttributeModifier.Operation operation,
                                            ResourceLocation id) {
        if (amountToAdd == 0) return;

        double currentStoredAmount = itemStack.getOrDefault(valueComponent, 0.0);
        double newTotalAmount = currentStoredAmount + amountToAdd;

        // 获取当前的属性修改器并构建新的列表，移除旧的同ID修改器
        ItemAttributeModifiers allCurrentModifiers = itemStack.getAttributeModifiers();
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        for (ItemAttributeModifiers.Entry entry : allCurrentModifiers.modifiers()) {
            if (!entry.modifier().id().equals(id)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // 添加更新后的修改器
        AttributeModifier newModifier = new AttributeModifier(id, newTotalAmount, operation);
        builder.add(attribute, newModifier, slot);

        // 保存回 ItemStack
        itemStack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
        itemStack.set(valueComponent, newTotalAmount);
    }

    // =================================================================
    //                      1. 注册与初始化事件
    // =================================================================

    @EventBusSubscriber(modid = MODID)
    public static class RegistrationEvents {

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            ModCapabilities.register(event);
        }

        /**
         * 修改原版物品的默认组件，例如增加堆叠上限
         */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
            final Consumer<DataComponentPatch.Builder> SET_MAX_STACK_64 = builder -> builder.set(DataComponents.MAX_STACK_SIZE, 64);

            // 1. 潜影盒
            event.modifyMatching(item -> item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock, SET_MAX_STACK_64);

            // 2. 各种通常不可堆叠或堆叠数低的物品类型
            List<Class<? extends Item>> stackableTypes = List.of(
                    SaddleItem.class, MinecartItem.class, BoatItem.class, SignItem.class, HangingSignItem.class,
                    BucketItem.class, SnowballItem.class, SolidBucketItem.class, MilkBucketItem.class, MobBucketItem.class,
                    EggItem.class, BundleItem.class, SpyglassItem.class, BedItem.class, EnderpearlItem.class,
                    PotionItem.class, SplashPotionItem.class, LingeringPotionItem.class, EnchantedBookItem.class,
                    ArmorStandItem.class, BannerItem.class, BannerPatternItem.class, InstrumentItem.class, HoneyBottleItem.class
            );
            stackableTypes.forEach(type -> event.modifyMatching(type::isInstance, SET_MAX_STACK_64));

            // 3. 具体物品实例
            Set<Item> specificItems = Set.of(Items.WRITABLE_BOOK, Items.MUSHROOM_STEW, Items.CAKE, Items.RABBIT_STEW, Items.BEETROOT_SOUP, Items.KNOWLEDGE_BOOK);
            specificItems.forEach(item -> event.modifyMatching(item::equals, SET_MAX_STACK_64));

            // 4. 稀有唱片
            event.modifyMatching(item -> item.getDefaultInstance().has(DataComponents.JUKEBOX_PLAYABLE) && item.getDefaultInstance().getRarity() == Rarity.RARE, SET_MAX_STACK_64);
        }
    }

    // =================================================================
    //                      2. 玩家相关事件 (登录、进食、死亡)
    // =================================================================

    @EventBusSubscriber(modid = MODID)
    public static class PlayerLifecycleEvents {

        /**
         * 玩家首次登录：发放初始装备并传送到村庄附近
         */
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity().level().isClientSide()) return;

            Player player = event.getEntity();
            ServerLevel level = (ServerLevel) player.level();
            CompoundTag forgeData = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);

            if (!forgeData.getBoolean("hasLoggedInBefore")) {
                player.sendSystemMessage(Component.literal("§eWelcome to this world! §aA starter gift has been placed in your inventory."));
                givePlayerStarterGear(player);
                teleportToNearestVillage(player, level);

                forgeData.putBoolean("hasLoggedInBefore", true);
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, forgeData);
            }
        }

        /**
         * 玩家死亡/维度切换时的数据复制
         */
        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            if (event.isWasDeath()) {
                IEatenFoods oldCap = event.getOriginal().getCapability(ModCapabilities.EATEN_FOODS_CAPABILITY);
                IEatenFoods newCap = event.getEntity().getCapability(ModCapabilities.EATEN_FOODS_CAPABILITY);

                if (oldCap != null && newCap != null) {
                    newCap.copyFrom(oldCap);
                }
            }
        }

        /**
         * 玩家进食：首次食用某种食物给予经验奖励
         */
        @SubscribeEvent
        public static void onPlayerFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
            if (event.getEntity() instanceof Player player && event.getItem().has(DataComponents.FOOD)) {
                IEatenFoods cap = player.getCapability(ModCapabilities.EATEN_FOODS_CAPABILITY);

                if (cap != null) {
                    ItemStack currentFood = event.getItem();
                    // 如果是新食物，给予奖励
                    if (!cap.hasEaten(currentFood)) {
                        if (!player.level().isClientSide()) {
                            // 计算经验值：基础100 + 难度缩放
                            int xpValue = 100;
                            PlayerDifficulty capPD = (PlayerDifficulty)((PlayerCapabilityHolder)LHMiscs.PLAYER.type()).getOrCreate(player);
                            int bonusXp = (int)(xpValue * (1 + 0.01 * capPD.getLevel(player).getLevel()));

                            net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(
                                    player.level(), player.getX(), player.getY() + 0.5, player.getZ(), bonusXp
                            );
                            player.level().addFreshEntity(orb);
                        }
                        // 记录已食用
                        cap.addFood(currentFood);
                    }
                }
            }
        }

        // --- 私有辅助方法 ---

        private static void givePlayerStarterGear(Player player) {
            Level level = player.level();
            // 给予全套下界合金装备（Limitless系列）
            player.getInventory().add(createStarterArmor(new ItemStack(Items.NETHERITE_HELMET), "Limitless Helmet", level));
            player.getInventory().add(createStarterArmor(new ItemStack(Items.NETHERITE_CHESTPLATE), "Limitless Chestplate", level));
            player.getInventory().add(createStarterArmor(new ItemStack(Items.NETHERITE_LEGGINGS), "Limitless Leggings", level));
            player.getInventory().add(createStarterArmor(new ItemStack(Items.NETHERITE_BOOTS), "Limitless Boots", level));

            // 给予 Maken Sword
            ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
            setEnchant(sword, level, Enchantments.SHARPNESS, 6);
            setEnchant(sword, level, Enchantments.UNBREAKING, 4);
            setEnchant(sword, level, Enchantments.MENDING, 1);
            sword.set(MAKEN_SWORD.get(), 1.0D);
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("Maken Sword"));
            setApotheosisMythicRarity(sword);
            player.getInventory().add(sword);

            // 给予恢复罗盘和末影珍珠
            ItemStack recoveryCompass = new ItemStack(Items.RECOVERY_COMPASS);
            recoveryCompass.enchant(getHolder(SOULBOUND_ENCHANTMENT, level), 6);
            player.getInventory().add(recoveryCompass);
            player.getInventory().add(new ItemStack(Items.ENDER_PEARL, 5));
        }

        private static ItemStack createStarterArmor(ItemStack armor, String name, Level level) {
            setEnchant(armor, level, Enchantments.PROTECTION, 5);
            setEnchant(armor, level, Enchantments.UNBREAKING, 4);
            setEnchant(armor, level, Enchantments.MENDING, 1);
            armor.set(MAKEN_ARMOR.get(), 1.0D);
            armor.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            setApotheosisMythicRarity(armor);
            return armor;
        }

        private static void teleportToNearestVillage(Player player, ServerLevel level) {
            BlockPos nearestVillage = level.findNearestMapStructure(StructureTags.VILLAGE, player.getOnPos(), 5000, false);
            if (nearestVillage != null) {
                int x = nearestVillage.getX();
                int z = nearestVillage.getZ();

                // 加载区块并寻找安全高度
                level.getChunk(x, z);
                int maxY = level.getMaxBuildHeight();
                int minY = level.getMinBuildHeight();
                BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(x, maxY, z);

                while (targetPos.getY() > minY && level.getBlockState(targetPos).isAir()) {
                    targetPos.move(Direction.DOWN);
                }
                player.teleportTo(x, targetPos.getY() + 1, z);
            }
        }
    }

    // =================================================================
    //                      3. 实体生成与特性事件
    // =================================================================

    @EventBusSubscriber(modid = MODID)
    public static class EntitySpawningEvents {

        /**
         * 实体加入世界：Warden特殊处理 & 随机盔甲纹饰
         */
        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)) return;

            // 监守者 (Warden) Boss化逻辑
            if (entity instanceof Warden warden) {
                handleWardenSpawn(warden);
            }

            // 为生成的实体随机添加盔甲纹饰
            addRandomArmorTrim(entity);
        }

        /**
         * L2Hostility 难度初始化后：修正 Warden Boss 的等级
         */
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

        // --- 私有辅助方法 ---

        private static void handleWardenSpawn(Warden warden) {
            // 根据难度概率生成 Boss Warden
            if (Math.random() < (double) AttributeHelper.getL2HostilityLevel(warden).getAsInt() / 12288) {
                if (warden.getMainHandItem().has(WAEDREN_BOSS)) return;

                ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
                sword.set(WAEDREN_BOSS, 7); // 设置7条命
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
                    // 如果没有纹饰，尝试添加
                    if (!equippedItem.has(DataComponents.TRIM)) {
                        Holder.Reference<TrimPattern> randomPattern = allPatterns.get(RANDOM.nextInt(allPatterns.size()));

                        // 筛选可用的材料（排除作为修复材料的物品，避免冲突）
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
                    // 如果没有染色，尝试随机染色
                    if (!equippedItem.has(DataComponents.DYED_COLOR)) {
                        equippedItem.set(DataComponents.DYED_COLOR, new DyedItemColor((int)(Math.random() * (1 << 24)), false));
                    }
                }
            }
        }

        private static List<Holder.Reference<TrimMaterial>> getPotentialMaterials(Holder.Reference<TrimPattern> pattern, List<Holder.Reference<TrimMaterial>> allMaterials) {
            ResourceLocation flowTrim = withDefaultNamespace("flow");
            ResourceLocation boltTrim = withDefaultNamespace("bolt");

            // 特殊纹饰图案只能使用特殊材料
            if (pattern.key().location().equals(flowTrim) || pattern.key().location().equals(boltTrim)) {
                return allMaterials.stream()
                        .filter(material -> SPECIAL_TRIM_MATERIALS.contains(material.key().location()))
                        .toList();
            }
            return allMaterials;
        }
    }

    // =================================================================
    //                      4. 战斗与伤害事件
    // =================================================================

    @EventBusSubscriber(modid = MODID)
    public static class CombatEvents {

        /**
         * 伤害预处理：
         * 1. 转换原版伤害类型为 Apothic Attributes 魔法伤害类型
         */
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

        /**
         * 攻击事件：Maken Sword 吸血与保护机制
         */
        @SubscribeEvent
        public static void onDamageWithMakenSword(LivingDamageEvent.Pre event) {
            // 攻击者逻辑：持有 Maken Sword 且有 Power 效果 -> 获得伤害护盾 (Absorption)
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

            // 受害者逻辑：玩家持有 Maken Sword 且血量过低 -> 触发保命机制
            if (event.getEntity() instanceof ServerPlayer player) {
                ItemStack weapon = player.getMainHandItem();
                if (weapon.has(MAKEN_SWORD.get())) {
                    if (player.getHealth() <= (player.getMaxHealth() * 0.1f) && !player.getCooldowns().isOnCooldown(weapon.getItem())) {
                        event.setNewDamage(1); // 锁血
                        player.heal(player.getMaxHealth() * 0.3f);
                        player.addEffect(new MobEffectInstance(MAKEN_POWER, 400, 0));
                        player.getCooldowns().addCooldown(weapon.getItem(), 3600); // 3分钟冷却
                        player.sendSystemMessage(Component.literal("§cYour Maken Sword awakens to protect you!"));
                    }
                }
            }
        }

        /**
         * Warden Boss 多命机制：当且仅当持有 WAEDREN_BOSS 物品时
         */
        @SubscribeEvent
        public static void onWardenHurtForLastStand(LivingDamageEvent.Pre event) {
            if (event.getEntity() instanceof Warden warden && event.getEntity().level() instanceof ServerLevel serverLevel) {
                ItemStack mainHand = warden.getMainHandItem();
                if (mainHand.has(WAEDREN_BOSS)) {
                    int livesLeft = mainHand.get(WAEDREN_BOSS);
                    // 致命伤害判定
                    if (livesLeft > 0 && (warden.getHealth() - event.getOriginalDamage()) <= 0) {
                        event.setNewDamage(0); // 抵消伤害
                        mainHand.set(WAEDREN_BOSS, livesLeft - 1); // 扣除命数

                        // 重置状态
                        warden.setHealth(warden.getMaxHealth());
                        warden.removeAllEffects();

                        // 特效与声音
                        serverLevel.playSound(null, warden.blockPosition(), SoundEvents.WARDEN_ROAR, warden.getSoundSource(), 2.0F, 0.5F);
                        serverLevel.playSound(null, warden.blockPosition(), SoundEvents.WITHER_SPAWN, warden.getSoundSource(), 1.0F, 1.0F);
                        warden.setPose(Pose.ROARING);

                        // L2Hostility 重新初始化特性
                        ((GeneralCapabilityHolder) LHMiscs.MOB.type()).getExisting(warden).ifPresent(capObject -> {
                            MobTraitCap cap = (MobTraitCap) capObject;
                            cap.reinit(warden, cap.lv, true);
                        });
                    }
                }
            }
        }

        /**
         * 生物死亡事件处理
         */
        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            LivingEntity deadEntity = event.getEntity();
            DamageSource source = event.getSource();

            // 1. 玩家持 Maken Sword 死亡 -> 自爆
            if (deadEntity instanceof Player player) {
                player.sendSystemMessage(Component.literal("Death Position: " + player.position().toString()));
                handlePlayerDeathWithMakenSword(player);
            }

            // 2. 漂浮的骷髅死亡 -> 掉落卷轴
            if (deadEntity instanceof Skeleton && deadEntity.hasEffect(LEVITATION) && Math.abs(Math.abs(deadEntity.getXRot()) - 180) < 45) {
                handleLevitatingSkeletonDeath(deadEntity);
            }

            // 3. 玩家击杀目标
            if (source.getEntity() instanceof Player player) {
                handlePlayerKill(player, deadEntity);
            }
        }

        // --- 私有辅助方法 ---

        private static void replaceDamageSource(LivingDamageEvent.Pre event, RegistryAccess registry, ResourceKey<net.minecraft.world.damagesource.DamageType> newTypeKey) {
            event.setNewDamage(0); // 阻止原伤害
            DamageSource newSource = new DamageSource(
                    registry.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(newTypeKey),
                    event.getSource().getDirectEntity(),
                    event.getSource().getEntity()
            );
            event.getEntity().hurt(newSource, event.getOriginalDamage()); // 应用新伤害
        }

        private static void handlePlayerDeathWithMakenSword(Player player) {
            if (player.getMainHandItem().has(MAKEN_SWORD.get()) && !player.level().isClientSide) {
                double bonusValue = player.getMainHandItem().getOrDefault(MAKEN_SWORD.get(), 0.0);
                float explosionRadius = (float) (player.getMaxHealth() + bonusValue);
                // 死亡时产生爆炸
                Explosion explosion = new Explosion(player.level(), player, player.getX(), player.getY(), player.getZ(), explosionRadius, true, Explosion.BlockInteraction.DESTROY);
                explosion.explode();
                explosion.finalizeExplosion(true);
            }
        }

        private static void handleLevitatingSkeletonDeath(LivingEntity skeleton) {
            if (skeleton.level().isClientSide) return;
            // 创建法术卷轴 (Firebolt)
            SpellSlot spellSlot = SpellSlot.of(new SpellData(SpellRegistry.FIREBOLT_SPELL.get(), 0), 0);
            SpellContainer spellContainer = new SpellContainer(1, false, false, true, new SpellSlot[]{spellSlot});
            ItemStack scroll = new ItemStack(SCROLL.get());
            scroll.set(SPELL_CONTAINER, spellContainer);

            ItemEntity itemEntity = new ItemEntity(skeleton.level(), skeleton.getX(), skeleton.getY(), skeleton.getZ(), scroll);
            skeleton.level().addFreshEntity(itemEntity);
        }

        private static void handlePlayerKill(Player player, LivingEntity killedEntity) {
            // Maken Power 击杀刷新
            if (player.hasEffect(MAKEN_POWER)) {
                MakenPowerEffect.onKill(player);
            }

            // 傀儡掉落
            if (killedEntity instanceof AbstractGolemEntity golem) {
                Level level = golem.level();
                if (!level.isClientSide) {
                    level.addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level, golem.getX(), golem.getY(), golem.getZ(), 5));
                    level.addFreshEntity(new ItemEntity(level, golem.getX(), golem.getY(), golem.getZ(), new ItemStack(Items.IRON_INGOT, 5)));
                }
            }

            // Maken 装备成长逻辑
            if (player.getMainHandItem().has(MAKEN_SWORD.get())) {
                handleMakenGrowth(player, killedEntity);
            }
        }

        /**
         * Maken 装备核心成长逻辑
         */
        private static void handleMakenGrowth(Player player, LivingEntity killedEntity) {
            float entityHP = killedEntity.getMaxHealth();
            float playerHP = player.getMaxHealth();

            // 血量相近时不成长
            if (Math.abs(entityHP - playerHP) <= 1f) return;

            if (entityHP < playerHP) {
                // 虐菜：剑攻击力成长
                if (killedEntity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                    double playerDmg = Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
                    // 收益公式：0.5 * (怪物攻击/玩家攻击)
                    double damageToAdd = 0.5 * AttributeHelper.getAttackDamageValue(killedEntity) / playerDmg;
                    addOrUpdateAttribute(player.getMainHandItem(), MAKEN_SWORD, Attributes.ATTACK_DAMAGE, damageToAdd, EquipmentSlotGroup.MAINHAND, AttributeModifier.Operation.ADD_VALUE, ATTACK_DAMAGE_GROWTH_ID);
                }
            } else {
                // 挑战强敌：防具生命值成长
                float totalHealthBonus = 0.125f * (entityHP - playerHP);
                distributeHealthBonus(player, totalHealthBonus);
            }
        }

        private static void distributeHealthBonus(Player player, float totalHealthBonus) {
            // 分配权重：头5, 胸8, 腿7, 脚4
            Map<EquipmentSlot, Integer> ratios = Map.of(
                    EquipmentSlot.HEAD, 5, EquipmentSlot.CHEST, 8,
                    EquipmentSlot.LEGS, 7, EquipmentSlot.FEET, 4
            );
            Map<EquipmentSlot, ItemStack> equippedMakenArmor = new EnumMap<>(EquipmentSlot.class);
            int totalWeight = 0;

            // 筛选已装备的 Maken Armor
            for (var entry : ratios.entrySet()) {
                ItemStack armor = player.getItemBySlot(entry.getKey());
                if (!armor.isEmpty() && armor.has(MAKEN_ARMOR.get())) {
                    equippedMakenArmor.put(entry.getKey(), armor);
                    totalWeight += entry.getValue();
                }
            }

            // 按权重分配属性
            if (totalWeight > 0) {
                final float finalTotalWeight = totalWeight;
                equippedMakenArmor.forEach((slot, stack) -> {
                    float bonus = totalHealthBonus * (ratios.get(slot) / finalTotalWeight);
                    ResourceLocation id = HEALTH_GROWTH_IDS.get(slot);
                    if (id != null) {
                        addOrUpdateAttribute(stack, MAKEN_ARMOR, Attributes.MAX_HEALTH, bonus, EquipmentSlotGroup.bySlot(slot), AttributeModifier.Operation.ADD_VALUE, id);
                    }
                });
            }
        }
    }

    // =================================================================
    //                      5. 方块与环境交互事件
    // =================================================================

    @EventBusSubscriber(modid = MODID)
    public static class InteractionEvents {

        /**
         * 放置基岩彩蛋：获得 Giant Sword
         */
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

        /**
         * 铁砧砸落事件：用于 Mowzie's Mobs 物品与 Maken 物品的合成转换
         */
        @SubscribeEvent
        public static void onAnvilLand(AnvilLandEvent event) {
            if (event.getLevel().isClientSide()) return;

            BlockPos pos = event.getPos();
            List<ItemEntity> availableItems = new ArrayList<>(event.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0)));

            // 循环尝试合成直到无法继续
            boolean crafted;
            do {
                crafted = false;
                // 尝试合成：斧头 -> 剑
                if (tryVectorCraft(availableItems, item -> item.is(WROUGHT_AXE.get()), item -> item.has(MAKEN_SWORD.get()))) {
                    crafted = true;
                }
                // 尝试合成：头盔 -> 护甲
                else if (tryVectorCraft(availableItems, item -> item.is(WROUGHT_HELMET.get()), item -> item.has(MAKEN_ARMOR.get()))) {
                    crafted = true;
                }
            } while (crafted);
        }

        private static boolean tryVectorCraft(List<ItemEntity> availableItems, Predicate<ItemStack> toolMatcher, Predicate<ItemStack> targetMatcher) {
            Optional<ItemEntity> toolEntityOpt = availableItems.stream().filter(e -> toolMatcher.test(e.getItem())).findFirst();
            Optional<ItemEntity> targetEntityOpt = availableItems.stream().filter(e -> targetMatcher.test(e.getItem())).findFirst();

            if (toolEntityOpt.isPresent() && targetEntityOpt.isPresent()) {
                ItemEntity toolEntity = toolEntityOpt.get();
                ItemEntity targetEntity = targetEntityOpt.get();

                // 转换逻辑：设置不可破坏，消耗工具
                targetEntity.getItem().set(DataComponents.UNBREAKABLE, new Unbreakable(true));
                toolEntity.getItem().shrink(1);
                if (toolEntity.getItem().isEmpty()) {
                    toolEntity.discard();
                }

                availableItems.remove(toolEntity);
                availableItems.remove(targetEntity); // 移除已处理目标防止重复
                return true;
            }
            return false;
        }

        /**
         * 修改村民交易
         */
        @SubscribeEvent
        public static void modifyVillagerTrades(VillagerTradesEvent event) {
            // 牧羊人 (SHEPHERD)
            if (event.getType() == VillagerProfession.SHEPHERD) {
                // Level 1: 用绿宝石买钻石 (福利交易)
                event.getTrades().get(1).add(new VillagerTrades.ItemsForEmeralds(Items.DIAMOND, 5, 1, 12, 2));
                // Level 2: 用石头换绿宝石
                event.getTrades().get(2).add(new VillagerTrades.EmeraldForItems(Blocks.STONE, 32, 16, 10));
            }
        }
    }

    // =================================================================
    //                      6. 法术系统集成 (Iron's Spells)
    // =================================================================

    @EventBusSubscriber(modid = MODID)
    public static class SpellSystemEvents {

        /**
         * 盾牌格挡触发反击闪电链
         */
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
                // 设置法术目标为攻击者
                magicData.setAdditionalCastData(new TargetEntityCastData(actualAttacker));

                if (spell.checkPreCastConditions(event.getEntity().level(), spellLevel, event.getEntity(), magicData)) {
                    spell.onCast(event.getEntity().level(), spellLevel, event.getEntity(), CastSource.SWORD, magicData);
                }
            }
        }

        /**
         * 法术施放：记录熟练度 & 减半魔力消耗
         */
        @SubscribeEvent
        public static void onSpellCast(SpellOnCastEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                CompoundTag persistentData = player.getPersistentData();
                CompoundTag forgeData = persistentData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
                CompoundTag spellCounts = forgeData.getCompound(SPELL_CAST_COUNT_TAG);

                // 增加该法术的施放计数
                int newCount = spellCounts.getInt(event.getSpellId()) + 1;
                spellCounts.putInt(event.getSpellId(), newCount);

                forgeData.put(SPELL_CAST_COUNT_TAG, spellCounts);
                persistentData.put(ServerPlayer.PERSISTED_NBT_TAG, forgeData);
            }
            // 减半魔力消耗
            event.setManaCost(event.getManaCost() / 2);
        }

        /**
         * 法术伤害：根据熟练度增加暴击率
         */
        @SubscribeEvent
        public static void onSpellDamage(SpellDamageEvent event) {
            LivingEntity attacker = (LivingEntity) event.getSpellDamageSource().getEntity();
            if (attacker != null && !event.getSpellDamageSource().is(ALObjects.Tags.CANNOT_CRITICALLY_STRIKE)) {

                int spellCastCount = getProficiencyForSpell(attacker, event.getSpellDamageSource().spell().getSpellId());
                double castCountBonus = calculateSCurveBonus(spellCastCount, 2.0, 100, 0.01);

                // 暴击几率 = 基础暴击 * (1 + 熟练度加成)
                double critChance = attacker.getAttributeValue(ALObjects.Attributes.CRIT_CHANCE) * (1 + castCountBonus);
                float critDmgMultiplier = (float) attacker.getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE);

                RandomSource rand = attacker.getRandom();
                float totalDamage = event.getAmount();
                float baseDamage = event.getAmount();

                // 处理超过100%的暴击几率（多重暴击）
                boolean didCrit = false;
                while (rand.nextFloat() < critChance) {
                    didCrit = true;
                    critChance -= 1.0;
                    totalDamage += baseDamage * (critDmgMultiplier - 1.0F);
                    critDmgMultiplier *= 0.85F; // 后续暴击收益递减
                }

                if (didCrit && !attacker.level().isClientSide) {
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) attacker.level(), event.getEntity().chunkPosition(), new CritParticlePayload(event.getEntity().getId()));
                }
                event.setAmount(totalDamage);
            }
        }

        /**
         * 法术治疗：根据学派熟练度增加治疗量
         */
        @SubscribeEvent
        public static void onSpellHeal(SpellHealEvent event) {
            if (event.getEntity() instanceof LivingEntity caster) {
                int schoolProficiency = getProficiencyForSchool(caster, event.getSchoolType());
                float proficiencyBonus = (float) (1.0F + calculateSCurveBonus(schoolProficiency, 2.0, 100, 0.01));
                ((SpellHealEventAccessor) event).setHealAmount(event.getHealAmount() * proficiencyBonus);
            }
        }

        /**
         * 修改法术等级：整合装备（手持、护甲、饰品）提供的加成
         */
        @SubscribeEvent
        public static void onModifySpellLevel(ModifySpellLevelEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity == null) return;

            ResourceLocation spellId = event.getSpell().getSpellResource();
            SchoolType schoolType = event.getSpell().getSchoolType();
            int totalBonus = 0;

            // 1. 装备栏
            totalBonus += getBonusFromStack(entity.getMainHandItem(), spellId, schoolType);
            totalBonus += getBonusFromStack(entity.getOffhandItem(), spellId, schoolType);
            for (ItemStack armorStack : entity.getArmorSlots()) {
                totalBonus += getBonusFromStack(armorStack, spellId, schoolType);
            }

            // 2. Curios 饰品栏
            totalBonus += getBonusFromCurios(entity, spellId, schoolType);

            if (totalBonus > 0) {
                event.setLevel(event.getLevel() + totalBonus);
            }
        }

        // --- 私有辅助方法 ---

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

        /**
         * S曲线加成计算：使收益在前期增长快，后期趋于平缓
         */
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
                    // 查找所有饰品
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
}