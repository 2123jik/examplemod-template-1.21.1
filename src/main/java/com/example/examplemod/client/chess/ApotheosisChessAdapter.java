package com.example.examplemod.client.chess;

import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.InvaderRegistry;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.mobs.util.BossStats;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.placebo.json.RandomAttributeModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ApotheosisChessAdapter {

    private static final int[][] SHOP_PROBABILITIES = {
            {100, 0, 0, 0, 0}, {100, 0, 0, 0, 0}, {75, 25, 0, 0, 0}, {55, 30, 15, 0, 0}, {45, 33, 20, 2, 0},
            {25, 40, 30, 5, 0}, {19, 30, 35, 15, 1}, {16, 20, 35, 25, 4}, {9, 15, 30, 30, 16}, {5, 10, 20, 40, 25}
    };

    public static LocalChessGame.UnitDefinition createRandomUnit(Player player, int level) {
        LocalChessGame.ShopCard[] shop = refreshShop(player, level);
        for (LocalChessGame.ShopCard card : shop) {
            if (card != null && card.def().sourceInvader() != null) {
                return card.def();
            }
        }
        return shop[0] != null ? shop[0].def() : createFallbackCard(1).def();
    }

    public static LocalChessGame.ShopCard[] refreshShop(Player player, int playerLevel) {
        LocalChessGame.ShopCard[] shop = new LocalChessGame.ShopCard[5];
        RandomSource rand = player.level().getRandom();
        WorldTier tier = mapLevelToTier(playerLevel);

        GenContext ctx = new GenContext(rand, tier, player.getLuck(), player.level().dimension(), player.level().getBiome(player.blockPosition()), Collections.emptySet());

        for (int i = 0; i < 5; i++) {
            int targetCost = rollCost(rand, playerLevel);
            LocalChessGame.ShopCard card = trySpawnUnit(ctx, player, targetCost);

            if (card == null) {
                card = trySpawnAnyUnit(ctx, player);
            }

            if (card == null) {
                shop[i] = createFallbackCard(targetCost);
            } else {
                shop[i] = card;
            }
        }
        return shop;
    }

    private static LocalChessGame.ShopCard trySpawnUnit(GenContext ctx, Player player, int targetCost) {
        List<LootRarity> targetRarities = getRaritiesByCost(targetCost);
        if (targetRarities.isEmpty()) return null;

        Predicate<Invader> filter = invader -> {
            for (LootRarity r : targetRarities) if (invader.stats().containsKey(r)) return true;
            return false;
        };

        if (player.level() instanceof ServerLevel) {
            filter = filter.and(inv -> inv.constraints().test(ctx));
        }

        Invader pickedInvader = null;
        try {
            pickedInvader = InvaderRegistry.INSTANCE.getRandomItem(ctx, filter);
        } catch (Exception ignored) {}

        if (pickedInvader == null) {
            List<Invader> fallbackList = InvaderRegistry.INSTANCE.getValues().stream().filter(filter).toList();
            if (!fallbackList.isEmpty()) pickedInvader = fallbackList.get(0);
        }

        if (pickedInvader != null) {
            LootRarity pickedRarity = pickBestRarityForCost(pickedInvader, targetRarities);
            if (pickedRarity != null) {
                return buildCard(player, pickedInvader, pickedRarity, targetCost);
            }
        }
        return null;
    }

    private static LocalChessGame.ShopCard trySpawnAnyUnit(GenContext ctx, Player player) {
        Predicate<Invader> filter = invader -> true;
        if (player.level() instanceof ServerLevel) {
            filter = filter.and(inv -> inv.constraints().test(ctx));
        }

        Invader pickedInvader = null;
        try {
            pickedInvader = InvaderRegistry.INSTANCE.getRandomItem(ctx, filter);
        } catch (Exception ignored) {}

        if (pickedInvader == null) {
            List<Invader> all = InvaderRegistry.INSTANCE.getValues().stream().filter(filter).toList();
            if (!all.isEmpty()) pickedInvader = all.get(ctx.rand().nextInt(all.size()));
        }

        if (pickedInvader != null) {
            LootRarity anyRarity = pickedInvader.stats().keySet().stream().findFirst().orElse(null);
            if (anyRarity != null) {
                int realCost = getCostForRarity(anyRarity);
                return buildCard(player, pickedInvader, anyRarity, realCost);
            }
        }
        return null;
    }

    private static LocalChessGame.ShopCard buildCard(Player player, Invader invader, LootRarity rarity, int cost) {
        LocalChessGame.UnitDefinition def;
        if (player.level() instanceof ServerLevel serverLevel) {
            Mob previewEntity = invader.createBoss(serverLevel, BlockPos.ZERO, new GenContext(serverLevel.getRandom(), WorldTier.HAVEN, 0f, serverLevel.dimension(), serverLevel.getBiome(player.blockPosition()), Collections.emptySet()), rarity);
            def = createDefinitionFromEntity(previewEntity, invader, rarity, cost);
        } else {
            def = createDefinitionFromStats(invader, rarity, cost);
        }
        return new LocalChessGame.ShopCard(def, UUID.randomUUID());
    }

    private static LocalChessGame.UnitDefinition createDefinitionFromStats(Invader invader, LootRarity rarity, int cost) {
        EntityType<?> type = invader.entity();
        BossStats stats = invader.stats().get(rarity);

        String name = invader.basicData().name().getString();
        if (name.isEmpty() || name.equals("use_name_generation")) name = type.getDescription().getString();

        int hp = calculateStat(type, stats.modifiers(), Attributes.MAX_HEALTH);
        int dmg = calculateStat(type, stats.modifiers(), Attributes.ATTACK_DAMAGE);

        String trait = "生物/战士";
        String typeId = EntityType.getKey(type).getPath();
        if(typeId.contains("skeleton") || typeId.contains("blaze")) trait = "怪物/射手";
        if (cost == 5) trait += "/神性";

        return new LocalChessGame.UnitDefinition(
                type, cost, name, trait, hp, dmg,
                typeId.contains("skeleton") ? 4.0f : 1.0f,
                invader, rarity
        );
    }

    private static int calculateStat(EntityType<?> type, List<RandomAttributeModifier> modifiers, Holder<Attribute> targetAttr) {
        double baseValue = 0.0;
        if (DefaultAttributes.hasSupplier(type)) {
            try {
                var supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
                if (supplier.hasAttribute(targetAttr)) {
                    baseValue = supplier.getBaseValue(targetAttr);
                }
            } catch (Exception ignored) {}
        }
        
        if (baseValue <= 0.0 && targetAttr == Attributes.ATTACK_DAMAGE) baseValue = 2.0;

        double val = baseValue;
        double addBase = 0.0, mulBase = 0.0, mulTotal = 0.0;

        for (RandomAttributeModifier mod : modifiers) {
            if (mod.attribute().value() == targetAttr.value()) {
                float value = mod.value().min();
                AttributeModifier.Operation op = mod.operation();
                if (op == AttributeModifier.Operation.ADD_VALUE) addBase += value;
                else if (op == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) mulBase += value;
                else if (op == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) mulTotal += value;
            }
        }
        return (int) ((val + addBase) * (1.0 + mulBase) * (1.0 + mulTotal));
    }

    private static LocalChessGame.ShopCard createFallbackCard(int cost) {
        return new LocalChessGame.ShopCard(
                new LocalChessGame.UnitDefinition(EntityType.CREEPER, cost, "Fallback Creeper", "Glitch", 100 * cost, 10 * cost, 1f, null, null),
                UUID.randomUUID()
        );
    }

    public static int getRarityColor(int cost) {
        return switch (cost) {
            case 1 -> 0xFFAAAAAA; case 2 -> 0xFF55FF55; case 3 -> 0xFF5555FF;
            case 4 -> 0xFFAA00AA; case 5 -> 0xFFFFAA00; default -> 0xFFFFFFFF;
        };
    }

    private static int getCostForRarity(LootRarity rarity) {
        List<LootRarity> sorted = RarityRegistry.getSortedRarities();
        int index = sorted.indexOf(rarity);
        if (index == -1) return 1;
        double percent = (double) index / sorted.size();
        if (percent < 0.2) return 1;
        if (percent < 0.4) return 2;
        if (percent < 0.6) return 3;
        if (percent < 0.8) return 4;
        return 5;
    }

    private static List<LootRarity> getRaritiesByCost(int cost) {
        return RarityRegistry.getSortedRarities().stream().filter(r -> getCostForRarity(r) == cost).collect(Collectors.toList());
    }

    private static LootRarity pickBestRarityForCost(Invader invader, List<LootRarity> allowedRarities) {
        for (LootRarity r : allowedRarities) if (invader.stats().containsKey(r)) return r;
        return invader.stats().keySet().stream().findFirst().orElse(null);
    }

    private static int rollCost(RandomSource rand, int level) {
        int[] probs = SHOP_PROBABILITIES[Math.min(level - 1, 9)];
        int roll = rand.nextInt(100);
        int current = 0;
        for (int c = 0; c < 5; c++) {
            current += probs[c];
            if (roll < current) return c + 1;
        }
        return 1;
    }

    private static WorldTier mapLevelToTier(int level) {
        if (level <= 2) return WorldTier.HAVEN;
        if (level <= 4) return WorldTier.FRONTIER;
        if (level <= 6) return WorldTier.ASCENT;
        if (level <= 8) return WorldTier.SUMMIT;
        return WorldTier.PINNACLE;
    }

    private static LocalChessGame.UnitDefinition createDefinitionFromEntity(Mob entity, Invader invader, LootRarity rarity, int cost) {
        int hp = (int) entity.getMaxHealth();
        int dmg = (int) entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
        String trait = determineTrait(entity, rarity);
        return new LocalChessGame.UnitDefinition(
                invader.entity(), cost, entity.getDisplayName().getString(), trait, hp, dmg,
                entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? 1.0f : 4.0f,
                invader, rarity
        );
    }

    private static String determineTrait(Mob mob, LootRarity rarity) {
        StringBuilder sb = new StringBuilder();
        if (mob.getType().getCategory().getName().equals("monster")) sb.append("怪物"); else sb.append("生物");
        sb.append("/");
        var item = mob.getMainHandItem().getItem();
        if (item instanceof net.minecraft.world.item.SwordItem) sb.append("战士");
        else if (item instanceof net.minecraft.world.item.BowItem) sb.append("射手");
        else if (item instanceof net.minecraft.world.item.AxeItem) sb.append("狂战士");
        else sb.append("辅助");
        if (getCostForRarity(rarity) == 5) sb.append("/神性");
        return sb.toString();
    }
}