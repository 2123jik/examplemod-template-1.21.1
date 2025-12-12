package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.accessor.IExtendedMobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public class MobEffectInstanceMixin implements IExtendedMobEffect {

    // 1. 新增一个字段用来存总时间
    @Unique
    private int examplemod$originalDuration = -1;

    // 2. 拦截构造函数，初始化时记录总时间
    // 注意：我们要拦截那个参数最全的构造函数，或者在所有构造函数注入
    @Inject(method = "<init>(Lnet/minecraft/core/Holder;IIZZZLnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // 在创建实例时，将当前的 duration (也就是初始时间) 赋值给 originalDuration
        MobEffectInstance self = (MobEffectInstance) (Object) this;
        this.examplemod$originalDuration = self.getDuration();
    }
    
    // 3. 拦截 update 方法
    // 当玩家喝了同样的药水，时间会被刷新，我们需要更新总时间
    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(MobEffectInstance other, CallbackInfoReturnable<Boolean> cir) {
        // 如果新的药水时间比当前长，重置总时间
        if (other.getDuration() > ((MobEffectInstance)(Object)this).getDuration()) {
             this.examplemod$originalDuration = other.getDuration();
        }
    }

    // 4. 实现接口方法
    @Override
    public int getOriginalDuration() {
        // 如果由于某种原因没初始化（比如从NBT加载但没存），就返回当前时间作为保底
        return examplemod$originalDuration <= 0 ? ((MobEffectInstance)(Object)this).getDuration() : examplemod$originalDuration;
    }

    @Override
    public void setOriginalDuration(int duration) {
        this.examplemod$originalDuration = duration;
    }
}