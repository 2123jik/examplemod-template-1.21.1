package com.example.examplemod.server.util;

import com.google.common.collect.ImmutableMap;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Map;
import java.util.Set;

import static com.example.examplemod.ExampleMod.loc;
import static net.minecraft.resources.ResourceLocation.withDefaultNamespace;

public class ServerEventUtils {

    // 常量定义
    public static final String SPELL_CAST_COUNT_TAG = "Examplemod_SpellCounts";
    public static final ResourceLocation ATTACK_DAMAGE_GROWTH_ID = loc("weapon_growth_bonus");

    public static final Map<EquipmentSlot, ResourceLocation> HEALTH_GROWTH_IDS = ImmutableMap.of(
            EquipmentSlot.HEAD, loc("max_health_head"),
            EquipmentSlot.CHEST, loc("max_health_chest"),
            EquipmentSlot.LEGS, loc("max_health_legs"),
            EquipmentSlot.FEET, loc("max_health_feet")
    );

    public static final Set<ResourceLocation> SPECIAL_TRIM_MATERIALS = Set.of(
            withDefaultNamespace("redstone"), withDefaultNamespace("quartz"),
            withDefaultNamespace("lapis"), withDefaultNamespace("netherite"),
            withDefaultNamespace("iron"), withDefaultNamespace("emerald"),
            withDefaultNamespace("diamond"), withDefaultNamespace("copper"),
            withDefaultNamespace("amethyst")
    );

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
    public static void setApotheosisMythicRarity(ItemStack stack) {
        LootRarity mythicRarity = RarityRegistry.INSTANCE.getValue(Apotheosis.loc("mythic"));
        if (mythicRarity != null) {
            AffixHelper.setRarity(stack, mythicRarity);
        }
    }

    /**
     * 向 ItemStack 添加或更新 AttributeModifier
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
}