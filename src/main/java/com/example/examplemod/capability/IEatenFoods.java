package com.example.examplemod.capability;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public interface IEatenFoods {
    /**
     * 向记录中添加一个食物。
     */
    void addFood(ItemStack food);

    /**
     * 检查是否已经吃过这种食物。
     * @param food 要检查的食物
     * @return 如果吃过返回 true，否则返回 false
     */
    boolean hasEaten(ItemStack food); // <--- 新增方法

    List<ItemStack> getEatenFoods();

    void copyFrom(IEatenFoods source);
}