package com.example.examplemod.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import java.util.List;

/**
 * 通用富文本 Tooltip 数据
 * @param parsedSegments 由 TooltipResolver 解析出的列表，包含 String 和 IconDrawer
 * @param defaultColor   该行文本的默认颜色
 */
public record RichTooltipData(List<Object> parsedSegments, int defaultColor) implements TooltipComponent {
}