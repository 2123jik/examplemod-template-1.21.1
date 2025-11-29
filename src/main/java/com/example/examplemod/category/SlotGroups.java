package com.example.examplemod.category;

import com.example.examplemod.ExampleMod;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.compat.CurioEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.neoforged.bus.api.IEventBus;

/**
 * SlotGroups 类
 * 负责注册自定义的实体装备槽位 (EntityEquipmentSlot) 和槽位组 (EntitySlotGroup)。
 * 这些槽位通常用于定义属性 (Attributes) 可以应用在哪些装备位置上。
 */
public class SlotGroups {

    // DeferredHelper 是 Placebo 库提供的注册辅助工具，类似于 NeoForge 的 DeferredRegister。
    // 用于将对象注册到指定的 Mod ID 下。
    public static final DeferredHelper R = DeferredHelper.create(ExampleMod.MODID);

    /**
     * 初始化并注册内容到事件总线。
     * 通常在模组主构造函数中调用此方法。
     *
     * @param bus Mod 的事件总线 (IEventBus)
     */
    public static void register(IEventBus bus) {
        // 将注册助手连接到事件总线
        bus.register(R);

        // 显式调用内部类的 init 方法，强制加载静态字段（即触发注册逻辑）
        Slots.init();
        Groups.init();
    }

    /**
     * 内部类：定义具体的装备槽位。
     */
    public static class Slots {
        // 定义一个名为 "spellbook" (法术书) 的槽位 Holder
        public static final Holder<EntityEquipmentSlot> SPELLBOOK = slot("spellbook");

        /**
         * 辅助方法：注册一个基于 Curios (饰品) 的装备槽位。
         *
         * @param slot 槽位的名称 ID
         * @return 注册后的 Holder 对象
         */
        private static Holder<EntityEquipmentSlot> slot(String slot) {
            // 使用 Placebo 的 customDH 方法注册自定义对象。
            // ALObjects.BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key() 指向 Apothic Attributes 的槽位注册表。
            // () -> new CurioEquipmentSlot(slot) 创建一个新的兼容 Curios 的槽位实例。
            return R.<EntityEquipmentSlot, EntityEquipmentSlot>customDH(slot, ALObjects.BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key(), () -> new CurioEquipmentSlot(slot));
        }

        // 空方法，仅用于在 SlotGroups.register 中被调用以触发类加载
        public static void init() {}
    }

    /**
     * 内部类：定义槽位组。
     * 槽位组是将一个或多个槽位集合在一起，属性修饰符通常是针对 "组" 生效的，而不是单个槽位。
     */
    public static class Groups {
        // 定义一个名为 "spellbook" 的槽位组，该组包含上面定义的 SPELLBOOK 槽位
        public static final EntitySlotGroup SPELLBOOK = group("spellbook", HolderSet.direct(Slots.SPELLBOOK));

        /**
         * 辅助方法：注册一个实体槽位组。
         *
         * @param path 组的名称 ID
         * @param slots 该组包含的槽位集合 (HolderSet)
         * @return 注册后的 EntitySlotGroup 对象
         */
        private static EntitySlotGroup group(String path, HolderSet<EntityEquipmentSlot> slots) {
            // 注册一个新的 EntitySlotGroup
            return R.custom(path, ALObjects.BuiltInRegs.ENTITY_SLOT_GROUP.key(), new EntitySlotGroup(ExampleMod.modResourceLoc(path), slots));
        }

        // 空方法，仅用于在 SlotGroups.register 中被调用以触发类加载
        public static void init() {}
    }
}