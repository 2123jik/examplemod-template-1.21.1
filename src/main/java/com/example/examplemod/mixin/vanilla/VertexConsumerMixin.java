package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.bridge.LegacyVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin extends LegacyVertexConsumer {
    // 这里不需要写代码，因为逻辑都在 LegacyVertexConsumer 的 default 方法里
    // Mixin 会自动把那些 default 方法“粘贴”到运行时类中
}