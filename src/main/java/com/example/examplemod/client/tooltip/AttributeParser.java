package com.example.examplemod.client.tooltip;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import java.util.stream.Stream;

public class AttributeParser extends BaseNameParser<Holder<Attribute>> {
    public static final AttributeParser INSTANCE = new AttributeParser();

    @Override
    protected Stream<Holder<Attribute>> getRegistryStream() {
        // 获取所有注册的属性
        return BuiltInRegistries.ATTRIBUTE.holders().map(h -> h);
    }

    @Override
    protected String getTranslationKey(Holder<Attribute> holder) {
        return holder.value().getDescriptionId();
    }
}