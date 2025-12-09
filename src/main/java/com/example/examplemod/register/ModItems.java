package com.example.examplemod.register;

import com.example.examplemod.item.StarterKitItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.example.examplemod.ExampleMod.MODID;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(MODID);

    // 注册新手礼包物品
    public static final DeferredHolder<Item, StarterKitItem> STARTER_KIT = ITEMS.register("starter_kit", 
            () -> new StarterKitItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}