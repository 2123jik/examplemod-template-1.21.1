package com.example.examplemod.mixin;

import com.example.examplemod.client.chess.ChessAIHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMob extends LivingEntity {

    protected MixinMob(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    // 注入到 onAddedToWorld 或 finalizeSpawn
    // 这里选择 tick 的第一帧或者 checkDespawn 附近，或者直接通过外部调用初始化
    // 为了保险，我们通过一个 Tag 来识别是否处理过
    @Inject(method = "tick", at = @At("HEAD"))
    private void injectChessAI(CallbackInfo ci) {
        if (!this.level().isClientSide) {
            Mob self = (Mob) (Object) this;
            // 如果实体有 chess_piece 标签，且没有被初始化过 AI
            if (self.getTags().contains("chess_piece") && !self.getTags().contains("ai_injected")) {
                ChessAIHelper.applyChessAI(self);
                self.addTag("ai_injected");
            }
        }
    }
}