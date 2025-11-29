package com.example.examplemod.mixin.vanilla;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(LootTable.class)
public class LootTableMixin
{

    /**
     * @author
     * @reason
     */
    @Overwrite
    public List<Integer> getAvailableSlots(Container inventory, RandomSource random)
    {
        ObjectArrayList<Integer> objectarraylist = new ObjectArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            if (inventory.getItem(i).isEmpty())
            {
                objectarraylist.add(i);
            }
        }
        return objectarraylist;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void shuffleAndSplitItems(ObjectArrayList<ItemStack> stacks, int emptySlotsCount, RandomSource random)
    {


        for(int i = 0; i < stacks.size(); i++) {
            ItemStack stackA = stacks.get(i);

            for(int j = i + 1; j < stacks.size(); j++) {
                ItemStack stackB = stacks.get(j);

                if(!ItemStack.isSameItemSameComponents(stackA, stackB)) {
                    continue;
                }

                int mergedCount = stackA.getCount() + stackB.getCount();
                int actualMergedCount = Math.min(mergedCount, stackB.getMaxStackSize());
                stackB.setCount(actualMergedCount);
                stackA.setCount(mergedCount - actualMergedCount);
            }
        }

    }
}