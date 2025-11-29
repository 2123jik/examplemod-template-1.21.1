package com.example.examplemod.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.*;

public class EatenFoods implements IEatenFoods, INBTSerializable<CompoundTag> {

    private final List<ItemStack> eatenFoods = new ArrayList<>();

    @Override
    public void addFood(ItemStack food) {
        // 关键修改：只有当没吃过的时候，才添加进列表
        // 这样可以保证内存和硬盘里的列表永远是干净的
        if (!hasEaten(food)) {
            // copy() 很重要，因为传入的 stack 可能会被消耗掉或改变
            ItemStack savedStack = food.copy();
            savedStack.setCount(1); // 强制数量为1，我们只关心类型
            this.eatenFoods.add(savedStack);
        }
    }

    // <--- 实现新增的方法
    @Override
    public boolean hasEaten(ItemStack food) {
        for (ItemStack eaten : this.eatenFoods) {
            // isSameItem 只比较物品类型 (例如都是 minecraft:apple)，不比较数量或耐久
            if (ItemStack.isSameItem(eaten, food)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ItemStack> getEatenFoods() {
        return this.eatenFoods;
    }

    @Override
    public void copyFrom(IEatenFoods source) {
        this.eatenFoods.clear();
        // 复制时也可以顺手去重，虽然理论上 source 应该是已经去重过的
        for (ItemStack stack : source.getEatenFoods()) {
            addFood(stack);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        ListTag listTag = new ListTag();
        for (ItemStack stack : this.eatenFoods) {
            listTag.add(stack.save(provider, new CompoundTag()));
        }
        nbt.put("EatenFoodsList", listTag);
        return nbt;
    }

    /**
     * 关键修改：反序列化时进行“数据清洗”
     * 当旧存档（包含1000个苹果）加载时，这里会把它变成1个苹果。
     */
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.eatenFoods.clear();
        ListTag listTag = nbt.getList("EatenFoodsList", CompoundTag.TAG_COMPOUND);

        // 使用一个临时的 Set 来记录我们在这次加载中已经添加过的 Item 类型
        Set<Item> loadedItemTypes = new HashSet<>();

        for (int i = 0; i < listTag.size(); i++) {
            Optional<ItemStack> stackOptional = ItemStack.parse(provider, listTag.getCompound(i));

            stackOptional.ifPresent(stack -> {
                // 如果这个类型的物品还没被加进本次的内存列表
                if (!loadedItemTypes.contains(stack.getItem())) {
                    this.eatenFoods.add(stack);
                    loadedItemTypes.add(stack.getItem()); // 标记为已添加
                }
                // 如果 loadedItemTypes 里已经有了，说明是旧存档的重复数据，直接丢弃（也就是不 add）
            });
        }
    }
}