package com.example.examplemod.client.screen;

import com.example.examplemod.network.TradeActionPayload;
import com.example.examplemod.server.menu.TradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

// TradeScreen.java
public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private Button lockButton;

    public TradeScreen(TradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 添加锁定按钮
        int x = this.leftPos + 120; // 调整坐标
        int y = this.topPos + 35;
        
        this.lockButton = Button.builder(Component.literal("Lock"), button -> {
            // 发送网络包
            boolean nextState = !getMyLockState();
            PacketDistributor.sendToServer(new TradeActionPayload(nextState));
        }).bounds(x, y, 40, 20).build();

        this.addRenderableWidget(lockButton);
    }

    private boolean getMyLockState() {
        // 判断我是左边(Host)还是右边(Guest)
        // 这里的判断逻辑需要和 Menu 中的 isHost 一致
        // 为方便，假设 Menu 暴露了 isHost 字段
        // (需要在 Menu 构造时保存 isHost)
        // return menu.isHost ? menu.getHostLocked() : menu.getGuestLocked();
        // 实际上 Menu 中可以通过 Slot 权限判断
        return menu.getSlot(0).mayPickup(this.minecraft.player) ? menu.getHostLocked() : menu.getGuestLocked();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 绘制背景 (使用原版 Dispenser 或自定义贴图)
        // guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, ...);
        
        // 简单的颜色指示
        // 绘制 Host 状态指示灯
        int colorHost = menu.getHostLocked() ? 0xFF00FF00 : 0xFFFF0000;
        guiGraphics.fill(leftPos + 10, topPos + 10, leftPos + 20, topPos + 20, colorHost);
        
        // 绘制 Guest 状态指示灯
        int colorGuest = menu.getGuestLocked() ? 0xFF00FF00 : 0xFFFF0000;
        guiGraphics.fill(leftPos + 10, topPos + 40, leftPos + 20, topPos + 50, colorGuest);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick); // 1.21 新写法
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // 更新按钮文字
        this.lockButton.setMessage(Component.literal(getMyLockState() ? "Unlock" : "Lock"));
    }
}