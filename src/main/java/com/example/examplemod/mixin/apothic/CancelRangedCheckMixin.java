package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.AdventureEvents;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(AdventureEvents.class) // 这里填写 fireProjectile 所在的类
public class CancelRangedCheckMixin {
    /**
     * @author
     * @reason
     */
    @Overwrite
    @SubscribeEvent(
            priority = EventPriority.HIGH
    )
    public void fireProjectile(EntityJoinLevelEvent e) {
        Entity var3 = e.getEntity();
        if (var3 instanceof Projectile proj) {
            if (!proj.getPersistentData().getBoolean("apoth.generated"))
            {
                Entity var4 = proj.getOwner();
                if (var4 instanceof LivingEntity) {
                    LivingEntity user = (LivingEntity)var4;
                    ItemStack weapon = user.getUseItem();
                    SocketHelper.getGems(weapon).onProjectileFired(user, proj);
                    AffixHelper.streamAffixes(weapon).forEach((a) -> a.onProjectileFired(user, proj));
                    AffixHelper.copyToProjectile(weapon, proj);
                }
            }
        }

    }


//    @ModifyExpressionValue(
//            method = "fireProjectile",
//            at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apotheosis/loot/LootCategory;isRanged()Z")
//    )
//    public boolean forceRanged(boolean original) {
//        return true;
//    }
}