package com.example.examplemod.mixin.vanilla;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Shadow
    public abstract AttributeMap getAttributes();

    /**
     * @param tag NBT数据标签
     * @param ci  回调信息
     */
    @Inject(
            method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("HEAD")
    )
    private void maxhealthfix$loadAttributesBeforeHealth(CompoundTag tag, CallbackInfo ci) {
        // 检查NBT中是否存在 "Attributes" 标签，这是存储属性的地方
        if (tag.contains("Attributes", 9)) { // 9 是 TAG_LIST 的类型ID
            // 调用 getAttributes().load() 方法，提前加载所有属性
            this.getAttributes().load(tag.getList("Attributes", 10)); // 10 是 TAG_COMPOUND 的类型ID
        }
    }
}