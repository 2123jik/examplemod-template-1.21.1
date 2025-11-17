package com.example.examplemod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityType.Builder.class)
public interface EntityTypeBuilderAccessor<T extends Entity> {
    /**
     * 通过此 Accessor 获取 updateInterval 字段的值。
     * 尽管我们在这个例子中用不到 getter，但写出来有助于理解。
     */
    @Accessor("updateInterval")
    int getUpdateInterval();

    /**
     * 通过此 Accessor 设置 updateInterval 字段的值。
     * 这是我们真正需要的方法。
     * @param updateInterval 新的更新间隔
     */
    @Accessor("updateInterval")
    void setUpdateInterval(int updateInterval);
}