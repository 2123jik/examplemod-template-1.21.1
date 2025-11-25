package com.example.examplemod.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotAccessor {
    // @Accessor 生成 getter/setter
    // @Mutable 移除 final 修饰符，允许修改
    
    @Accessor("x")
    @Mutable
    void setX(int x);

    @Accessor("y")
    @Mutable
    void setY(int y);
}