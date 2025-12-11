package com.example.examplemod.client.tooltip;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import java.util.stream.Stream;

public class EffectParser extends BaseNameParser<Holder<MobEffect>> {
    public static final EffectParser INSTANCE = new EffectParser();

    @Override
    protected Stream<Holder<MobEffect>> getRegistryStream() {
        // 核心修改：添加 .map(h -> h)
        // 这会将 Stream<Reference<MobEffect>> 转换为 Stream<Holder<MobEffect>>
        return BuiltInRegistries.MOB_EFFECT.holders()
                .map(holder -> holder);
    }

    @Override
    protected String getTranslationKey(Holder<MobEffect> holder) {
        return holder.value().getDescriptionId();
    }
}