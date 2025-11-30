package com.example.examplemod.mixin.vanilla;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
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
    /**
     * @author
     * @reason
     */
    @Overwrite
    private boolean checkTotemDeathProtection(DamageSource damageSource){
        LivingEntity player = (LivingEntity)(Object)this;
        if (damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            ItemStack itemstack = null;

            for (InteractionHand interactionhand : InteractionHand.values()) {
                ItemStack itemstack1 = player.getItemInHand(interactionhand);
                if (itemstack1.is(Items.TOTEM_OF_UNDYING) && net.neoforged.neoforge.common.CommonHooks.onLivingUseTotem(player, damageSource, itemstack1, interactionhand)) {
                    itemstack = itemstack1.copy();
                    itemstack1.shrink(1);
                    break;
                }
            }

            if (itemstack != null) {
                if (player instanceof ServerPlayer serverplayer) {
                    serverplayer.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING), 1);
                    CriteriaTriggers.USED_TOTEM.trigger(serverplayer, itemstack);
                    player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
                }

                player.setHealth(player.getMaxHealth()*0.1f);
                player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.PROTECTED_BY_TOTEM);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                player.level().broadcastEntityEvent(player, (byte)35);
            }

            return itemstack != null;
        }
    }
}