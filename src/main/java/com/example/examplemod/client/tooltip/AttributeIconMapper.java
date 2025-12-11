package com.example.examplemod.client.tooltip;

import com.gametechbc.gtbcs_geomancy_plus.api.init.GGAttributes;
import com.gametechbc.gtbcs_geomancy_plus.init.GGItems;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.warphan.iss_magicfromtheeast.registries.MFTEAttributeRegistries;
import net.warphan.iss_magicfromtheeast.registries.MFTEItemRegistries;

import java.util.HashMap;
import java.util.Map;

public class AttributeIconMapper {
    
    private static final Map<Holder<Attribute>, ItemStack> ICON_MAP = new HashMap<>();
    // 在这里配置你的属性对应什么物品图标
    static {
        register(AttributeRegistry.FIRE_SPELL_POWER, ItemRegistry.FIRE_UPGRADE_ORB.get());
        register(AttributeRegistry.ICE_SPELL_POWER, ItemRegistry.ICE_UPGRADE_ORB.get());
        register(AttributeRegistry.LIGHTNING_SPELL_POWER,ItemRegistry.LIGHTNING_UPGRADE_ORB.get());
        register(AttributeRegistry.HOLY_SPELL_POWER, ItemRegistry.HOLY_UPGRADE_ORB.get());
        register(AttributeRegistry.ENDER_SPELL_POWER, ItemRegistry.ENDER_UPGRADE_ORB.get());
        register(AttributeRegistry.BLOOD_SPELL_POWER, ItemRegistry.BLOOD_UPGRADE_ORB.get());
        register(AttributeRegistry.EVOCATION_SPELL_POWER, ItemRegistry.EVOCATION_UPGRADE_ORB.get());
        register(AttributeRegistry.NATURE_SPELL_POWER, ItemRegistry.NATURE_UPGRADE_ORB.get());
        register(AttributeRegistry.MAX_MANA, ItemRegistry.MANA_UPGRADE_ORB.get());
        register(AttributeRegistry.COOLDOWN_REDUCTION, ItemRegistry.COOLDOWN_UPGRADE_ORB.get());
        register(AttributeRegistry.SPELL_RESIST, ItemRegistry.PROTECTION_UPGRADE_ORB.get());
        register(GGAttributes.GEO_SPELL_POWER, GGItems.GEO_UPGRADE_ORB.get());
        register(MFTEAttributeRegistries.SPIRIT_SPELL_POWER, MFTEItemRegistries.SPIRIT_UPGRADE_ORB.get());
        register(MFTEAttributeRegistries.SYMMETRY_SPELL_POWER, MFTEItemRegistries.SYMMETRY_UPGRADE_ORB.get());

    }

    private static void register(Holder<Attribute> attribute, net.minecraft.world.item.Item item) {
        ICON_MAP.put(attribute, new ItemStack(item));
    }

    public static ItemStack getIcon(Holder<Attribute> attribute) {
        return ICON_MAP.get(attribute);
    }
}