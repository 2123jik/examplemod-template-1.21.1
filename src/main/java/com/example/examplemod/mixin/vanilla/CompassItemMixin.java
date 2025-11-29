package com.example.examplemod.mixin.vanilla;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {

    // 只需要继承构造函数，为了不报错
    public CompassItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void onUseOnBed(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(BlockTags.BEDS) && state.is(Blocks.RESPAWN_ANCHOR)) {
            
            // 1. 播放“锁定”音效
            level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);

            if (!level.isClientSide) {
                Player player = context.getPlayer();
                ItemStack handStack = context.getItemInHand();

                // 2. 创建追踪组件
                // tracked = false 很重要！
                // 这表示“只记录静态坐标，不检查方块是否是磁石”。
                // 这样即使床被挖了，指南针依然会指向那个位置（类似 GPS）。
                LodestoneTracker tracker = new LodestoneTracker(
                        Optional.of(GlobalPos.of(level.dimension(), pos)),
                        false
                );

                // 3. 处理物品堆叠 (如果只有1个，直接变；如果有多个，拆分)
                if (handStack.getCount() == 1) {
                    handStack.set(DataComponents.LODESTONE_TRACKER, tracker);
                } else {
                    ItemStack newStack = handStack.transmuteCopy(Items.COMPASS, 1);
                    handStack.shrink(1);
                    newStack.set(DataComponents.LODESTONE_TRACKER, tracker);

                    if (!player.getInventory().add(newStack)) {
                        player.drop(newStack, false);
                    }
                }
            }

            // 4. 拦截原版逻辑，直接返回成功
            // 这样原版代码就不会运行，防止它因为不是磁石而报错或没反应
            cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        }
        // 如果不是床，方法继续向下执行，原版磁石逻辑依然有效
    }
}