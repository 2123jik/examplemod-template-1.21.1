package com.example.examplemod.client.tooltip;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
// 移除 net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

@EventBusSubscriber(value = Dist.CLIENT)
public class UnifiedTooltipHandler {

    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RichTooltipData.class, RichTooltipRenderer::new);
    }

    @SubscribeEvent
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        ListIterator<Either<FormattedText, TooltipComponent>> it = elements.listIterator();

        while (it.hasNext()) {
            Either<FormattedText, TooltipComponent> element = it.next();

            // 如果已经不是纯文本了，跳过
            if (element.left().isEmpty()) continue;

            FormattedText textComponent = element.left().get();
            String rawText = extractRawText(textComponent);

            // 1. 调用 Resolver 进行解析 (这将触发懒加载)
            List<Object> parsed = TooltipResolver.INSTANCE.parseText(rawText);

            // 如果解析结果只有1个元素且是String，说明没有发生任何替换，不需要改变
            if (parsed.size() == 1 && parsed.get(0) instanceof String) {
                continue;
            }

            // 2. 获取原文本的颜色作为默认颜色
            int color = 0xAAAAAA; // 默认灰色
            Style style = textComponent.visit((s, str) -> s.isEmpty() ? Optional.empty() : Optional.of(s), Style.EMPTY).orElse(Style.EMPTY);
            if (style.getColor() != null) {
                color = style.getColor().getValue();
            }

            // 3. 替换为 RichTooltipData
            RichTooltipData data = new RichTooltipData(parsed, color);
            it.set(Either.right(data));
        }
    }

    private static String extractRawText(FormattedText text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, s) -> {
            sb.append(s);
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }
}