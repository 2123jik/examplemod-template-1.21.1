package com.example.examplemod.spells;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class SpellRegistries {
    // 关键点1：使用 SpellRegistry.SPELL_REGISTRY_KEY，这样才能注册进 Iron's Spells 的系统
    // 关键点2：第二个参数填写你的 MOD ID ("firesenderexpansion")
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ExampleMod.MODID);

    // 注册你的法术
    public static final Supplier<AbstractSpell> GATE_OF_BABYLON = SPELLS.register("gate_of_babylon", GateOfBabylonSpell::new);

    public static final Supplier<AbstractSpell> CUSTOM_FLAMING_STRIKE = SPELLS.register("custom_flaming_strike", CustomFlamingStrikeSpell::new);
    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }
}