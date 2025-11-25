package com.example.examplemod.mixin;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

// 告诉 Mixin 我们要“入侵” StructureTemplate 这个类
@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor {

    // @Accessor 自动生成 getter 方法来访问私有的 "palettes" 字段
    @Accessor("palettes")
    List<StructureTemplate.Palette> getPalettes();
}