package com.example.examplemod.category;

import com.example.examplemod.ExampleMod;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LootCategories {

    private static final DeferredRegister<LootCategory> LOOT_CATEGORIES = DeferredRegister.create(Apoth.BuiltInRegs.LOOT_CATEGORY.key(), ExampleMod.MODID);

    // --- Curios 分类 ---
    // 逻辑设为 s -> false，表示默认不匹配任何物品。
    // 物品的归属完全由 loot_category_overrides.json 决定。
    public static final Holder<LootCategory> BELT = register("belt", SlotGroups.Groups.BELT);
    public static final Holder<LootCategory> BODY = register("body", SlotGroups.Groups.BODY);
    public static final Holder<LootCategory> BRACELET = register("bracelet", SlotGroups.Groups.BRACELET);
    public static final Holder<LootCategory> HEAD = register("head", SlotGroups.Groups.HEAD);
    public static final Holder<LootCategory> NECKLACE = register("necklace", SlotGroups.Groups.NECKLACE);
    public static final Holder<LootCategory> BACK = register("back", SlotGroups.Groups.BACK);
    public static final Holder<LootCategory> CHARM = register("charm", SlotGroups.Groups.CHARM);
    public static final Holder<LootCategory> HANDS = register("hands", SlotGroups.Groups.HANDS);
    public static final Holder<LootCategory> RING = register("ring", SlotGroups.Groups.RING);
    public static final Holder<LootCategory> SHEATH = register("sheath", SlotGroups.Groups.SHEATH);
    public static final Holder<LootCategory> TRINKET = register("trinket", SlotGroups.Groups.TRINKET);

    // --- 其它模组兼容 ---
    public static final Holder<LootCategory> SPELLBOOK = LOOT_CATEGORIES.register("spellbook", () ->
            new LootCategory(s -> s.getItem() instanceof SpellBook, SlotGroups.Groups.SPELLBOOK, 900));

    public static final Holder<LootCategory> STAFF = LOOT_CATEGORIES.register("staff", () ->
            new LootCategory(s -> s.getItem() instanceof CastingItem, ALObjects.EquipmentSlotGroups.MAINHAND, 900));

    public static void register(IEventBus bus) {
        LOOT_CATEGORIES.register(bus);
    }

    /**
     * 极简工厂方法：创建纯占位符分类。
     * Predicate 永远返回 false，确保只有 JSON 覆写能生效。
     */
    private static Holder<LootCategory> register(String name, dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup slot) {
        return LOOT_CATEGORIES.register(name, () -> new LootCategory(
                s -> false, // 这里没有任何 Java 逻辑，只等 JSON 召唤
                slot,
                900 // 权重
        ));
    }

    // 辅助检查方法（如果代码中其他地方需要用到）
    public static boolean isStaff(ItemStack stack) {
        return LootCategory.forItem(stack) == STAFF.value();
    }
}