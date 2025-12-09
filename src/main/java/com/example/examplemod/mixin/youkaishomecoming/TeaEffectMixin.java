package com.example.examplemod.mixin.youkaishomecoming;

import dev.xkmc.youkaishomecoming.content.effect.TeaEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TeaEffect.class)
public class TeaEffectMixin {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean applyEffectTick(LivingEntity e, int lv) {
        if (e.level().isDay() && !e.level().isClientSide) {
            float f = e.getLightLevelDependentMagicValue();
            BlockPos blockpos = BlockPos.containing(e.getX(), e.getEyeY(), e.getZ());
            if (f > 0.5F && e.level().canSeeSky(blockpos)) {
                e.heal(e.getMaxHealth()*0.05f*(float) lv);
            }
        }

        return true;
    }
}
