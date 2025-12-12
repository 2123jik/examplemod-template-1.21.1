package com.example.examplemod.register;

import com.example.examplemod.server.effect.CombustionCurseEffect;
import com.example.examplemod.server.effect.FearEffect;
import com.example.examplemod.server.effect.MakenPowerEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.example.examplemod.ExampleMod.MODID;

/**
 * 模组效果注册类 (ModEffects)
 * 用于统一管理和注册模组中所有的自定义药水/状态效果。
 */
public class ModEffects {

    // 创建一个 DeferredRegister（延迟注册器）实例。
    // 这是 NeoForge 用于安全注册游戏元素（如方块、物品、效果）的标准机制。
    // 1. Registries.MOB_EFFECT: 指定我们要注册的内容类型是“药水效果”。
    // 2. MODID: 指定这些效果归属于哪个模组（命名空间）。
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);

    public static final Holder<MobEffect> COMBUSTION_CURSE = MOB_EFFECTS.register("combustion_curse",
            () -> new CombustionCurseEffect(MobEffectCategory.HARMFUL, 0xFF4500));

    // 注册一个名为 "fear" (恐惧) 的效果。
    // Holder<MobEffect>: 返回一个持有者对象，用于在后续代码中（如给实体添加效果时）引用这个效果。
    // .register 参数说明:
    //   - "fear": 注册名（Registry Name），游戏内 ID 将变为 examplemod:fear。
    //   - Supplier: 一个提供者函数，用于实例化自定义的效果类 FearEffect。
    //     - MobEffectCategory.HARMFUL: 分类为“有害”效果（红色文本，对玩家不利）。
    //     - 0x551a8b: 效果粒子的颜色（十六进制 RGB，此处为一种深紫色）。
    public static final Holder<MobEffect> FEAR = MOB_EFFECTS.register("fear",
            () -> new FearEffect(MobEffectCategory.HARMFUL, 0x551a8b));

    public static final Holder<MobEffect> ARMOR = MOB_EFFECTS.register("armor",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> ARMOR_TOUGHNESS = MOB_EFFECTS.register("armor_toughness",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> ATTACK_BURNING_DURATION = MOB_EFFECTS.register("attack_burning_duration",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> ATTACK_KNOCKBACK = MOB_EFFECTS.register("attack_knockback",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> BLOCK_INTERACTION_RANGE = MOB_EFFECTS.register("block_interaction_range",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> DRINKING_SPEED = MOB_EFFECTS.register("drinking_speed",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> EATING_SPEED = MOB_EFFECTS.register("eating_speed",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> FLATULENCE = MOB_EFFECTS.register("flatulence",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> KNOCKBACK_RESISTANCE = MOB_EFFECTS.register("knockback_resistance",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> MINING_SPEED = MOB_EFFECTS.register("mining_speed",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> PROJECTILE_DAMAGE = MOB_EFFECTS.register("projectile_damage",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
    public static final Holder<MobEffect> MOUNT_SPEED = MOB_EFFECTS.register("mount_speed",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    // 注册一个名为 "maken_power" (魔剑之力/玛肯之力) 的效果。
    // .register 参数说明:
    //   - "maken_power": 注册名，游戏内 ID 为 examplemod:maken_power。
    //   - Supplier: 实例化 MakenPowerEffect 类。
    //     - MobEffectCategory.BENEFICIAL: 分类为“有益”效果（蓝色文本，对玩家有利）。
    //     - 0xff4500: 效果粒子的颜色（十六进制 RGB，此处为橙红色）。
    public static final Holder<MobEffect> MAKEN_POWER = MOB_EFFECTS.register("maken_power",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));


    public static final Holder<MobEffect> MOVEMENT_SPEED_ON_SNOW = MOB_EFFECTS.register("movement_speed_on_snow",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> SLIP_RESISTANCE = MOB_EFFECTS.register("slip_resistance",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> SPRINTING_SPEED = MOB_EFFECTS.register("sprinting_speed",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> SPRINTING_STEP_HEIGHT = MOB_EFFECTS.register("sprinting_step_height",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> LIFE_STEAL = MOB_EFFECTS.register("life_steal",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> GOLEM_JUMP = MOB_EFFECTS.register("golem_jump",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> GOLIATH_SLAYER = MOB_EFFECTS.register("goliath_slayer",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));


    public static final Holder<MobEffect> SCALE = MOB_EFFECTS.register("scale",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> ATTACK_SPEED = MOB_EFFECTS.register("attack_speed",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));

    public static final Holder<MobEffect> MANA_REGEN = MOB_EFFECTS.register("mana_regen",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));


}