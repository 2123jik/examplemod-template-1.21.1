package com.example.examplemod.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotAccessor {
    @Accessor("x")
    void setX(int x);

    @Accessor("x")
    int getX();

    @Accessor("y")
    void setY(int y);

    @Accessor("y")
    int getY();
}