package com.example.examplemod.init;

import com.example.examplemod.server.effect.FearEffect;
import com.example.examplemod.server.effect.MakenPowerEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.example.examplemod.ExampleMod.MODID;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);

    public static final Holder<MobEffect> FEAR = MOB_EFFECTS.register("fear",
            () -> new FearEffect(MobEffectCategory.HARMFUL, 0x551a8b));

    public static final Holder<MobEffect> MAKEN_POWER = MOB_EFFECTS.register("maken_power",
            () -> new MakenPowerEffect(MobEffectCategory.BENEFICIAL, 0xff4500));
}