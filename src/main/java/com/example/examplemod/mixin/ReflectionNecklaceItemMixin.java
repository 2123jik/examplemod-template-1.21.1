package com.example.examplemod.mixin;

import it.hurts.sskirillss.relics.entities.StalactiteEntity;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.necklace.ReflectionNecklaceItem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import top.theillusivec4.curios.api.SlotContext;

@Mixin(value = ReflectionNecklaceItem.class, remap = false) // remap=false 很重要，因为这是在修改另一个Mod
public abstract class ReflectionNecklaceItemMixin extends RelicItem {

    // Mixin 要求构造函数，这里提供一个虚拟的
    private ReflectionNecklaceItemMixin(Properties properties) {
        super(properties);
    }
    
    // 使用常量，这是一个好习惯
    @Unique
    private static final String ABILITY_EXPLODE = "explode";
    @Unique
    private static final String STAT_CAPACITY = "capacity";
    @Unique
    private static final String STAT_DAMAGE = "damage";
    @Unique
    private static final String STAT_STUN = "stun";

    /**
     * @author YourName // 你的名字
     * @reason Performance optimization and visual improvement for projectile spawning. // 解释为什么重写
     */
    @Overwrite
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // 使用 instanceof 的模式匹配，使代码更简洁
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide) {
            // 每秒执行一次 (20 ticks)
            if (player.tickCount % 20 == 0) {
                int time = stack.getOrDefault(DataComponentRegistry.TIME, 0);
                double charge = stack.getOrDefault(DataComponentRegistry.CHARGE, 0);
                double capacity = this.getStatValue(stack, ABILITY_EXPLODE, STAT_CAPACITY);

                if (time > 0 && charge < capacity) {
                    stack.set(DataComponentRegistry.TIME, --time);
                } else if (charge > 0) {
                    // 调用我们新的、优化的投射物生成方法
                    this.relics_patch$spawnProjectiles(player, stack, charge);
                    
                    // 给予经验并重置状态
                    this.spreadRelicExperience(player, stack, (int) (charge / 10.0));
                    stack.set(DataComponentRegistry.CHARGE, 0);
                    stack.set(DataComponentRegistry.TIME, 0);
                }
            }
        }
    }

    /**
     * 这是我们新添加的方法，用于生成投射物。
     * 使用 @Unique 注解确保它不会与目标类中的任何现有方法冲突。
     * 遵循 "modid$mixinClassName$methodName" 的命名约定是一种推荐的做法，但不是强制的。
     */
    @Unique
    private void relics_patch$spawnProjectiles(Player player, ItemStack stack, double charge) {
        Level level = player.level();
        RandomSource random = player.getRandom();
        
        float damage = (float) (charge * this.getStatValue(stack, ABILITY_EXPLODE, STAT_DAMAGE));
        float stun = (float) (charge * this.getStatValue(stack, ABILITY_EXPLODE, STAT_STUN));

        // 根据 charge 决定生成的投射物数量，但设定一个上限以防止性能问题
        int projectileCount = (int) Math.min(100, 10 + charge * 2); 

        for (int i = 0; i < projectileCount; ++i) {
            // 在单位球体内生成一个随机向量
            double x = random.nextGaussian();
            double y = random.nextGaussian();
            double z = random.nextGaussian();
            double magnitude = Math.sqrt(x * x + y * y + z * z);
            
            // 避免除以零
            if (magnitude == 0) continue;

            Vec3 direction = new Vec3(x / magnitude, y / magnitude, z / magnitude);
            
            // 将生成位置定于玩家身体中上部，避免生成在脚下
            float bodyHeightOffset = player.getBbHeight() * 0.66F;
            Vec3 spawnPos = player.position().add(0.0, bodyHeightOffset, 0.0);

            // 使用 BlockPos.containing 简化坐标转换
            if (!level.getBlockState(BlockPos.containing(spawnPos)).blocksMotion()) {
                StalactiteEntity stalactite = new StalactiteEntity(level, damage, stun);
                stalactite.setOwner(player);
                stalactite.setPos(spawnPos);
                stalactite.setDeltaMovement(direction.scale(0.35F + charge * 0.001F));
                level.addFreshEntity(stalactite);
            }
        }
    }
}