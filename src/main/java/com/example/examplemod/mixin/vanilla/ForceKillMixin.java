//package com.example.examplemod.mixin.vanilla;
//
//import net.minecraft.world.damagesource.DamageSource;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.level.Level;
//import org.jetbrains.annotations.Nullable;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(LivingEntity.class)
//public abstract class ForceKillMixin {
//
//    // 影子方法：获取原版逻辑
//    @Shadow public abstract boolean isDeadOrDying();
//    @Shadow public abstract boolean hurt(DamageSource source, float amount);
//    @Shadow public abstract void remove(Entity.RemovalReason reason);
//    @Shadow @Nullable private DamageSource lastDamageSource;
//
//
//    // 标记：该实体是否已经触发过死亡流程
//    @Unique private boolean myMod$hasEnteredDeathProcess = false;
//
//    // 标记：记录第一次致死的原因
//    @Unique private DamageSource myMod$fatalReason = null;
//
//    // 标记：是否已经尝试过一次“物理处决”（造成伤害）
//    @Unique private boolean myMod$executionAttempted = false;
//
//    /**
//     * 1. 监听死亡开始
//     * 当实体进入死亡动画逻辑（tickDeath）时，我们将其标记为“已死”。
//     */
//    @Inject(method = "tickDeath", at = @At("HEAD"))
//    private void onDeathProcessStart(CallbackInfo ci) {
//        // 如果还没标记过，且确实有伤害来源，就记录下来
//        if (!this.myMod$hasEnteredDeathProcess) {
//            this.myMod$hasEnteredDeathProcess = true;
//            if (this.lastDamageSource != null) {
//                // 备份死因，防止后续逻辑丢失
//                this.myMod$fatalReason = this.lastDamageSource;
//            }
//        }
//    }
//
//    /**
//     * 2. 每刻检查 (Tick) - 执行处决逻辑
//     * 这里的逻辑是：既然你已经进过 tickDeath，那你现在必须得是死的状态。
//     * 如果你居然还活着 (!isDeadOrDying)，说明出 Bug 了，我要手动干预。
//     */
//    @Inject(method = "tick", at = @At("TAIL"))
//    private void onTickTail(CallbackInfo ci) {
//        // 只有当实体被标记为“该死”但系统认为它“活着”时触发
//        if (this.myMod$hasEnteredDeathProcess && !this.isDeadOrDying()) {
//
//            // 准备伤害源：如果有记录就用记录的，没有就用通用的 GENERIC
//            DamageSource source = this.myMod$fatalReason;
//
//
//            // --- 第一阶段：尝试再次造成巨额伤害 ---
//            // 这样做是为了触发可能存在的死亡掉落或事件
//            this.hurt(source, Float.MAX_VALUE); // 造成无限大伤害
//
//            // --- 第二阶段：强力模式 (Force Mode) ---
//            if (this.myMod$executionAttempted) {
//                // 如果我们已经试过一次造成伤害，但它还没死（能运行到这一行说明还在活着）
//                // 或者是处于无敌状态。
//                // 直接移除实体，不废话。
//                this.remove(Entity.RemovalReason.KILLED);
//
//                // 可选：打印日志方便调试
//                // System.out.println("Force removed buggy entity: " + ((LivingEntity)(Object)this));
//            }
//
//            // 标记已经尝试过一次了，下一刻如果它还活着，直接 remove
//            this.myMod$executionAttempted = true;
//        }
//    }
//}