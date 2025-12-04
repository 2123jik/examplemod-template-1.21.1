package com.example.examplemod.entity;

import com.example.examplemod.register.ModEntities;
import com.example.examplemod.util.ClientColorUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class SwordProjectileEntity extends AbstractArrow {
    // 同步数据：存储这把剑的具体 ItemStack
    private static final EntityDataAccessor<ItemStack> SWORD_ITEM = SynchedEntityData.defineId(SwordProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

    // 全局缓存列表：只在内存里存一份，不用每次都扫描
    private static final List<ItemStack> GLOBAL_SWORD_CACHE = new ArrayList<>();

    public SwordProjectileEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public SwordProjectileEntity(EntityType<? extends AbstractArrow> type, Level level, double x, double y, double z) {
        super(type, x, y, z, level, ItemStack.EMPTY, null); // 1.21+ 构造函数可能有变化，根据你的映射表调整
        this.pickup = Pickup.DISALLOWED;

        // 只有在服务端才需要选剑
        if (!level.isClientSide) {
            initializeCacheIfNeeded(); // 确保缓存已加载
            this.setSwordItem(getRandomSword());
        }
    }

    // 如果你用的是旧版构造逻辑，这里保留一份方便你直接调用
    public SwordProjectileEntity(Level level, double x, double y, double z) {

        super(ModEntities.SWORD_PROJECTILE.get(), x, y, z, level, ItemStack.EMPTY, null);

        this.pickup = Pickup.DISALLOWED;

        if (!level.isClientSide) {
            initializeCacheIfNeeded();
            this.setSwordItem(getRandomSword());
        }
    }

    /**
     * 核心逻辑：扫描整个注册表
     */
    private static void initializeCacheIfNeeded() {
        if (!GLOBAL_SWORD_CACHE.isEmpty()) return;

        System.out.println("Gate of Babylon: Scanning for HIGH DAMAGE weapons...");

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;

            // --- 筛选逻辑开始 ---
            boolean validWeapon = false;

            // 1. 类型白名单 (剑、三叉戟必然入选)
            if (item instanceof SwordItem || item instanceof TridentItem) {
                validWeapon = true;
            }
            // 2. 斧头 (必然入选)
            else if (item instanceof AxeItem) {
                validWeapon = true;
            }
            // 3. 模组武器漏网之鱼检测 (简单的名字检测，或者是 TieredItem)
            else if (item instanceof net.minecraft.world.item.TieredItem) {
                // 这里可以做得更细，比如排除掉 PickaxeItem 和 ShovelItem
                // 除非你想让“王”扔出钻石铲子... 其实也不是不行，这很羞辱人
                validWeapon = true;
            }

            // --- 筛选逻辑结束 ---

            if (validWeapon) {
                GLOBAL_SWORD_CACHE.add(new ItemStack(item));
            }
        }

        if (GLOBAL_SWORD_CACHE.isEmpty()) {
            GLOBAL_SWORD_CACHE.add(new ItemStack(Items.IRON_SWORD));
        }

        System.out.println("Gate of Babylon: Loaded " + GLOBAL_SWORD_CACHE.size() + " items.");
    }

    private ItemStack getRandomSword() {
        if (GLOBAL_SWORD_CACHE.isEmpty()) return new ItemStack(Items.IRON_SWORD);
        return GLOBAL_SWORD_CACHE.get(this.random.nextInt(GLOBAL_SWORD_CACHE.size())).copy();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // 默认值给个空或者铁剑
        builder.define(SWORD_ITEM, new ItemStack(Items.IRON_SWORD));
    }

    public void setSwordItem(ItemStack stack) {
        this.entityData.set(SWORD_ITEM, stack);
    }

    public ItemStack getSwordItem() {
        return this.entityData.get(SWORD_ITEM);
    }

    // --- 下面是防止拾取的标准逻辑 ---

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 100) { // 插在地上5秒就消失，既然是宝具，回收要快
            this.discard();
        }
        super.tick();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }
    private int clientCachedColor = -1; // -1 表示未计算
    private ItemStack lastRenderedStack = ItemStack.EMPTY;

    // 获取颜色的方法 (供 Renderer 调用)
    public int getDominantColor() {
        // 严防服务端崩溃：由服务端逻辑调用时直接返回白色
        if (!this.level().isClientSide) {
            return 0xFFFFFF;
        }

        ItemStack currentStack = this.getSwordItem();

        // 如果是第一次运行，或者物品发生了变化 (比如通过命令变了)
        if (this.clientCachedColor == -1 || !ItemStack.matches(this.lastRenderedStack, currentStack)) {
            // 更新缓存的物品
            this.lastRenderedStack = currentStack.copy();

            // 调用工具类计算颜色 (这是一个耗时操作，所以我们只做一次)
            // 注意：这里调用了一个单独的工具类，防止类加载问题
            this.clientCachedColor = ClientColorUtils.getItemColor(currentStack);
        }

        return this.clientCachedColor;
    }
}