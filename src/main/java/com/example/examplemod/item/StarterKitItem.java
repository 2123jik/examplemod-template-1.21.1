package com.example.examplemod.item;

import com.example.examplemod.server.util.ServerEventUtils;
import dev.xkmc.l2backpack.init.data.LBConfig;
import dev.xkmc.l2backpack.init.registrate.LBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import static com.example.examplemod.component.ModDataComponents.MAKEN_ARMOR;
import static com.example.examplemod.component.ModDataComponents.MAKEN_SWORD;
import static fuzs.enderzoology.init.ModRegistry.SOULBOUND_ENCHANTMENT;

public class StarterKitItem extends Item {

    public StarterKitItem(Properties properties) {
        super(properties);
    }
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 60;
    }
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        if (!(entityLiving instanceof Player player)) {
            return stack;
        }

        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;

            // 1. 发放装备
            givePlayerStarterGear(player, serverLevel);

            // 2. 传送逻辑
            teleportToNearestVillage(player, serverLevel);

            // 3. 通知并消耗物品
            player.displayClientMessage(Component.literal("§a新手礼包已成功开启！"), true);

            // 消耗掉这一个物品 (如果是创造模式可能不会消耗，加个判断)
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    private void givePlayerStarterGear(Player player, Level level) {
        // 使用 addItem 如果背包满了会自动掉落，比直接 getInventory().add 更安全
        player.addItem(createStarterArmor(new ItemStack(Items.NETHERITE_HELMET), "Limitless Helmet", level));
        player.addItem(createStarterArmor(new ItemStack(Items.NETHERITE_CHESTPLATE), "Limitless Chestplate", level));
        player.addItem(createStarterArmor(new ItemStack(Items.NETHERITE_LEGGINGS), "Limitless Leggings", level));
        player.addItem(createStarterArmor(new ItemStack(Items.NETHERITE_BOOTS), "Limitless Boots", level));

        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        ServerEventUtils.setEnchant(sword, level, Enchantments.SHARPNESS, 6);
        ServerEventUtils.setEnchant(sword, level, Enchantments.UNBREAKING, 4);
        ServerEventUtils.setEnchant(sword, level, Enchantments.MENDING, 1);
        sword.set(MAKEN_SWORD.get(), 1.0D);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Maken Sword"));
        ServerEventUtils.setApotheosisMythicRarity(sword);
        player.addItem(sword);

        ItemStack recoveryCompass = new ItemStack(Items.RECOVERY_COMPASS);
        // 注意：需确保 ServerEventUtils 能够处理 Enchantment 查找
        recoveryCompass.enchant(ServerEventUtils.getHolder(SOULBOUND_ENCHANTMENT, level), 6);
        int initialRow =  LBConfig.SERVER.initialRows.get();
        ItemStack stack = LBItems.DC_ROW.set(LBItems.BACKPACKS[DyeColor.WHITE.ordinal()].asStack(), initialRow);
        player.addItem(stack);

        player.addItem(recoveryCompass);
        player.addItem(new ItemStack(Items.ENDER_PEARL.asItem(), 5));
    }


    private ItemStack createStarterArmor(ItemStack armor, String name, Level level) {
        ServerEventUtils.setEnchant(armor, level, Enchantments.PROTECTION, 5);
        ServerEventUtils.setEnchant(armor, level, Enchantments.UNBREAKING, 4);
        ServerEventUtils.setEnchant(armor, level, Enchantments.MENDING, 1);
        armor.set(MAKEN_ARMOR.get(), 1.0D);
        armor.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        ServerEventUtils.setApotheosisMythicRarity(armor);
        return armor;
    }

    private void teleportToNearestVillage(Player player, ServerLevel level) {
        // 寻找最近的村庄
        BlockPos nearestVillage = level.findNearestMapStructure(StructureTags.VILLAGE, player.getOnPos(), 5000, false);
        
        if (nearestVillage != null) {
            int x = nearestVillage.getX();
            int z = nearestVillage.getZ();
            
            // 加载区块以确保安全
            level.getChunk(x, z);
            
            int maxY = level.getMaxBuildHeight();
            int minY = level.getMinBuildHeight();
            BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(x, maxY, z);

            // 从最高点向下扫描寻找地面
            while (targetPos.getY() > minY && level.getBlockState(targetPos).isAir()) {
                targetPos.move(Direction.DOWN);
            }
            
            // 传送到地面上方一格
            player.teleportTo(x, targetPos.getY() + 1, z);
            player.sendSystemMessage(Component.literal("§e已将你传送至最近的村庄。"));
        } else {
            player.sendSystemMessage(Component.literal("§c未在附近找到村庄，仅发放了物品。"));
        }
    }
}