package com.example.examplemod.server.menu;

import com.example.examplemod.register.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// TradeMenu.java
public class TradeMenu extends AbstractContainerMenu {
    // 0-8: 发起者(Host)的物品, 9-17: 接受者(Guest)的物品
    private final Container tradeInventory;
    private final ContainerData data; // 用于同步锁定状态 [0]:HostLock, [1]:GuestLock
    private final boolean isHost; // 当前客户端/玩家是否是发起者
    public TradeMenu(int containerId, Inventory playerInv, Container tradeInv, ContainerData data, boolean isHost) {
        super(ModMenus.TRADE_MENU.get(), containerId);
        this.tradeInventory = tradeInv;
        this.data = data;
        this.isHost = isHost;

        checkContainerSize(tradeInv, 18);
        checkContainerDataCount(data, 2);

        // 绑定交易区
        addTradeSlots();
        // 绑定玩家背包
        addPlayerInventory(playerInv);
        // 绑定数据同步 (锁定状态)
        addDataSlots(data);
    }
    // 构造函数 (客户端调用)
    public TradeMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, new SimpleContainer(18), new SimpleContainerData(2), extraData.readBoolean());
    }
    @Override
    public void slotsChanged(Container pContainer) {
        // 如果变动的容器是交易容器（而不是玩家自己的背包）
        if (pContainer == this.tradeInventory) {
            this.resetLocks();
        }
        super.slotsChanged(pContainer);
    }

    private void resetLocks() {
        this.data.set(0, 0); // Reset Host
        this.data.set(1, 0); // Reset Guest
    }

    public void setPlayerLocked(Player player, boolean locked) {
        // 简单判定：如果我有权限拿取 Slot 0，我就是 Host
        boolean playerIsHost = this.getSlot(0).mayPickup(player);
        this.data.set(playerIsHost ? 0 : 1, locked ? 1 : 0);

        // 检查是否双方都锁定了
        if (this.data.get(0) == 1 && this.data.get(1) == 1) {
            if (completeCallback != null) completeCallback.run();
        }
    }

    private void addTradeSlots() {
        // Host区域
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(tradeInventory, i, 8 + i * 18, 20) {
                @Override public boolean mayPickup(Player p) { return isHost; }
                @Override public boolean mayPlace(ItemStack s) { return isHost; }
            });
        }
        // Guest区域
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(tradeInventory, i + 9, 8 + i * 18, 50) {
                @Override public boolean mayPickup(Player p) { return !isHost; }
                @Override public boolean mayPlace(ItemStack s) { return !isHost; }
            });
        }
    }

    private void addPlayerInventory(Inventory playerInv) {
        for (int l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInv, k + l * 9 + 9, 8 + k * 18, 84 + l * 18));
            }
        }
        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(playerInv, i1, 8 + i1 * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        // 必须实现，否则Shift点击会崩溃或丢物品，这里简化返回空
        return ItemStack.EMPTY;
    }

    private void tryExecuteTrade(Level level) {
        if (level.isClientSide) return;

        boolean hostLocked = this.data.get(0) == 1;
        boolean guestLocked = this.data.get(1) == 1;

        if (hostLocked && guestLocked) {
            completeTrade(level);
        }
    }

    private void completeTrade(Level level) {
        // 执行交换逻辑
        // 1. 获取参与者（这里需要外部 Session 管理器支持，或者通过 Inventory 反推）
        // 为简化代码，假设我们有办法获取到两个 Player 实体 (通常在 openMenu 时传入并存储在 Menu 中)
        // 此处仅展示物品逻辑：

        // 逻辑：将 0-8 的物品给 Guest，将 9-17 的物品给 Host
        // 实现略微复杂，因为要把物品塞进背包，塞不下要掉落
        // 这里必须由 Server 端的 Manager 控制，因为 Menu 可能丢失玩家引用

        // 发送事件或回调给 Manager 来处理实际转移，然后关闭 Menu
        if (this.completeCallback != null) this.completeCallback.run();
    }

    public Runnable completeCallback; // 由打开 Menu 的地方注入

    @Override
    public boolean stillValid(Player player) {
        return true; // 实际应判断距离
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 安全回退：界面关闭时，将属于该玩家的输入物品退回背包
        if (!player.level().isClientSide) {
            // 如果我是 Host，退回 0-8；如果我是 Guest，退回 9-17
            // 或者更暴力的：全部退回（谁捡到算谁的，但这不安全）。
            // 正确做法：
            boolean pIsHost = this.getSlot(0).mayPickup(player);
            int start = pIsHost ? 0 : 9;
            int end = pIsHost ? 9 : 18;

            for (int i = start; i < end; i++) {
                ItemStack stack = this.tradeInventory.getItem(i);
                if (!stack.isEmpty()) {
                    if (player.isAlive()) {
                        player.getInventory().placeItemBackInInventory(stack);
                    } else {
                        player.drop(stack, false);
                    }
                    this.tradeInventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }


    // Getter 用于 GUI 渲染
    public boolean getHostLocked() { return data.get(0) == 1; }
    public boolean getGuestLocked() { return data.get(1) == 1; }
}