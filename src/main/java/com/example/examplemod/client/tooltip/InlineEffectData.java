package com.example.examplemod.client.tooltip;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

// 这是一个纯数据载体
public record InlineEffectData(String prefix, Holder<MobEffect> effect, String suffix, int color) implements TooltipComponent {
}