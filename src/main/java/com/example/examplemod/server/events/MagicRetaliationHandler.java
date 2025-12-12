package com.example.examplemod.server.events;

import com.example.examplemod.ExampleMod;
import com.google.common.collect.Maps;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.spells.TargetAreaCastData;
import io.redspace.ironsspellbooks.spells.fire.ScorchSpell; 
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import twilightforest.init.TFItems;
import twilightforest.item.FieryArmorItem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class MagicRetaliationHandler {

    // ScorchSpell 实例 (作为单例，避免重复创建)
    private static final ScorchSpell SCORCH_SPELL = new ScorchSpell(); 
    
    // 冷却映射表: <实体ID, 下次可施法时间>
    private static final Map<Integer, Long> COOLDOWN_MAP = new ConcurrentHashMap<>();
    
    // 冷却时间：20 刻 (1 秒)。用于防止法术被高频触发。
    private static final long COOLDOWN_TICKS = 20; 

    private static final int SPELL_LEVEL = 1;

    public static boolean isWearingSpecialEquipment(LivingEntity entity) {
        // 遍历所有盔甲槽位
        for (ItemStack itemStack : entity.getArmorSlots()) {
            if (!itemStack.isEmpty() && itemStack.getItem() instanceof FieryArmorItem) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLivingDamaged(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity(); // 被攻击者 (施法者)
        Level level = target.level();

        if (level.isClientSide()) {
            return; // 仅在服务器端处理施法逻辑
        }
        if (!isWearingSpecialEquipment(target)) {
            return;
        }

        // B. 冷却检查
        long currentTime = level.getGameTime();
        int entityId = target.getId();

        if (COOLDOWN_MAP.containsKey(entityId) && COOLDOWN_MAP.get(entityId) > currentTime) {
            return; 
        }

        // C. 攻击者检查
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            
            if (target == attacker) {
                return; // 防止自我伤害触发反击
            }

            // D. 施法准备
            MagicData targetMagicData = MagicData.getPlayerMagicData(target);

            if (targetMagicData != null && attacker.isAlive()) {

                Vec3 targetLocation = attacker.position();

                
                // 1. 创建临时的 TargetedAreaEntity 实例
                TargetedAreaEntity dummyArea = TargetedAreaEntity.createTargetAreaEntity(
                    level, 
                    targetLocation,
                    2.5f,
                    Utils.packRGB(SCORCH_SPELL.getTargetingColor())
                );
                
                // 2. 设置 Cast Data
                TargetAreaCastData castData = new TargetAreaCastData(targetLocation, dummyArea);
                targetMagicData.setAdditionalCastData(castData);
                
                // 3. 强制施法 (瞬发，零蓝耗，绕过冷却)
                SCORCH_SPELL.onCast(
                    level, 
                    SPELL_LEVEL, 
                    target,         
                    CastSource.SWORD,
                    targetMagicData
                );
                
                // 4. 清理：移除假实体
                dummyArea.discard();
                
                // 5. 设置冷却时间
                COOLDOWN_MAP.put(entityId, currentTime + COOLDOWN_TICKS);
            }
        }
    }
}