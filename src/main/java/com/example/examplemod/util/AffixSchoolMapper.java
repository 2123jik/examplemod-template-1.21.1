package com.example.examplemod.util;

import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工具类：派系映射器 (AffixSchoolMapper)。
 * <p>
 * 该类的主要作用是检测一个物品堆 (ItemStack) 包含哪些魔法派系 (SchoolType)。
 * 检测的依据是物品提供的属性 (Attribute)，例如 "fire_spell_power" 对应火系。
 */
public class AffixSchoolMapper {

    /**
     * 获取指定物品关联的所有魔法派系。
     * 整合了三种来源：
     * 1. 原版属性槽位 (Vanilla Attributes)
     * 2. Curios 饰品属性 (Curios Attributes)
     * 3. Iron's Spells 的升级宝珠属性 (Upgrade Orbs)
     *
     * @param stack 要检测的物品
     * @return 包含该物品涉及的所有魔法派系的集合
     */
    public static Set<SchoolType> getSpellSchoolsFromGear(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();

        // 检查原版属性 (如手持、穿戴时增加的属性)
        schools.addAll(getVanillaSlotAttributes(stack));
        // 检查 Curios 饰品属性 (如果安装了 Curios 模组)
        schools.addAll(getCurioAttributes(stack));
        // 检查 Iron's Spells 的镶嵌升级属性
        schools.addAll(getUpgradeDataAttributes(stack));
        return schools;
    }

    /**
     * 检查原版物品属性修饰符 (ItemAttributeModifiers)。
     * 适用于普通装备、武器等。
     */
    private static Set<SchoolType> getVanillaSlotAttributes(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();

        // 获取 Minecraft 1.20.5+ 的 DataComponent 格式属性
        ItemAttributeModifiers componentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> allModifierEntries = componentModifiers.modifiers();

        // 遍历所有修饰符
        for (ItemAttributeModifiers.Entry entry : allModifierEntries) {
            Holder<Attribute> attributeHolder = entry.attribute();
            if (!attributeHolder.isBound()) continue;

            Attribute attribute = attributeHolder.value();
            // 解析属性对应的派系
            SchoolType school = getSchoolFromAttribute(attribute);
            if (school != null) {
                schools.add(school);
            }
        }

        return schools;
    }

    /**
     * 检查 Iron's Spells 'n Spellbooks 的升级数据 (UpgradeData)。
     * 这个模组允许给装备镶嵌"宝珠" (Orbs)，这些宝珠会赋予属性。
     */
    private static Set<SchoolType> getUpgradeDataAttributes(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();
        // 获取 Iron's Spells 特有的数据组件
        UpgradeData upgradeData = UpgradeData.getUpgradeData(stack);

        // 遍历镶嵌的宝珠
        for (Holder<UpgradeOrbType> upgradeHolder : upgradeData.upgrades().keySet()) {
            if (!upgradeHolder.isBound()) continue;

            UpgradeOrbType upgradeOrb = upgradeHolder.value();
            Holder<Attribute> attributeHolder = upgradeOrb.attribute();

            if (!attributeHolder.isBound()) continue;

            Attribute attribute = attributeHolder.value();
            // 解析宝珠属性对应的派系 (例如镶嵌了火之宝珠 -> 火系)
            SchoolType school = getSchoolFromAttribute(attribute);
            if (school != null) {
                schools.add(school);
            }
        }
        return schools;
    }

    /**
     * 检查 Curios (饰品栏) 提供的属性。
     * 因为 Curios 物品的属性存储方式和原版装备不同，需要单独处理。
     */
    private static Set<SchoolType> getCurioAttributes(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();

        // 如果物品不是 Curios 物品，直接跳过
        if (!(stack.getItem() instanceof ICurioItem curio)) {
            return schools;
        }

        // 获取该物品适用的饰品槽位类型
        var slotTypes = CuriosApi.getItemStackSlots(stack, false);

        // 遍历所有适用的槽位
        for (String slotId : slotTypes.keySet()) {
            // 创建一个虚拟的 SlotContext 用于获取属性
            SlotContext context = new SlotContext(slotId, null, -1, false, true);
            // 这里使用了一个自定义的 UUID/ID 来获取属性映射，主要是为了触发 getAttributeModifiers
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("examplemod", "school_check");
            Multimap<Holder<Attribute>, AttributeModifier> attributes = curio.getAttributeModifiers(context, modifierId, stack);

            // 遍历属性
            for (Holder<Attribute> attributeHolder : attributes.keySet()) {
                if (!attributeHolder.isBound()) continue;

                Attribute attribute = attributeHolder.value();
                SchoolType school = getSchoolFromAttribute(attribute);
                if (school != null) {
                    schools.add(school);
                }
            }
        }

        return schools;
    }

    /**
     * 核心逻辑：将属性 (Attribute) 转换为魔法派系 (SchoolType)。
     *
     * 逻辑依据：
     * Iron's Spells 的属性命名通常遵循 "{school_name}_spell_power" 的格式。
     * 例如: "irons_spellbooks:fire_spell_power" -> 派系 "fire"。
     */
    private static SchoolType getSchoolFromAttribute(Attribute attribute) {
        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);

        // 检查属性 ID 是否以 "_spell_power" 结尾
        if (attrId != null && attrId.getPath().endsWith("_spell_power")) {
            // 去掉后缀，获取派系名称 (如 "fire")
            String schoolName = attrId.getPath().replace("_spell_power", "");
            // 构建派系的资源路径
            ResourceLocation schoolResource = ResourceLocation.fromNamespaceAndPath(attrId.getNamespace(), schoolName);
            // 从注册表中查找对应的 SchoolType 对象
            return SchoolRegistry.REGISTRY.get(schoolResource);
        }

        return null; // 如果不是法术强度属性，返回 null
    }
}