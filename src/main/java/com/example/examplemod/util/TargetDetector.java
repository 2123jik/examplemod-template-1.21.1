package com.example.examplemod.util;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

public class TargetDetector {

    @Nullable // 可能没有目标，所以返回可空类型
    public static LivingEntity getTargetedEntity() {
        Minecraft mc = Minecraft.getInstance();
        
        // 1. 获取准星的碰撞结果
        HitResult hitResult = mc.hitResult;
        
        // 2. 检查结果类型是否为实体
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            // 3. 将结果转换为实体碰撞结果
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity target = entityHitResult.getEntity();
            
            // 4. 确保目标是 LivingEntity (因为只有它们才有属性)
            if (target instanceof LivingEntity) {
                return (LivingEntity) target;
            }
        }
        
        // 如果没有指向实体，或者实体不是 LivingEntity，返回 null
        return null; 
    }
}