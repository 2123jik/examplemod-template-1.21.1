package com.example.examplemod.traits;

import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // 记得导入这个
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import com.example.examplemod.ExampleMod; // 导入你的主类以获取 MODID

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

import static com.example.examplemod.ExampleMod.modrl;

public class ThunderStrikeTrait extends MobTrait {

    // 1. 定义一个静态 ID 常量 (必须和 ModTraits 里注册的名字一致)
    public static final ResourceLocation ID = modrl("thunder_strike");

    private final IntSupplier interval;

    public ThunderStrikeTrait(ChatFormatting format, IntSupplier interval) {
        super(format);
        this.interval = interval;
    }

    // 2. 【核心修复】重写 getRegistryName 方法
    // 强制返回我们定义的 ID，防止父类 NamedEntry 查找注册表失败导致崩溃
    @Override
    public ResourceLocation getRegistryName() {
        return ID;
    }

    // 3. 【核心修复】手动指定配置 (之前提到过，再次确认加上)
    // 因为 generic 注册不包含 Config，如果不加这三个方法，词条可能不会生效
    @Override
    public int getCost(RegistryAccess access, double factor) {
        return 20; // 花费
    }

    @Override
    public int getMaxLevel(RegistryAccess access) {
        return 5; // 最大等级
    }

    @Override
    public boolean allow(LivingEntity le, int difficulty, int maxModLv) {
        return true; // 允许所有符合条件的生成
    }

    @Override
    public void tick(LivingEntity e, int level) {
        if (!e.level().isClientSide()) {
            if (e instanceof Mob mob) {
                Optional<MobTraitCap> opt = ((GeneralCapabilityHolder) LHMiscs.MOB.type()).getExisting(e);

                if (opt.isPresent()) {
                    // 这里调用 this.getRegistryName() 现在会直接返回常量，非常安全
                    Data data = (Data) opt.get().getOrCreateData(this.getRegistryName(), Data::new);

                    ++data.tickCount;
                    int currentInterval = this.interval.getAsInt();

                    if (data.tickCount >= currentInterval) {
                        LivingEntity target = mob.getTarget();
                        if (target != null && target.isAlive()) {
                            data.tickCount = 0;
                            spawnLightning(mob.level(), target.blockPosition());
                        }
                    }
                }
            }
        }
    }

    private void spawnLightning(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (lightning != null) {
                lightning.moveTo(pos.getX(), pos.getY(), pos.getZ());
                lightning.setVisualOnly(false);
                serverLevel.addFreshEntity(lightning);
            }
        }
    }

    @Override
    public void addDetail(RegistryAccess access, List<Component> list) {
        String key = this.getDescriptionId() + ".desc";
        double seconds = (double) this.interval.getAsInt() / 20.0F;
        list.add(Component.translatable(key,
                Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.GRAY));
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialField
        public int tickCount;

        public Data() {
        }
    }
}