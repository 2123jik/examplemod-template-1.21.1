package com.example.examplemod.client.tooltip;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record InlineAttributeDataItem(String prefix, Holder<Attribute> attribute, String suffix, int color) implements TooltipComponent {
}