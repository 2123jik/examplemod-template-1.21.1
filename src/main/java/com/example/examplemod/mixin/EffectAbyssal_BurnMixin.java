package com.example.examplemod.mixin;

import com.github.L_Ender.cataclysm.effects.EffectAbyssal_Burn;
import com.github.L_Ender.cataclysm.util.CMDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EffectAbyssal_Burn.class)
public abstract class EffectAbyssal_BurnMixin extends MobEffect {

    protected EffectAbyssal_BurnMixin(MobEffectCategory category, int color) {
        super(category, color);
    }

}
