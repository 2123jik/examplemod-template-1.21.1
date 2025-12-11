package com.example.examplemod.client.tooltip;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.stream.Stream;

public class SpellParser extends BaseNameParser<AbstractSpell> {
    public static final SpellParser INSTANCE = new SpellParser();

    @Override
    protected Stream<AbstractSpell> getRegistryStream() {
        // 将 Registry 转换为 Stream
        return SpellRegistry.REGISTRY.stream();
    }

    @Override
    protected String getTranslationKey(AbstractSpell spell) {
        return spell.getComponentId();
    }

    @Override
    protected boolean shouldRegister(AbstractSpell spell) {
        return spell.isEnabled();
    }
}