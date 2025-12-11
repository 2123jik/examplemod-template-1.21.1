package com.example.examplemod.item;

import com.example.examplemod.component.ModDataComponents;
import com.example.examplemod.register.ModItems;
import com.example.examplemod.util.StructureIDList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MiniatureStructureItem extends Item {

    public MiniatureStructureItem(Properties properties) {
        super(properties);
    }

    /**
     * 静态工厂方法：创建一个带随机结构 ID 的物品堆。
     * 可以在合成、战利品表或 /give 命令中使用。
     */
    public static ItemStack createRandomStack(RandomSource random) {
        // 创建物品堆
        ItemStack stack = new ItemStack(ModItems.MINIATURE_STRUCTURE_ITEM.get());

        // 随机获取结构 ID
        ResourceLocation randomId = StructureIDList.getRandomStructure(random);

        // 设置 Data Component
        stack.set(ModDataComponents.STRUCTURE_ID.get(), randomId);

        return stack;
    }
}