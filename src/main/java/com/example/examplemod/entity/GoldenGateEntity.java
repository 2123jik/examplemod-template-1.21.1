package com.example.examplemod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class GoldenGateEntity extends Entity {
    // 目标实体的 UUID
    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID = SynchedEntityData.defineId(GoldenGateEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // 施法者（也是箭的主人）
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    // 生命周期计时器
    private int lifeTime = 0;
    private static final int SHOOT_TIME = 15;
    private static final int MAX_LIFE = 25;

    public GoldenGateEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner.getUUID();
    }

    public void setTarget(LivingEntity target) {
        if (target != null) {
            this.entityData.set(TARGET_UUID, Optional.of(target.getUUID()));
        }
    }

    @Override
    public void tick() {
        super.tick();

        // 修复 1: 在 tick 开始时尝试恢复 owner，确保后续逻辑能用到
        resolveOwner();

        this.lifeTime++;

        // 客户端和服务端都会运行这个，所以 getTargetPos 必须安全
        faceTarget();

        if (!this.level().isClientSide) {
            if (this.lifeTime == SHOOT_TIME) {
                shootArrow();
            }
            if (this.lifeTime >= MAX_LIFE) {
                this.discard();
            }
        }
    }

    // 辅助方法：尝试从 UUID 恢复 owner 实体引用
    private void resolveOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity living) {
                this.owner = living;
            }
        }
    }

    private void shootArrow() {
        if (owner == null) return;

        SwordProjectileEntity sword = new SwordProjectileEntity(this.level(), this.getX(), this.getY(), this.getZ());

        sword.setOwner(this.owner);
        sword.setBaseDamage(2.0);
        sword.setNoGravity(true);
        Vec3 targetPos = getTargetPos();
        Vec3 direction = targetPos.subtract(this.position()).normalize();

        sword.shoot(direction.x, direction.y, direction.z, 2.4F, 0.1F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
        this.level().addFreshEntity(sword);

        sword.setNoGravity(false); // 开启重力
    }

    private void faceTarget() {
        // 获取我们要看的目标位置
        Vec3 targetPos = getTargetPos();
        // 计算从自身指向目标的向量
        Vec3 dir = targetPos.subtract(this.position());

        double d0 = dir.horizontalDistance();

        // 计算旋转角度
        this.setYRot((float)(Math.atan2(dir.z, dir.x) * (180D / Math.PI)) - 90.0F);
        this.setXRot((float)(-(Math.atan2(dir.y, d0) * (180D / Math.PI))));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private Vec3 getTargetPos() {
        // 1. 尝试获取锁定的攻击目标 (主要在服务端有效)
        Optional<UUID> targetUUID = this.entityData.get(TARGET_UUID);
        if (targetUUID.isPresent() && this.level() instanceof ServerLevel serverLevel) {
            Entity target = serverLevel.getEntity(targetUUID.get());
            if (target != null) {
                return target.getBoundingBox().getCenter();
            }
        }

        // 2. 尝试跟随主人的视线
        // 修复 2: 检查 owner 是否为 null (客户端 owner 永远为 null，除非手动发包同步)
        if (owner != null) {
            // 修复 3: owner.getForward() 是一个方向向量(例如 0,0,1)，不是坐标点。
            // 我们需要的是 "实体当前位置 + 前方远处的点"
            return this.position().add(owner.getForward().scale(10.0));
        }

        // 3. 终极兜底：如果没有目标也没有主人（比如在客户端），看向自身前方
        // 这样保证永远不会返回 null 或导致计算错误
        return this.position().add(this.getForward().scale(5.0));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_UUID, Optional.empty());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTime = tag.getInt("LifeTime");
        if (tag.hasUUID("Owner")) this.ownerUUID = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifeTime", this.lifeTime);
        if (this.ownerUUID != null) tag.putUUID("Owner", this.ownerUUID);
    }
}