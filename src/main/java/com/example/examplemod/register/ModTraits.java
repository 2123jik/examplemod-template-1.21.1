package com.example.examplemod.register;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.traits.ThunderStrikeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits; // 引用 L2Hostility 的注册表
import dev.xkmc.l2hostility.init.entries.LHRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.ChatFormatting;

public class ModTraits {

    public static final LHRegistrate REGISTRATE = new LHRegistrate(ExampleMod.MODID);

    // 改用 RegistryEntry，不再是 TraitEntry
    public static final RegistryEntry<MobTrait, ThunderStrikeTrait> THUNDER_STRIKE = REGISTRATE
            // 使用 generic 通用注册，显式指定注册到 L2Hostility 的 TRAITS 注册表中
            .generic(LHTraits.TRAITS, "thunder_strike",
                    () -> new ThunderStrikeTrait(ChatFormatting.YELLOW, () -> 100))
            .register();

    public static void register() {
    }
}