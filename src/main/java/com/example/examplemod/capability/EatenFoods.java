package com.example.examplemod.capability;

import net.minecraft.core.HolderLookup; // 导入 HolderLookup
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EatenFoods implements IEatenFoods, INBTSerializable<CompoundTag> {

    private final List<ItemStack> eatenFoods = new ArrayList<>();

    @Override
    public void addFood(ItemStack food) {
        this.eatenFoods.add(food.copy());
    }

    @Override
    public List<ItemStack> getEatenFoods() {
        return this.eatenFoods;
    }

    @Override
    public void copyFrom(IEatenFoods source) {
        this.eatenFoods.clear();
        this.eatenFoods.addAll(source.getEatenFoods());
    }

    /**
     * 将数据序列化（保存）到 NBT。
     * @param provider NeoForge 提供的 HolderLookup.Provider，用于查找注册表数据。
     * @return 包含数据的 CompoundTag。
     */
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) { // <-- 签名已更新
        CompoundTag nbt = new CompoundTag();
        ListTag listTag = new ListTag();
        for (ItemStack stack : this.eatenFoods) {
            // ItemStack.save 方法现在需要 provider 参数
            listTag.add(stack.save(provider, new CompoundTag())); // <-- 调用已更新
        }
        nbt.put("EatenFoodsList", listTag);
        return nbt;
    }

    /**
     * 从 NBT 中反序列化（加载）数据。
     * @param provider NeoForge 提供的 HolderLookup.Provider。
     * @param nbt 包含数据的 CompoundTag。
     */
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) { // <-- 签名已更新
        this.eatenFoods.clear();
        ListTag listTag = nbt.getList("EatenFoodsList", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            // ItemStack.of() 已被 ItemStack.parse() 替代，用于从 NBT 加载
            // 它返回一个 Optional，因为物品可能在版本更新后消失
            Optional<ItemStack> stackOptional = ItemStack.parse(provider, listTag.getCompound(i)); // <-- 调用已更新
            stackOptional.ifPresent(this.eatenFoods::add);
        }
    }
}