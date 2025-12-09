package com.example.examplemod.register;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.server.menu.TradeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ExampleMod.MODID);

    // 注册 MenuType
    // 注意：这里必须使用 IMenuTypeExtension.create，因为我们需要网络包支持 (extraData)
    public static final DeferredHolder<MenuType<?>, MenuType<TradeMenu>> TRADE_MENU =
            MENUS.register("trade_menu", () -> IMenuTypeExtension.create(TradeMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}