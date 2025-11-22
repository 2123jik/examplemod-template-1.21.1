package com.example.examplemod.spells;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GoldenGateEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.example.examplemod.register.ModEntities.GOLDENGATEENTITY;

@AutoSpellConfig
public class GateOfBabylonSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "gate_of_babylon");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(10)
            .build();

    public GateOfBabylonSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 100; // 持续施法时间 (5秒)
        this.baseManaCost = 20;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // 持续施法开始时触发，通常不需要做太多事，除了开启声音
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData playerMagicData) {
        // 这是一个持续施法法术，每 tick 都会调用
        // 我们设定每 4 tick 生成一个传送门 (也就是每秒 5 发)
        if ((playerMagicData.getCastDurationRemaining()) % 4 == 0) {
            spawnGate(level, caster, spellLevel);
        }
    }

    private void spawnGate(Level level, LivingEntity caster, int spellLevel) {
        // --- 1. 获取目标 (保持不变) ---
        LivingEntity target = null;
        HitResult result = Utils.raycastForEntity(level, caster, 40, true);
        if (result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult entityHitResult) {
            Entity hitEntity = entityHitResult.getEntity();
            if (hitEntity instanceof LivingEntity livingHit && livingHit != caster) {
                target = livingHit;
            }
        }

        // --- 2. 计算生成的“锚点”坐标系 (核心修改) ---

        // A. 获取玩家视线，但只保留水平分量 (x, z)。
        // 这样无论玩家是看天还是看地，生成的“墙”始终是垂直立在地上的，不会插进土里。
        Vec3 rawView = caster.getLookAngle();
        Vec3 forward = new Vec3(rawView.x, 0, rawView.z).normalize();

        // B. 定义“上方” (世界 Y 轴)
        Vec3 up = new Vec3(0, 1, 0);

        // C. 计算“右方” (通过 叉积 计算垂直于前方的向量)
        // 在 Minecraft 坐标系中，forward 叉乘 up 得到右侧向量(或左侧，取决于手性，但这不重要，因为我们要随机左右散布)
        Vec3 right = forward.cross(up).normalize();

        // --- 3. 设定位置偏移参数 (清晰可调) ---

        // 宽度散布：左右随机偏移 (-3.0 到 +3.0 格)
        double spreadWidth = (Math.random() - 0.5) * 6.0;

        // 高度散布：头顶上方随机高度 (2.5 到 5.0 格)
        double heightOffset = 2.5 + Math.random() * 2.5;

        // 前后散布：身后来一点距离 (1.5 到 3.0 格)
        // 注意：我们要往“后”放，所以后面会用到 subtract
        double distanceBehind = 1.5 + Math.random() * 1.5;

        // --- 4. 组合最终坐标 ---
        // 公式：玩家位置 + (右向量 * 宽度) + (上向量 * 高度) - (前向量 * 后退距离)
        Vec3 spawnPos = caster.position()
                .add(right.scale(spreadWidth))      // 左右移动
                .add(up.scale(heightOffset))        // 向上移动
                .subtract(forward.scale(distanceBehind)); // 向后移动 (减去前方向量)


        // --- 5. 生成实体 (保持不变，加上之前的旋转修复) ---
        GoldenGateEntity gate = new GoldenGateEntity(GOLDENGATEENTITY.get(), level);

        gate.setOwner(caster);
        if (target != null && target != caster) {
            gate.setTarget(target);
        }

        // 计算朝向逻辑 (保持之前的修复)
        Vec3 targetPos;
        if (target != null) {
            targetPos = target.getBoundingBox().getCenter();
        } else {
            // 如果没有目标，看向施法者视线的前方远处 (而不是施法者本身)
            targetPos = caster.position().add(caster.getLookAngle().scale(30.0));
        }

        Vec3 dir = targetPos.subtract(spawnPos);
        double d0 = dir.horizontalDistance();

        float yRot = (float)(Math.atan2(dir.z, dir.x) * (180D / Math.PI)) - 90.0F;
        float xRot = (float)(-(Math.atan2(dir.y, d0) * (180D / Math.PI)));

        // 设置位置和旋转
        gate.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, yRot, xRot);

        // 消除插值抖动
        gate.yRotO = yRot;
        gate.xRotO = xRot;

        level.addFreshEntity(gate);
    }
    
    // 其他必要的方法...
}