package com.example.examplemod.server.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.examplemod.ExampleMod.MODID;

@EventBusSubscriber(modid = MODID)
public class LootEventHandler {

    private static final List<Item> FOOD_CACHE = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static final double BASE_DROP_CHANCE = 0.31;
    private static final double COMPLEXITY_PENALTY = 0.5;

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Enemy)) return;

        if (FOOD_CACHE.isEmpty()) {
            initFoodCache();
        }

        if (FOOD_CACHE.isEmpty()) return;
        Item randomFood = FOOD_CACHE.get(RANDOM.nextInt(FOOD_CACHE.size()));
        ItemStack foodStack = randomFood.getDefaultInstance();

        int componentCount = foodStack.getComponents().size();
        double finalChance = BASE_DROP_CHANCE / (1.0 + (componentCount * COMPLEXITY_PENALTY));

        if (RANDOM.nextDouble() < finalChance) {
            ItemEntity drop = new ItemEntity(
                    event.getEntity().level(),
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    foodStack
            );
            drop.setPickUpDelay(10);
            event.getDrops().add(drop);
        }
    }

    private static void initFoodCache() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.getDefaultInstance().has(DataComponents.FOOD)) {
                FOOD_CACHE.add(item);
            }
        }
    }
}