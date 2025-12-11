package com.example.examplemod.client.tooltip;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

@EventBusSubscriber(value = Dist.CLIENT)
public class UnifiedTooltipHandler {

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(AttributeParser.INSTANCE); // 注册属性解析器
        event.registerReloadListener(EffectParser.INSTANCE);
        event.registerReloadListener(SpellParser.INSTANCE);

    }

    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(InlineAttributeData.class, InlineAttributeRenderer::new); // 注册渲染器
        event.register(InlineEffectData.class, InlineEffectRenderer::new);
        event.register(InlineSpellData.class, InlineSpellRenderer::new);
        event.register(InlineAttributeDataItem.class, InlineAttributeItemRenderer::new);

    }

    @SubscribeEvent
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        ListIterator<Either<FormattedText, TooltipComponent>> it = elements.listIterator();

        while (it.hasNext()) {
            Either<FormattedText, TooltipComponent> element = it.next();

            // 如果不是纯文本，直接跳过
            if (element.left().isEmpty()) continue;

            String rawText = extractRawText(element.left().get());
            var attriResult = AttributeParser.INSTANCE.findInText(rawText);
            if (attriResult.isPresent()) {
                var res = attriResult.get();

                // 尝试获取当前文本行的颜色，如果获取不到则使用默认属性蓝
                int color = 0x5555FF; // 默认属性蓝
                Style style = element.left().get().visit((s, str) -> s.isEmpty() ? Optional.empty() : Optional.of(s), Style.EMPTY).orElse(Style.EMPTY);
                if (style.getColor() != null) {
                    color = style.getColor().getValue();
                }

                InlineAttributeDataItem data = new InlineAttributeDataItem(
                        res.originalText().substring(0, res.index()), // 前缀: "+ 5 "
                        res.object(),                                 // 属性对象
                        res.originalText().substring(res.index()),    // 后缀: "攻击伤害"
                        color
                );
                it.set(Either.right(data));
                // continue; // 如果后面没有其他逻辑，这里continue
            }
            var attrResult = AttributeParser.INSTANCE.findInText(rawText);
            if (attrResult.isPresent()) {
                var res = attrResult.get();

                // 获取原始文本的样式颜色 (例如 "+5" 通常是蓝色或红色)
                // 我们尽量保留这个颜色，或者如果需要，使用药水颜色
                int textColor = 0x5555FF; // 默认属性蓝

                // 简单的样式颜色提取逻辑 (取第一个字符的样式)
                Style style = element.left().get().visit((s, str) -> s.isEmpty() ? Optional.empty() : Optional.of(s), Style.EMPTY).orElse(Style.EMPTY);
                if (style.getColor() != null) {
                    textColor = style.getColor().getValue();
                }

                // 创建数据
                InlineAttributeData data = new InlineAttributeData(
                        res.originalText().substring(0, res.index()), // 前缀
                        res.object(),                                 // 属性 Holder
                        res.originalText().substring(res.index()),    // 后缀 (属性名)
                        textColor
                );

                it.set(Either.right(data));
            }
            
            // 1. 尝试匹配药水效果
            var effectResult = EffectParser.INSTANCE.findInText(rawText);
            if (effectResult.isPresent()) {
                var res = effectResult.get();
                InlineEffectData data = new InlineEffectData(
                        res.originalText().substring(0, res.index()),
                        res.object(),
                        res.originalText().substring(res.index()),
                        0xA0A0A0
                );
                it.set(Either.right(data));
                continue; // 这一行处理完了，处理下一行
            }

            // 2. 尝试匹配法术 (只有当没匹配到药水时才执行)
            var spellResult = SpellParser.INSTANCE.findInText(rawText);
            if (spellResult.isPresent()) {
                var res = spellResult.get();
                InlineSpellData data = new InlineSpellData(
                        res.originalText().substring(0, res.index()),
                        res.object(),
                        res.originalText().substring(res.index()),
                        0xA0A0A0
                );
                it.set(Either.right(data));
            }

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