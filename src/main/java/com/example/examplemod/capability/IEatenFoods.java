package com.example.examplemod.capability;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * 这个接口定义了我们的能力有什么功能。
 * 其他代码应该通过这个接口来与我们的能力交互，而不是直接使用实现类。
 */
public interface IEatenFoods {
    /**
     * 向记录中添加一个食物。
     * @param food 吃掉的食物 ItemStack。
     */
    void addFood(ItemStack food);

    /**
     * 获取所有吃过的食物的列表。
     * @return 一个包含所有吃过的食物的列表。
     */
    List<ItemStack> getEatenFoods();

    /**
     * 从另一个能力实例中复制数据。主要用于玩家重生。
     * @param source 提供数据的源能力实例。
     */
    void copyFrom(IEatenFoods source);

}