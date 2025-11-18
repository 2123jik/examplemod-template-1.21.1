package com.example.examplemod.recipe; // 确保包名正确

import com.example.examplemod.component.SpellBonusData;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import static com.example.examplemod.component.ModDataComponents.SPELL_BONUSES; // 你的组件
import static io.redspace.ironsspellbooks.registries.ComponentRegistry.SPELL_CONTAINER; // Iron's Spells 的组件

public class ApplyScrollBonusRecipe extends SmithingTransformRecipe {

    public ApplyScrollBonusRecipe(Ingredient template, Ingredient base, Ingredient addition, ItemStack result) {
        super(template, base, addition, result);
    }

    /**
     * 这是配方的核心逻辑，用于生成最终的物品。
     */
    @Override
    public ItemStack assemble(SmithingRecipeInput container, HolderLookup.Provider registryAccess) {
        ItemStack baseItem = container.base();
        ItemStack scroll = container.addition();

        ItemStack result = baseItem.copy();
        result.setCount(1);
        SpellBonusData bonusData = new SpellBonusData.Builder()
                .addSpellBonus(scroll.get(SPELL_CONTAINER).getSpellAtIndex(0).getSpell().getSpellResource(), 1)
                .build();

        result.set(SPELL_BONUSES.get(), bonusData);

        return result;
    }

    @Override
    public boolean matches(SmithingRecipeInput container, net.minecraft.world.level.Level level) {
        if (!super.matches(container, level)) {
            return false;
        }
        if(!(container.addition().getItem() instanceof Scroll))return false;
        ItemStack scroll = container.addition();
        var spellContainer = scroll.get(SPELL_CONTAINER);

        return spellContainer != null && !spellContainer.isEmpty();
    }

    /**
     * 告诉游戏这个配方应该使用哪个序列化器。
     */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.APPLY_SCROLL_BONUS_SERIALIZER.get();
    }
}