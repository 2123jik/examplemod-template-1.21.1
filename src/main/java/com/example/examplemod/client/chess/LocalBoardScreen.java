package com.example.examplemod.client.chess;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalBoardScreen extends Screen {

    private static final int SLOT_SIZE = 50;
    private static final int GAP = 4;
    private double camX = 0, camY = 0, camScale = 1.0;

    private LocalChessGame.ClientUnit draggingUnit = null;
    private int draggingFromIndex = -999;
    private ItemStack draggingItem = null;
    private int draggingItemIndex = -1;

    private boolean editMode = false;
    private String draggingUIKey = null;
    private float dragOffsetX, dragOffsetY;

    private static final String UI_RESET = "Reset Btn";
    private static final String UI_INFO = "Info Panel";
    private static final String UI_SYNERGY = "Synergy List";
    private static final String UI_SHOP = "Shop";
    private static final String UI_ITEMS = "Items";
    private static final String UI_BTN = "Battle Btn";
    private static final String UI_STREAK = "Streak Flame";
    private static final String UI_INTEREST = "Interest Slots";

    private final Map<EntityType<?>, LivingEntity> entityCache = new HashMap<>();

    public LocalBoardScreen() { super(Component.literal("Local Auto Chess")); }

    @Override
    protected void init() {
        int boardW = LocalChessGame.COL * (SLOT_SIZE + GAP);
        int boardH = LocalChessGame.ROW * (SLOT_SIZE + GAP);
        this.camX = (this.width - boardW) / 2.0;
        this.camY = (this.height - boardH) / 2.0 - 60;

        AutoChessUIConfig cfg = AutoChessUIConfig.get();
        cfg.getElement(UI_INFO, 5, 5, 250, 30);
        cfg.getElement(UI_SYNERGY, width - 100, 100, 90, 200);
        cfg.getElement(UI_SHOP, (width - 500)/2f, height - 130, 500, 130);
        cfg.getElement(UI_ITEMS, 10, height - 150, 110, 50);
        cfg.getElement(UI_BTN, width - 70, height/2f - 20, 60, 40);
        cfg.getElement(UI_RESET, width - 70, 10, 60, 20);
        cfg.getElement(UI_STREAK, 5, 250, 40, 50);
        cfg.getElement(UI_INTEREST, 5, 310, 25, 120);
    }

    @Override public void tick() { LocalChessGame.get().tick(); }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 290) { // F1
            editMode = !editMode;
            if (!editMode) AutoChessUIConfig.get().save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g, mx, my, partialTick);
        LocalChessGame game = LocalChessGame.get();

        // 1. 渲染棋盘和3D实体
        g.pose().pushPose();
        g.pose().translate(camX, camY, 0);
        g.pose().scale((float)camScale, (float)camScale, 1.0f);
        double worldMx = (mx - camX) / camScale;
        double worldMy = (my - camY) / camScale;

        if (game.getState() == LocalChessGame.State.PREPARE) {
            drawPrepareBoard(g, worldMx, worldMy);
        } else {
            drawBattleBoard(g);
        }
        g.pose().popPose();

        // 2. 渲染UI面板
        drawConfigurableUI(g, game, mx, my);

        if (editMode) drawEditOverlay(g, mx, my);

        if (!editMode) {
            // 拖拽中的单位
            if (draggingUnit != null) {
                renderUnitModel(g, mx, my, draggingUnit.def.type(), draggingUnit.star(), 0, 1.2f, draggingUnit.items);
                // 显示拖拽单位的装备槽
                drawUnitEquipmentSlots(g, mx + 30, my - 40, draggingUnit.items);

                if (my > this.height - 140) {
                    g.fillGradient(0, height - 140, width, height, 0x88FF0000, 0xAAFF0000);
                    g.drawCenteredString(font, "出售 (+" + draggingUnit.baseCost() + "$)", mx, my - 40, 0xFFFFFFFF);
                }
            }
            // 拖拽中的物品
            if (draggingItem != null) {
                g.renderItem(draggingItem, mx - 8, my - 8);
            }

            drawTooltips(g, game, mx, my, worldMx, worldMy);
        }
        g.drawString(font, "[F1] 自定义布局", width - 80, 5, 0xFF888888);
    }

    private void drawConfigurableUI(GuiGraphics g, LocalChessGame game, int mx, int my) {
        AutoChessUIConfig cfg = AutoChessUIConfig.get();

        AutoChessUIConfig.UIElement infoEl = cfg.elements.get(UI_INFO);
        if (infoEl.visible) {
            g.fill((int)infoEl.x, (int)infoEl.y, (int)(infoEl.x+infoEl.width), (int)(infoEl.y+infoEl.height), 0x88000000);
            g.renderOutline((int)infoEl.x, (int)infoEl.y, (int)infoEl.width, (int)infoEl.height, 0xFF444444);
            String txt = "金币: " + game.getGold() + " | 回合: " + game.getRound() + " | 人口: " + game.countUnitsOnBoard() + "/" + game.getLevel();
            g.drawString(font, txt, (int)infoEl.x+10, (int)infoEl.y+10, 0xFFFFFFFF);
        }

        AutoChessUIConfig.UIElement synEl = cfg.elements.get(UI_SYNERGY);
        if (synEl.visible) drawSynergyPanel(g, game, mx, my, synEl);

        AutoChessUIConfig.UIElement streakEl = cfg.elements.get(UI_STREAK);
        if (streakEl.visible) drawStreakIndicator(g, game, streakEl);

        AutoChessUIConfig.UIElement interestEl = cfg.elements.get(UI_INTEREST);
        if (interestEl.visible) drawInterestIndicators(g, game, interestEl);

        if (game.getState() == LocalChessGame.State.PREPARE) {
            AutoChessUIConfig.UIElement shopEl = cfg.elements.get(UI_SHOP);
            if (shopEl.visible) drawShop(g, game, mx, my, shopEl);
            AutoChessUIConfig.UIElement itemEl = cfg.elements.get(UI_ITEMS);
            if (itemEl.visible) drawItemBench(g, game, mx, my, itemEl);
            AutoChessUIConfig.UIElement btnEl = cfg.elements.get(UI_BTN);
            if (btnEl.visible) {
                int x = (int)btnEl.x, y = (int)btnEl.y, w = (int)btnEl.width, h = (int)btnEl.height;
                boolean hov = isHovering(mx, my, x, y, w, h);
                g.fill(x, y, x+w, y+h, hov ? 0xFF00CC00 : 0xFF008800);
                g.renderOutline(x, y, w, h, 0xFFFFFFFF);
                g.drawCenteredString(font, "战斗", x+w/2, y+16, 0xFFFFFFFF);
            }
            AutoChessUIConfig.UIElement resetEl = cfg.elements.get(UI_RESET);
            if (resetEl != null && resetEl.visible) {
                int x = (int)resetEl.x, y = (int)resetEl.y, w = (int)resetEl.width, h = (int)resetEl.height;
                boolean hov = isHovering(mx, my, x, y, w, h);
                g.fill(x, y, x+w, y+h, hov ? 0xFFAA0000 : 0xFF880000);
                g.renderOutline(x, y, w, h, 0xFFFFFFFF);
                g.drawCenteredString(font, "重置游戏", x+w/2, y+6, 0xFFFFFFFF);
            }
        }
    }

    private void drawStreakIndicator(GuiGraphics g, LocalChessGame game, AutoChessUIConfig.UIElement el) {
        int x = (int)el.x, y = (int)el.y;
        int streak = 0;
        int colorBg = 0xAA333333;
        if (game.getWinStreak() >= 2) { streak = game.getWinStreak(); colorBg = 0xAAFF3333; }
        else if (game.getLossStreak() >= 2) { streak = game.getLossStreak(); colorBg = 0xAA3333FF; }
        if (streak < 2) return;
        g.fill(x, y, x + 40, y + 40, colorBg);
        g.renderOutline(x, y, 40, 40, 0xFFFFFFFF);
        g.renderItem(new ItemStack(Items.FIRE_CHARGE), x + 12, y + 5);
        g.drawCenteredString(font, String.valueOf(streak), x + 20, y + 28, 0xFFFFFFFF);
    }

    private void drawInterestIndicators(GuiGraphics g, LocalChessGame game, AutoChessUIConfig.UIElement el) {
        int x = (int)el.x, y = (int)el.y;
        int maxInterest = 5;
        int currentInterest = Math.min(game.getGold() / 10, maxInterest);
        int slotSize = 20, gap = 2;
        g.drawString(font, "利息", x, y - 10, 0xFFE0E0E0);
        for (int i = 0; i < maxInterest; i++) {
            int currentY = y + (maxInterest - 1 - i) * (slotSize + gap);
            g.fill(x, currentY, x + slotSize, currentY + slotSize, 0x88000000);
            g.renderOutline(x, currentY, slotSize, slotSize, 0xFF444444);
            if (i < currentInterest) g.renderItem(new ItemStack(Items.GOLD_BLOCK), x + 2, currentY + 2);
        }
    }

    private void drawEditOverlay(GuiGraphics g, int mx, int my) {
        g.fillGradient(0, 0, width, height, 0x44000000, 0x44000000);
        for (AutoChessUIConfig.UIElement el : AutoChessUIConfig.get().elements.values()) {
            if (LocalChessGame.get().getState() != LocalChessGame.State.PREPARE && (el.name.equals(UI_SHOP) || el.name.equals(UI_ITEMS))) continue;
            int x = (int)el.x; int y = (int)el.y; int w = (int)el.width; int h = (int)el.height;
            boolean hover = isHovering(mx, my, x, y, w, h);
            int color = hover ? 0xFFFFFF00 : (el.visible ? 0xFF00FF00 : 0xFFFF0000);
            g.renderOutline(x, y, w, h, color);
            g.fill(x, y, x+w, y+h, (color & 0x00FFFFFF) | 0x44000000);
            g.drawString(font, el.name, x+2, y+2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (editMode) {
            for (AutoChessUIConfig.UIElement el : AutoChessUIConfig.get().elements.values()) {
                if (LocalChessGame.get().getState() != LocalChessGame.State.PREPARE && (el.name.equals(UI_SHOP) || el.name.equals(UI_ITEMS))) continue;
                if (isHovering(mx, my, (int)el.x, (int)el.y, (int)el.width, (int)el.height)) {
                    if (btn == 0) { draggingUIKey = el.name; dragOffsetX = (float)mx - el.x; dragOffsetY = (float)my - el.y; return true; }
                    else if (btn == 1) { el.visible = !el.visible; return true; }
                }
            }
            return super.mouseClicked(mx, my, btn);
        }
        LocalChessGame game = LocalChessGame.get();
        AutoChessUIConfig cfg = AutoChessUIConfig.get();
        if (game.getState() == LocalChessGame.State.PREPARE) {
            AutoChessUIConfig.UIElement shopEl = cfg.elements.get(UI_SHOP);
            if (shopEl.visible && isHovering(mx, my, (int)shopEl.x, (int)shopEl.y, (int)shopEl.width, (int)shopEl.height)) {
                handleShopClick(game, mx, my, shopEl); return true;
            }
            AutoChessUIConfig.UIElement btnEl = cfg.elements.get(UI_BTN);
            if (btnEl.visible && isHovering(mx, my, (int)btnEl.x, (int)btnEl.y, (int)btnEl.width, (int)btnEl.height)) {
                game.startBattle(); return true;
            }
            AutoChessUIConfig.UIElement itemEl = cfg.elements.get(UI_ITEMS);
            if (itemEl.visible) {
                int idx = getHoveredItemSlot(mx, my, itemEl);
                if (idx != -1 && idx < game.itemBench.size()) { draggingItem = game.itemBench.get(idx); draggingItemIndex = idx; return true; }
            }
            AutoChessUIConfig.UIElement resetEl = cfg.elements.get(UI_RESET);
            if (resetEl != null && resetEl.visible && isHovering(mx, my, (int)resetEl.x, (int)resetEl.y, (int)resetEl.width, (int)resetEl.height)) {
                if (Screen.hasShiftDown()) game.restartGame();
                else Minecraft.getInstance().player.displayClientMessage(Component.literal("§c请按住 Shift 点击以确认重置游戏！"), true);
                return true;
            }
            double wx = (mx - camX) / camScale; double wy = (my - camY) / camScale;
            int slot = getHoveredSlot(wx, wy);
            if (slot != -999 && game.getUnit(slot) != null) { draggingUnit = game.getUnit(slot); draggingFromIndex = slot; return true; }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (editMode && draggingUIKey != null) {
            for (AutoChessUIConfig.UIElement e : AutoChessUIConfig.get().elements.values()) {
                if (e.name.equals(draggingUIKey)) { e.x = (float)mx - dragOffsetX; e.y = (float)my - dragOffsetY; break; }
            }
            return true;
        }
        if (button == 1 || button == 2) { camX += dx; camY += dy; return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (editMode) { draggingUIKey = null; return true; }
        if (draggingUnit != null) {
            AutoChessUIConfig.UIElement shopEl = AutoChessUIConfig.get().elements.get(UI_SHOP);
            if ((shopEl.visible && isHovering(mx, my, (int)shopEl.x, (int)shopEl.y, (int)shopEl.width, (int)shopEl.height)) || my > height - 100) {
                LocalChessGame.get().sellUnit(draggingFromIndex);
            }
            else {
                double wx = (mx - camX) / camScale; double wy = (my - camY) / camScale;
                int to = getHoveredSlot(wx, wy); if (to != -999) LocalChessGame.get().moveUnit(draggingFromIndex, to);
            }
            draggingUnit = null; draggingFromIndex = -999; return true;
        }
        if (draggingItem != null) {
            double wx = (mx - camX) / camScale; double wy = (my - camY) / camScale;
            int to = getHoveredSlot(wx, wy);
            if (to != -999) { LocalChessGame.get().itemBench.remove(draggingItemIndex); LocalChessGame.get().equipUnit(to, draggingItem); }
            draggingItem = null; draggingItemIndex = -1; return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double oldScale = camScale; camScale += scrollY * 0.1; camScale = Mth.clamp(camScale, 0.5, 3.0);
        double worldX = (mouseX - camX) / oldScale; double worldY = (mouseY - camY) / oldScale;
        camX = mouseX - worldX * camScale; camY = mouseY - worldY * camScale; return true;
    }

    private void drawSynergyPanel(GuiGraphics g, LocalChessGame game, int mx, int my, AutoChessUIConfig.UIElement el) {
        int x = (int)el.x, y = (int)el.y, w = (int)el.width, h = (int)el.height;
        g.fill(x, y, x+w, y+h, 0x88000000); g.renderOutline(x, y, w, h, 0xFF444444);
        g.drawString(font, "羁绊", x+5, y+5, 0xFFFFFFFF);
        int ly = y + 18;
        for(LocalChessGame.SynergyStatus s : game.getSynergyReport()) {
            if(ly+10 > y+h) break;
            g.drawString(font, s.synergy().name(), x+5, ly, s.isActive()?0xFF00FF00:0xFFAAAAAA);
            g.drawString(font, s.count() + s.getNextThreshold(), x+w-30, ly, 0xFFFFFFFF);
            ly+=14;
        }
    }

    private void drawShop(GuiGraphics g, LocalChessGame game, int mx, int my, AutoChessUIConfig.UIElement el) {
        int x = (int)el.x, y = (int)el.y, w = (int)el.width, h = (int)el.height;
        g.fillGradient(x, y, x+w, y+h, 0xEE111111, 0xEE222222); g.renderOutline(x, y, w, h, 0xFF666666);
        int cardW = 80, gap = 5, sx = x + 10, cy = y + 10;
        LocalChessGame.ShopCard[] cards = game.getShop();
        for(int i=0; i<5; i++) {
            int cx = sx + i*(cardW+gap);
            LocalChessGame.ShopCard c = cards[i];
            if(c != null) {
                boolean hov = !editMode && isHovering(mx, my, cx, cy, cardW, 100);
                g.fill(cx, cy, cx+cardW, cy+100, hov?0xFF444444:0xFF222222);
                g.renderOutline(cx, cy, cardW, 100, game.getRarityColor(c.def().cost()));
                renderUnitModel(g, cx+cardW/2, cy+85, c.def().type(), 1, 0, 1.2f, null);
                g.drawString(font, c.def().name(), cx+4, cy+4, game.getRarityColor(c.def().cost()));
                g.drawString(font, "$"+c.def().cost(), cx+4, cy+14, 0xFFFFD700);
                g.drawString(font, c.def().trait().split("/")[0], cx+4, cy+24, 0xFFAAAAAA);
            } else g.renderOutline(cx, cy, cardW, 100, 0xFF333333);
        }
        g.drawString(font, "XP $4", x+w-60, y+20, 0xFFFFFFFF);
        g.drawString(font, "Roll $2", x+w-60, y+50, 0xFFFFFFFF);
        g.drawString(font, game.isShopLocked()?"Lock":"Unlock", x+w-60, y+80, 0xFFAAAAAA);
    }

    private void handleShopClick(LocalChessGame game, double mx, double my, AutoChessUIConfig.UIElement el) {
        int cardW = 80, gap = 5, sx = (int)el.x + 10, cy = (int)el.y + 10;
        for(int i=0; i<5; i++) { if(isHovering(mx, my, sx + i*(cardW+gap), cy, cardW, 100)) { game.buyUnit(i); return; } }
        int fx = (int)(el.x + el.width - 60);
        if(isHovering(mx, my, fx, (int)el.y+20, 50, 10)) game.buyXp();
        else if(isHovering(mx, my, fx, (int)el.y+50, 50, 10)) game.rollShop();
        else if(isHovering(mx, my, fx, (int)el.y+80, 50, 10)) game.toggleLock();
    }

    private void drawItemBench(GuiGraphics g, LocalChessGame game, int mx, int my, AutoChessUIConfig.UIElement el) {
        int sx = (int)el.x, sy = (int)el.y;
        g.drawString(font, "仓库", sx, sy-10, 0xFFEEEEEE);
        for(int i=0; i<10; i++) {
            int x = sx + (i%5)*22, y = sy + (i/5)*22;
            g.fill(x, y, x+20, y+20, 0x88000000); g.renderOutline(x, y, 20, 20, 0xFF555555);
            if(i < game.itemBench.size() && i != draggingItemIndex) g.renderItem(game.itemBench.get(i), x+2, y+2);
        }
    }

    private int getHoveredItemSlot(double mx, double my, AutoChessUIConfig.UIElement el) {
        int sx = (int)el.x, sy = (int)el.y;
        for(int i=0; i<10; i++) {
            int x = sx + (i%5)*22, y = sy + (i/5)*22; if(isHovering(mx, my, x, y, 20, 20)) return i;
        }
        return -1;
    }

    private void drawTooltips(GuiGraphics g, LocalChessGame game, int mx, int my, double wx, double wy) {
        int slot = getHoveredSlot(wx, wy);
        if (slot != -999) {
            LocalChessGame.ClientUnit u = game.getUnit(slot);
            if (u != null) {
                List<Component> tips = new ArrayList<>();
                LocalChessGame.UnitDefinition def = u.def;
                tips.add(Component.literal(def.name() + " (" + u.star() + "★)").withStyle(net.minecraft.ChatFormatting.BOLD));
                tips.add(Component.literal("Cost: " + def.cost()).withStyle(net.minecraft.ChatFormatting.GOLD));
                tips.add(Component.literal("羁绊: " + def.trait()).withStyle(net.minecraft.ChatFormatting.AQUA));
                
                // 绘制装备槽提示
                if (!u.items.isEmpty()) {
                    tips.add(Component.literal("装备:").withStyle(net.minecraft.ChatFormatting.WHITE));
                    for(ItemStack s : u.items) tips.add(Component.literal(" - " + s.getHoverName().getString()).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                
                g.renderComponentTooltip(font, tips, mx, my);
            }
        }
        AutoChessUIConfig.UIElement itemEl = AutoChessUIConfig.get().elements.get(UI_ITEMS);
        if (itemEl.visible) {
            int idx = getHoveredItemSlot(mx, my, itemEl);
            if (idx != -1 && idx < game.itemBench.size()) g.renderTooltip(font, game.itemBench.get(idx), mx, my);
        }
    }

    private void drawPrepareBoard(GuiGraphics g, double wx, double wy) {
        for (int r = 0; r < LocalChessGame.ROW; r++) for (int c = 0; c < LocalChessGame.COL; c++)
            drawSingleSlot(g, wx, wy, c * (SLOT_SIZE + GAP), r * (SLOT_SIZE + GAP), r * LocalChessGame.COL + c);
        int benchY = (LocalChessGame.ROW * (SLOT_SIZE + GAP)) + 30;
        for (int i = 0; i < 9; i++) drawSingleSlot(g, wx, wy, (i - 1) * (SLOT_SIZE + GAP), benchY, -1 - i);
    }

    private void drawBattleBoard(GuiGraphics g) {
        int w = LocalChessGame.COL * (SLOT_SIZE + GAP), h = LocalChessGame.ROW * (SLOT_SIZE + GAP);
        g.fill(0, 0, w, h, 0x44000000); g.renderOutline(0, 0, w, h, 0xFF555555);
        for(LocalChessGame.Projectile p : LocalChessGame.get().projectiles) {
            float cx = p.ex * (SLOT_SIZE + GAP) + SLOT_SIZE/2, cz = p.ez * (SLOT_SIZE + GAP) + SLOT_SIZE/2;
            g.fill((int)cx-2, (int)cz-2, (int)cx+2, (int)cz+2, 0xFFFF0000);
        }
    }

    private void drawSingleSlot(GuiGraphics g, double wx, double wy, int x, int y, int index) {
        boolean hover = isHovering(wx, wy, x, y, SLOT_SIZE, SLOT_SIZE);
        g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hover ? 0xAA666666 : (index >= 0 && ((index % LocalChessGame.COL) + (index / LocalChessGame.COL)) % 2 == 0 ? 0x88444444 : 0x88222222));
        g.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xFF666666);
        LocalChessGame.ClientUnit u = LocalChessGame.get().getUnit(index);
        if (u != null && index != draggingFromIndex) {
            renderUnitModel(g, x + SLOT_SIZE/2, y + SLOT_SIZE - 5, u.def.type(), u.star(), 0, 1.0f, u.items);
            drawUnitEquipmentSlots(g, x + SLOT_SIZE - 10, y + 2, u.items);
            g.drawCenteredString(font, "★".repeat(u.star()), x+SLOT_SIZE/2, y-10, 0xFFFFD700);
        }
    }

    // ★★★ 新增：绘制单位旁的装备槽 ★★★
    private void drawUnitEquipmentSlots(GuiGraphics g, double x, double y, List<ItemStack> items) {
        int slotSize = 10;
        int gap = 1;
        for (int i = 0; i < 3; i++) { // 最多显示3个槽位
            int dx = (int)x;
            int dy = (int)y + i * (slotSize + gap);
            
            // 绘制槽位背景
            g.fill(dx, dy, dx + slotSize, dy + slotSize, 0x88000000);
            g.renderOutline(dx, dy, slotSize, slotSize, 0xFF555555);

            // 如果有物品，渲染物品
            if (i < items.size()) {
                ItemStack stack = items.get(i);
                g.pose().pushPose();
                g.pose().translate(dx + 1, dy + 1, 200); // Z轴提升防止遮挡
                g.pose().scale(0.5f, 0.5f, 1f); // 缩小物品图标以适应小槽位
                g.renderItem(stack, 0, 0);
                g.pose().popPose();
            }
        }
    }

    private void renderUnitModel(GuiGraphics g, int x, int y, EntityType<?> type, int star, float rotation, float scale, List<ItemStack> items) {
        LivingEntity entity = entityCache.computeIfAbsent(type, t -> (LivingEntity) t.create(Minecraft.getInstance().level));
        if (entity == null) return;
        
        // 重置状态
        entity.yBodyRot = 0; entity.yHeadRot = 0; entity.setYRot(0); entity.setXRot(0);
        entity.deathTime = 0; entity.hurtTime = 0; entity.attackAnim = 0; entity.walkAnimation.update(0.0f, 0.0f);
        
        // 清空装备
        for(EquipmentSlot slot : EquipmentSlot.values()) {
            entity.setItemSlot(slot, ItemStack.EMPTY);
        }

        // ★★★ 核心改动：自动穿戴装备 ★★★
        if (items != null) {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    // 使用原版逻辑判断物品属于哪个槽位 (头/胸/腿/脚/主手/副手)
                    EquipmentSlot slot = entity.getEquipmentSlotForItem(stack);
                    entity.setItemSlot(slot, stack);
                }
            }
        }

        g.pose().pushPose(); 
        g.pose().translate(x, y, 100); 
        float s = SLOT_SIZE * 0.5f * scale; 
        g.pose().scale(s, s, -s);
        Quaternionf q = new Quaternionf().rotateZ((float)Math.PI).mul(new Quaternionf().rotateX((float)(-Math.PI / 6))).mul(new Quaternionf().rotateY((float)Math.toRadians(30 + rotation)));
        g.pose().mulPose(q);
        RenderSystem.setShaderLights(new Vector3f(-0.2f, 1.0f, -1.0f), new Vector3f(0.2f, -1.0f, 0.0f));
        Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(false);
        Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0, 0, 0, 0, 1, g.pose(), g.bufferSource(), 15728880);
        g.flush();
        Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(true); 
        g.pose().popPose();
    }
    
    private int getHoveredSlot(double wx, double wy) {
        for (int r = 0; r < LocalChessGame.ROW; r++) for (int c = 0; c < LocalChessGame.COL; c++)
            if (isHovering(wx, wy, c * (SLOT_SIZE + GAP), r * (SLOT_SIZE + GAP), SLOT_SIZE, SLOT_SIZE)) return r * LocalChessGame.COL + c;
        int benchY = (LocalChessGame.ROW * (SLOT_SIZE + GAP)) + 30;
        for (int i = 0; i < 9; i++) if (isHovering(wx, wy, (i - 1) * (SLOT_SIZE + GAP), benchY, SLOT_SIZE, SLOT_SIZE)) return -1 - i;
        return -999;
    }
    private boolean isHovering(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    @Override public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}