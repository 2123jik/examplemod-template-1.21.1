package com.example.examplemod.category;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.compat.CurioEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

import static com.example.examplemod.ExampleMod.MODID;

public class SlotGroups {

    public static final DeferredHelper R = DeferredHelper.create(MODID);

    public static void register(IEventBus bus) {
        bus.register(R);
        Slots.init();
        Groups.init();
    }

    public static class Slots {
        // --- 原有 ---
        public static final Holder<EntityEquipmentSlot> SPELLBOOK = slot("spellbook");

        // --- Curios 专用槽位 ---
        public static final Holder<EntityEquipmentSlot> BELT = slot("belt");
        public static final Holder<EntityEquipmentSlot> BODY = slot("body");
        public static final Holder<EntityEquipmentSlot> BRACELET = slot("bracelet");
        public static final Holder<EntityEquipmentSlot> HEAD = slot("head");
        public static final Holder<EntityEquipmentSlot> NECKLACE = slot("necklace");
        public static final Holder<EntityEquipmentSlot> BACK = slot("back");
        public static final Holder<EntityEquipmentSlot> CHARM = slot("charm");
        public static final Holder<EntityEquipmentSlot> HANDS = slot("hands");
        public static final Holder<EntityEquipmentSlot> RING = slot("ring");
        public static final Holder<EntityEquipmentSlot> SHEATH = slot("sheath");
        public static final Holder<EntityEquipmentSlot> TRINKET = slot("trinket"); // 注意：有些模组用 trinket 作为通用名

        public static final Holder<EntityEquipmentSlot> ARTIFACT_NECKLACE = slot("artifact_necklace");
        public static final Holder<EntityEquipmentSlot> ARTIFACT_HEAD = slot("artifact_head");
        public static final Holder<EntityEquipmentSlot> ARTIFACT_BODY = slot("artifact_body");
        public static final Holder<EntityEquipmentSlot> ARTIFACT_BRACELET = slot("artifact_bracelet");
        public static final Holder<EntityEquipmentSlot> ARTIFACT_BELT = slot("artifact_belt");


        private static Holder<EntityEquipmentSlot> slot(String name) {
            return R.customDH(name, ALObjects.BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key(), () -> new CurioEquipmentSlot(name));
        }

        public static void init() {}
    }

    public static class Groups {
        // --- 原有 ---
        public static final EntitySlotGroup SPELLBOOK = group("spellbook", HolderSet.direct(Slots.SPELLBOOK));

        public static final EntitySlotGroup BELT = group("belt",
                HolderSet.direct(Slots.BELT, Slots.ARTIFACT_BELT)); // 兼容 artifact_belt

        public static final EntitySlotGroup BODY = group("body",
                HolderSet.direct(Slots.BODY, Slots.ARTIFACT_BODY)); // 兼容 artifact_body

        public static final EntitySlotGroup BRACELET = group("bracelet",
                HolderSet.direct(Slots.BRACELET, Slots.HANDS, Slots.ARTIFACT_BRACELET)); // 兼容 artifact_bracelet

        public static final EntitySlotGroup HEAD = group("head",
                HolderSet.direct(Slots.HEAD, Slots.ARTIFACT_HEAD)); // 兼容 artifact_head

        public static final EntitySlotGroup NECKLACE = group("necklace",
                HolderSet.direct(Slots.NECKLACE, Slots.ARTIFACT_NECKLACE)); // 兼容 artifact_necklace

        public static final EntitySlotGroup BACK = group("back",
                HolderSet.direct(Slots.BACK));

        public static final EntitySlotGroup CHARM = group("charm",
                HolderSet.direct(Slots.CHARM, Slots.NECKLACE));

        public static final EntitySlotGroup HANDS = group("hands",
                HolderSet.direct(Slots.HANDS, Slots.RING));

        public static final EntitySlotGroup RING = group("ring",
                HolderSet.direct(Slots.RING, Slots.HANDS));

        public static final EntitySlotGroup SHEATH = group("sheath",
                HolderSet.direct(Slots.SHEATH, Slots.BELT));

        public static final EntitySlotGroup TRINKET = group("trinket",
                HolderSet.direct(Slots.TRINKET));

        private static EntitySlotGroup group(String path, HolderSet<EntityEquipmentSlot> slots) {
            return R.custom(path, ALObjects.BuiltInRegs.ENTITY_SLOT_GROUP.key(), new EntitySlotGroup(loc(path), slots));
        }
        public static ResourceLocation loc(String id) {
            return ResourceLocation.fromNamespaceAndPath(MODID, id);
        }
        public static void init() {}
    }
}