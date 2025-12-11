package com.example.examplemod.mixin.ironsspellbooks;

import com.example.examplemod.accessor.SpellHealEventAccessor;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SpellHealEvent.class, remap = false)
public abstract class SpellHealEventMixin implements SpellHealEventAccessor {
    @Mutable
    @Shadow
    private float healAmount;
    public void setHealAmount(float newAmount) {
        this.healAmount = newAmount;
    }
}
