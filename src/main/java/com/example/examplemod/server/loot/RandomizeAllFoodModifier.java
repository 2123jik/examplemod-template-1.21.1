package com.example.examplemod.server.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger; // 引入日志
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RandomizeAllFoodModifier extends LootModifier {

    // 添加日志记录器
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomizeAllFoodModifier.class);

    private static final int SCORE_NUTRITION = 10;
    private static final int SCORE_EFFECT = 25;
    private static final int SCORE_COMPONENT = 10;

    private static final int SCORE_RARITY_COMMON = 0;
    private static final int SCORE_RARITY_UNCOMMON = 50;
    private static final int SCORE_RARITY_RARE = 150;
    private static final int SCORE_RARITY_EPIC = 400;

    private static final int INVERSE_BASE_CONSTANT = 5000;

    private static final Supplier<FoodWeightData> WEIGHT_DATA = Suppliers.memoize(RandomizeAllFoodModifier::buildWeightData);

    public static final MapCodec<RandomizeAllFoodModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).apply(inst, RandomizeAllFoodModifier::new)
    );

    public RandomizeAllFoodModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation tableId = context.getQueriedLootTableId();

        // 这里的检查通常没问题，但要注意有些mod箱子可能不带 "chests/"
        if (tableId == null || !tableId.getPath().contains("chests/")) {
            return generatedLoot;
        }

        FoodWeightData data;
        try {
            data = WEIGHT_DATA.get();
        } catch (Exception e) {
            // 如果获取数据极其罕见地失败了，返回原战利品，防止箱子变空
            LOGGER.error("Error retrieving food weight data", e);
            return generatedLoot;
        }

        if (data.totalWeight <= 0 || data.items.length < 2) return generatedLoot;

        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            if (stack.isEmpty()) continue;

            Item currentItem = stack.getItem();

            if (data.validFoods.contains(currentItem)) {
                try {
                    Item randomFood = selectRandomFood(context, data, currentItem);
                    if (randomFood != currentItem) {
                        generatedLoot.set(i, new ItemStack(randomFood, stack.getCount()));
                    }
                } catch (Exception e) {
                    // 捕获替换过程中的异常，确保其他物品能正常生成
                    LOGGER.error("Failed to replace food item: " + currentItem, e);
                }
            }
        }

        return generatedLoot;
    }

    private static FoodWeightData buildWeightData() {
        LOGGER.info("Building Food Randomizer Weight Data...");

        // 使用 List 临时存储，因为我们不知道有多少物品会成功通过 try-catch
        List<Item> validItemList = new ArrayList<>();
        List<Integer> weightList = new ArrayList<>();
        int totalWeight = 0;
        ReferenceOpenHashSet<Item> validFoodsSet = new ReferenceOpenHashSet<>();

        // 遍历所有物品
        for (Item item : BuiltInRegistries.ITEM) {
            // 基础检查
            if (item == null || !item.components().has(DataComponents.FOOD)) continue;

            try {
                // 将计算逻辑包裹在 try-catch 中
                int weight = calculateInverseWeight(item);

                validItemList.add(item);
                weightList.add(weight);
                totalWeight += weight;
                validFoodsSet.add(item);
            } catch (Exception e) {
                // 记录错误但不崩溃，跳过这个有问题的物品
                LOGGER.warn("Skipping invalid food item during weight calculation: " + BuiltInRegistries.ITEM.getKey(item), e);
            }
        }

        if (validItemList.isEmpty()) {
            return new FoodWeightData(new Item[0], new int[0], 0, new ReferenceOpenHashSet<>());
        }

        // 转换为数组
        Item[] itemsArray = validItemList.toArray(new Item[0]);
        int[] cumulativeWeights = new int[itemsArray.length];
        int cumulative = 0;

        for (int i = 0; i < weightList.size(); i++) {
            cumulative += weightList.get(i);
            cumulativeWeights[i] = cumulative;
        }

        LOGGER.info("Food Weight Data built. Total valid foods: " + itemsArray.length);
        return new FoodWeightData(itemsArray, cumulativeWeights, totalWeight, validFoodsSet);
    }

    private static int calculateInverseWeight(Item item) {
        int powerScore = 1;

        FoodProperties foodProps = item.components().get(DataComponents.FOOD);
        if (foodProps != null) {
            powerScore += foodProps.nutrition() * SCORE_NUTRITION;
            powerScore += (int) (foodProps.saturation() * 20) * SCORE_NUTRITION;

            // 安全检查：防止 effects 列表为 null（虽然通常不会，但防御性编程更好）
            if (foodProps.effects() != null && !foodProps.effects().isEmpty()) {
                powerScore += foodProps.effects().size() * SCORE_EFFECT;
            }
        }

        int componentCount = item.components().size();
        if (componentCount > 1) {
            powerScore += (componentCount - 1) * SCORE_COMPONENT;
        }

        // 安全获取稀有度
        Rarity rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        // 如果 rarity 为 null (极端情况)，默认为 COMMON
        if (rarity == null) rarity = Rarity.COMMON;

        powerScore += switch (rarity) {
            case EPIC -> SCORE_RARITY_EPIC;
            case RARE -> SCORE_RARITY_RARE;
            case UNCOMMON -> SCORE_RARITY_UNCOMMON;
            case COMMON -> SCORE_RARITY_COMMON;
        };

        // 确保 powerScore 不为负数
        if (powerScore < 0) powerScore = 0;

        int finalWeight = INVERSE_BASE_CONSTANT / (powerScore + 10);
        return Math.max(1, finalWeight);
    }

    private static Item selectRandomFood(LootContext context, FoodWeightData data, Item excludeItem) {
        if (data.items.length == 1) return data.items[0];

        Item selected = excludeItem;
        int attempts = 0;
        int maxAttempts = 3;

        while (selected == excludeItem && attempts < maxAttempts) {
            // 确保 totalWeight 正确，防止 nextInt 报错
            if (data.totalWeight <= 0) break;

            int randomValue = context.getRandom().nextInt(data.totalWeight);
            int index = binarySearch(data.cumulativeWeights, randomValue);

            // 防止数组越界
            if (index >= 0 && index < data.items.length) {
                selected = data.items[index];
            }
            attempts++;
        }
        return selected;
    }

    private static int binarySearch(int[] array, int value) {
        int left = 0, right = array.length - 1;
        while (left < right) {
            int mid = (left + right) >>> 1;
            if (value < array[mid]) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return left;
    }

    private record FoodWeightData(
            Item[] items,
            int[] cumulativeWeights,
            int totalWeight,
            Set<Item> validFoods
    ) {}

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}