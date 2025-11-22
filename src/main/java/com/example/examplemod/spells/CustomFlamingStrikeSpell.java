package com.example.examplemod.spells;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.particle.FlameStrikeParticleOptions;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@AutoSpellConfig
public class CustomFlamingStrikeSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "custom_flaming_strike");
    private final DefaultConfig defaultConfig;

    // 定义基础数值 (原版数值)
    private static final float BASE_RADIUS = 3.25F;
    private static final float BASE_DISTANCE = 1.9F;

    // 定义每级提升的幅度 (你可以根据需要调整这两个数)
    private static final float RADIUS_PER_LEVEL = 0.75F;   // 每级半径增加 0.75
    private static final float DISTANCE_PER_LEVEL = 0.5F;  // 每级距离前推 0.5

    public CustomFlamingStrikeSpell() {
        this.defaultConfig = (new DefaultConfig())
                .setMinRarity(SpellRarity.COMMON)
                .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
                .setMaxLevel(5)
                .setCooldownSeconds(15.0)
                .build();

        this.manaCostPerLevel = 15;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 2;
        this.castTime = 10;
        this.baseManaCost = 30;
        // 移除了构造函数中固定的 attackRadius 和 attackDistance
    }

    // 辅助方法：根据等级获取当前半径
    private float getLevelRadius(int spellLevel) {
        return BASE_RADIUS + (RADIUS_PER_LEVEL * (spellLevel - 1));
    }

    // 辅助方法：根据等级获取当前距离
    private float getLevelDistance(int spellLevel) {
        return BASE_DISTANCE + (DISTANCE_PER_LEVEL * (spellLevel - 1));
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", this.getDamageText(spellLevel, caster)),
                // 可选：在提示中显示当前范围
                Component.literal(String.format("Range: %.1f", getLevelRadius(spellLevel)))
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FLAMING_STRIKE_UPSWING.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.FLAMING_STRIKE_SWING.get());
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return this.spellId;
    }

    @Override
    public boolean canBeInterrupted(@Nullable Player player) {
        return false;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return this.getCastTime(spellLevel);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // 1. 动态计算当前的半径和距离
        float currentRadius = getLevelRadius(spellLevel);
        float currentDistance = getLevelDistance(spellLevel);

        Vec3 forward = entity.getForward();

        // 2. 使用计算出的 currentDistance 确定打击中心
        Vec3 hitLocation = entity.position()
                .add(0.0, entity.getBbHeight() * 0.3, 0.0)
                .add(forward.scale(currentDistance));

        // 3. 使用计算出的 currentRadius 确定搜索范围
        List<Entity> entities = level.getEntities(entity, AABB.ofSize(hitLocation, currentRadius * 2.0, currentRadius, currentRadius * 2.0));
        SpellDamageSource damageSource = this.getDamageSource(entity);

        for (Entity targetEntity : entities) {
            if (targetEntity instanceof LivingEntity && targetEntity.isAlive() && entity.isPickable() &&
                    targetEntity.position().subtract(entity.getEyePosition()).dot(forward) >= 0.0 &&
                    // 距离判断使用 currentRadius
                    entity.distanceToSqr(targetEntity) < (double)(currentRadius * currentRadius) &&
                    Utils.hasLineOfSight(level, entity.getEyePosition(), targetEntity.getBoundingBox().getCenter(), true)) {

                Vec3 offsetVector = targetEntity.getBoundingBox().getCenter().subtract(entity.getEyePosition());
                if (offsetVector.dot(forward) >= 0.0 && DamageSources.applyDamage(targetEntity, this.getDamage(spellLevel, entity), damageSource)) {
                    MagicManager.spawnParticles(level, ParticleHelper.FIRE, targetEntity.getX(), targetEntity.getY() + (targetEntity.getBbHeight() * 0.5), targetEntity.getZ(), 30, targetEntity.getBbWidth() * 0.5, targetEntity.getBbHeight() * 0.5, targetEntity.getBbWidth() * 0.5, 0.03, false);
                    EnchantmentHelper.doPostAttackEffects((ServerLevel)level, targetEntity, damageSource);
                }
            }
        }

        // 4. 计算特效缩放比例
        // 比例 = 当前半径 / 基础(1级)半径
        float renderScale = currentRadius / BASE_RADIUS;

        boolean mirrored = playerMagicData.getCastingEquipmentSlot().equals(SpellSelectionManager.OFFHAND);

        // 5. 生成特效，传入动态 Scale
        MagicManager.spawnParticles(level,
                new FlameStrikeParticleOptions(
                        (float)forward.x,
                        (float)forward.y,
                        (float)forward.z,
                        mirrored,
                        false,
                        renderScale // 随着等级提高，这个值会变大
                ),
                hitLocation.x,
                hitLocation.y + 0.3,
                hitLocation.z,
                1, 0.0, 0.0, 0.0, 0.0, true);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public SpellDamageSource getDamageSource(@Nullable Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setFireTicks(60);
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return this.getSpellPower(spellLevel, entity) + this.getAdditionalDamage(entity);
    }

    private float getAdditionalDamage(LivingEntity entity) {
        if (entity == null) {
            return 0.0F;
        } else {
            float weaponDamage = Utils.getWeaponDamage(entity);
            ItemStack weaponItem = entity.getWeaponItem();
            if (!weaponItem.isEmpty() && weaponItem.has(DataComponents.ENCHANTMENTS)) {
                weaponDamage += (float)Utils.getEnchantmentLevel(entity.level(), Enchantments.FIRE_ASPECT, weaponItem.get(DataComponents.ENCHANTMENTS));
            }
            return weaponDamage;
        }
    }

    private String getDamageText(int spellLevel, LivingEntity entity) {
        if (entity != null) {
            float weaponDamage = Utils.getWeaponDamage(entity);
            String plus = "";
            if (weaponDamage > 0.0F) {
                plus = String.format(" (+%s)", Utils.stringTruncation(weaponDamage, 1));
            }
            String damage = Utils.stringTruncation(this.getDamage(spellLevel, entity), 1);
            return damage + plus;
        } else {
            return "" + this.getSpellPower(spellLevel, entity);
        }
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }
}