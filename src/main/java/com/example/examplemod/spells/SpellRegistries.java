package com.example.examplemod.spells;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 法术注册类 (SpellRegistries)
 * 用于集中管理和注册模组中的自定义法术。
 */
public class SpellRegistries {

    /**
     * 创建法术的延迟注册器 (DeferredRegister)。
     * <p>
     * 关键点解释：
     * 1. SpellRegistry.SPELL_REGISTRY_KEY: 告诉 NeoForge 我们要注册的是 Iron's Spells 的法术，而不是原版的物品或方块。
     * 2. ExampleMod.MODID: 告诉 NeoForge 这些法术属于哪个模组（防止ID冲突）。
     */
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ExampleMod.MODID);

    // =========================================
    //               法术注册项
    // =========================================

    /**
     * 注册 "王之财宝" (Gate of Babylon) 法术。
     *
     * @param "gate_of_babylon" 法术的注册名（ID），全名为 "examplemod:gate_of_babylon"
     * @param GateOfBabylonSpell::new  法术类的构造函数引用（当需要实例化时被调用）
     */
    public static final Supplier<AbstractSpell> GATE_OF_BABYLON = SPELLS.register("gate_of_babylon", GateOfBabylonSpell::new);

    /**
     * 注册 "自定义烈焰打击" (Custom Flaming Strike) 法术。
     *
     * @param "custom_flaming_strike" 法术的注册名（ID）
     * @param CustomFlamingStrikeSpell::new 法术类的构造函数引用
     */
    public static final Supplier<AbstractSpell> CUSTOM_FLAMING_STRIKE = SPELLS.register("custom_flaming_strike", CustomFlamingStrikeSpell::new);

    /**
     * 初始化注册方法。
     * <p>
     * 必须在模组主类 (ExampleMod) 的构造函数中调用此方法，
     * 否则这里的注册项永远不会被加载到游戏中。
     *
     * @param eventBus 模组的事件总线
     */
    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }
}