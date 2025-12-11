package com.example.examplemod.client.tooltip;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record InlineSpellData(String prefix, AbstractSpell spell, String suffix, int color) implements TooltipComponent {
}