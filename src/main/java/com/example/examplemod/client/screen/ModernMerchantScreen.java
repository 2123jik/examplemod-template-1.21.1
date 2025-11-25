package com.example.examplemod.client.screen;

import com.example.examplemod.mixin.SlotAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLPaths; // 确保引用了 FMLPaths 用于获取 config 目录

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ModernMerchantScreen extends AbstractContainerScreen<MerchantMenu> {

    // --- 颜色常量 ---
    private static final int COL_BG_MAIN = 0xFF010A13;
    private static final int COL_BG_PANEL = 0xFF091416;
    private static final int COL_BORDER_GOLD = 0xFF785A28;
    private static final int COL_BORDER_BRIGHT = 0xFFC8AA6E;
    private static final int COL_BORDER_EDIT = 0xFF00FF00;
    private static final int COL_HANDLE_EDIT = 0xFFFF0000;
    private static final int COL_TEXT_GOLD = 0xFFF0E6D2;
    private static final int COL_TEXT_BLUE = 0xFF0ACBE6;
    private static final int COL_PRICE_RED = 0xFFFF4444;

    // --- 布局系统 ---
    private Map<String, LayoutBox> layoutMap = new HashMap<>();
    private boolean isEditMode = false;

    // 文件保存路径
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("merchant_layout.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 编辑状态
    private LayoutBox activeBox = null;
    private EditAction currentAction = EditAction.NONE;
    private int dragOffsetX, dragOffsetY;
    private int resizeStartW, resizeStartH;
    private static final int RESIZE_HANDLE_SIZE = 10;

    // --- 组件引用 ---
    private PurchaseButton purchaseButton;
    private Button editToggleButton;

    // --- 状态 ---
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int gridCols = 4;
    private static final int GRID_ITEM_SIZE = 24;
    private static final int GRID_GAP = 2;

    private enum EditAction {
        NONE, DRAG, RESIZE
    }

    public ModernMerchantScreen(MerchantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 280;
        this.imageHeight = 200;
        this.inventoryLabelY = -10000;
        this.titleLabelY = -10000;

        // 1. 先设置默认值
        setupDefaultLayout();
    }

    private void setupDefaultLayout() {
        layoutMap.put("WINDOW", new LayoutBox(0, 0, 280, 200));
        layoutMap.put("GRID", new LayoutBox(10, 25, 110, 110));
        layoutMap.put("DETAIL", new LayoutBox(170, 10, 100, 120));
        layoutMap.put("INVENTORY", new LayoutBox(10, 140, 260, 50));
        layoutMap.put("BTN_BUY", new LayoutBox(180, 140, 80, 20));
    }

    @Override
    protected void init() {
        // 2. 在初始化时尝试读取配置文件，覆盖默认值
        loadLayoutConfig();

        super.init();

        // 编辑开关
        this.editToggleButton = this.addRenderableWidget(Button.builder(Component.literal("[EDIT UI]"), btn -> {
            this.isEditMode = !this.isEditMode;
            if (!this.isEditMode) {
                // 退出编辑模式时保存
                saveLayoutConfig();
                btn.setMessage(Component.literal("[EDIT UI] (Saved)"));
            } else {
                btn.setMessage(Component.literal("[SAVE UI]"));
            }
        }).bounds(5, 5, 100, 20).build());

        // 购买按钮
        this.purchaseButton = this.addRenderableWidget(new PurchaseButton(0, 0, 80, 20, Component.literal("PURCHASE"), btn -> tryPurchase()));

        // 3. 初始化后立即更新所有位置（包括按钮和Slot），防止错位
        updateButtonLayout();
        updateSlotsPositions();
    }

    // --- 持久化逻辑 (新增) ---
    private void loadLayoutConfig() {
        if (!Files.exists(CONFIG_PATH)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Map<String, LayoutBox> loaded = GSON.fromJson(reader, new TypeToken<Map<String, LayoutBox>>(){}.getType());
            if (loaded != null) {
                // 合并配置，防止新版本加了key读取不到
                loaded.forEach((k, v) -> this.layoutMap.put(k, v));
            }
        } catch (Exception e) {
            e.printStackTrace(); // 实际开发建议用Logger
        }
    }

    private void saveLayoutConfig() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this.layoutMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 如果在编辑模式，持续刷新位置
        if (this.isEditMode) {
            updateSlotsPositions();
            updateButtonLayout();
        }

        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);

        if (this.isEditMode) {
            renderEditOverlay(gfx, mouseX, mouseY);
        }

        // 4. 渲染 Tooltip (原版方法 + 自定义网格方法)
        this.renderTooltip(gfx, mouseX, mouseY);
        this.renderGridTooltip(gfx, mouseX, mouseY); // 新增：渲染自定义网格的Tooltip
    }

    private void updateButtonLayout() {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        LayoutBox btnBox = layoutMap.get("BTN_BUY");
        if (btnBox != null) {
            this.purchaseButton.setPosition(left + btnBox.x, top + btnBox.y);
            this.purchaseButton.setWidth(btnBox.w);
            this.purchaseButton.setHeight(btnBox.h);
        }
    }

    private void updateSlotsPositions() {
        LayoutBox invBox = layoutMap.get("INVENTORY");
        if (invBox == null) return;

        int startX = invBox.x + 8;
        int startY = invBox.y + 5;

        for (Slot slot : this.menu.slots) {
            if (slot.container instanceof Inventory) {
                int newX, newY;
                if (slot.getSlotIndex() >= 9) {
                    int idx = slot.getSlotIndex() - 9;
                    int row = idx / 9;
                    int col = idx % 9;
                    newX = startX + col * 18;
                    newY = startY + row * 18;
                } else {
                    int col = slot.getSlotIndex();
                    newX = startX + col * 18;
                    newY = startY + 3 * 18 + 4;
                }
                ((SlotAccessor) slot).setX(newX);
                ((SlotAccessor) slot).setY(newY);
            } else {
                ((SlotAccessor) slot).setX(-10000);
                ((SlotAccessor) slot).setY(-10000);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        LayoutBox win = layoutMap.get("WINDOW");
        fillBorderedRect(gfx, left + win.x, top + win.y, win.w, win.h, COL_BG_MAIN, COL_BORDER_GOLD);

        LayoutBox grid = layoutMap.get("GRID");
        renderItemGrid(gfx, left + grid.x, top + grid.y, grid.w, grid.h, mouseX, mouseY);

        LayoutBox detail = layoutMap.get("DETAIL");
        renderDetailPanel(gfx, left + detail.x, top + detail.y, detail.w, detail.h);

        LayoutBox inv = layoutMap.get("INVENTORY");
        renderInventoryBg(gfx, left + inv.x, top + inv.y, inv.w, inv.h);

        gfx.drawCenteredString(this.font, "ITEM SHOP", left + win.x + win.w / 2, top + win.y + 6, COL_BORDER_BRIGHT);
    }

    private void renderItemGrid(GuiGraphics gfx, int x, int y, int w, int h, int mouseX, int mouseY) {
        fillBorderedRect(gfx, x, y, w, h, COL_BG_PANEL, COL_BORDER_GOLD);

        int contentW = w - 10;
        this.gridCols = Math.max(1, contentW / (GRID_ITEM_SIZE + GRID_GAP));

        MerchantOffers offers = this.menu.getOffers();
        if (offers.isEmpty()) return;

        int contentH = h - 10;
        int visibleRows = Math.max(1, contentH / (GRID_ITEM_SIZE + GRID_GAP));

        int start = scrollOffset * gridCols;
        int end = Math.min(start + (visibleRows * gridCols), offers.size());

        for (int i = start; i < end; i++) {
            MerchantOffer offer = offers.get(i);
            int relIndex = i - start;
            int row = relIndex / gridCols;
            int col = relIndex % gridCols;

            int itemX = x + 5 + col * (GRID_ITEM_SIZE + GRID_GAP);
            int itemY = y + 5 + row * (GRID_ITEM_SIZE + GRID_GAP);

            if (itemY + GRID_ITEM_SIZE > y + h) break;

            boolean isHovered = mouseX >= itemX && mouseX < itemX + GRID_ITEM_SIZE && mouseY >= itemY && mouseY < itemY + GRID_ITEM_SIZE;
            boolean isSelected = (i == this.selectedIndex);

            int borderColor = isSelected ? COL_BORDER_BRIGHT : (isHovered ? COL_TEXT_GOLD : 0xFF333333);
            fillBorderedRect(gfx, itemX, itemY, GRID_ITEM_SIZE, GRID_ITEM_SIZE, 0xFF111111, borderColor);

            gfx.renderItem(offer.getResult(), itemX + 4, itemY + 4);
            gfx.renderItemDecorations(this.font, offer.getResult(), itemX + 4, itemY + 4);

            if (offer.isOutOfStock()) {
                gfx.fill(itemX, itemY, itemX + GRID_ITEM_SIZE, itemY + GRID_ITEM_SIZE, 0x80000000);
                gfx.drawCenteredString(this.font, "x", itemX + 12, itemY + 6, COL_PRICE_RED);
            }
        }

        // 滚动条
        int totalRows = (int) Math.ceil((double) offers.size() / gridCols);
        if (totalRows > visibleRows) {
            float ratio = (float) scrollOffset / (totalRows - visibleRows);
            int barH = Math.max(10, h - 20);
            int barY = y + 5 + (int)(ratio * (barH));
            gfx.fill(x + w - 3, y + 5, x + w - 1, y + h - 5, 0xFF222222);
            gfx.fill(x + w - 3, barY, x + w - 1, barY + 10, COL_BORDER_BRIGHT);
        }
    }

    // --- 新增：网格 Tooltip 渲染逻辑 ---
    private void renderGridTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        // 如果有覆盖层（如编辑模式），可能不希望显示Tooltip，这里看你需求，目前是共存
        LayoutBox grid = layoutMap.get("GRID");
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        int x = left + grid.x;
        int y = top + grid.y;

        // 检查鼠标是否在Grid区域内
        if (mouseX < x || mouseX > x + grid.w || mouseY < y || mouseY > y + grid.h) return;

        MerchantOffers offers = this.menu.getOffers();
        if (offers.isEmpty()) return;

        int contentH = grid.h - 10;
        int visibleRows = Math.max(1, contentH / (GRID_ITEM_SIZE + GRID_GAP));
        int start = scrollOffset * gridCols;
        int end = Math.min(start + (visibleRows * gridCols), offers.size());

        for (int i = start; i < end; i++) {
            MerchantOffer offer = offers.get(i);
            int relIndex = i - start;
            int row = relIndex / gridCols;
            int col = relIndex % gridCols;

            int itemX = x + 5 + col * (GRID_ITEM_SIZE + GRID_GAP);
            int itemY = y + 5 + row * (GRID_ITEM_SIZE + GRID_GAP);

            // 检查是否悬停在当前物品上
            if (mouseX >= itemX && mouseX < itemX + GRID_ITEM_SIZE &&
                    mouseY >= itemY && mouseY < itemY + GRID_ITEM_SIZE) {

                // 调用原版 Tooltip 渲染
                gfx.renderTooltip(this.font, offer.getResult(), mouseX, mouseY);
                return; // 找到一个就返回，避免重叠
            }
        }
    }

    private void renderEditOverlay(GuiGraphics gfx, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        layoutMap.forEach((name, box) -> {
            int bx = left + box.x;
            int by = top + box.y;
            gfx.renderOutline(bx, by, box.w, box.h, COL_BORDER_EDIT);
            gfx.fill(bx, by, bx + box.w, by + box.h, 0x2000FF00);
            gfx.drawString(this.font, name, bx + 2, by + 2, COL_BORDER_EDIT, false);
            gfx.fill(bx + box.w - RESIZE_HANDLE_SIZE, by + box.h - RESIZE_HANDLE_SIZE,
                    bx + box.w, by + box.h, COL_HANDLE_EDIT);
        });
        gfx.drawCenteredString(this.font, "LMB: Drag Body | RMB: Resize Handle", this.width / 2, 10, COL_BORDER_EDIT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isEditMode) {
            int left = (this.width - this.imageWidth) / 2;
            int top = (this.height - this.imageHeight) / 2;

            // 倒序遍历，优先选中上层
            for (String key : layoutMap.keySet()) {
                LayoutBox box = layoutMap.get(key);
                int bx = left + box.x;
                int by = top + box.y;

                if (mouseX >= bx + box.w - RESIZE_HANDLE_SIZE && mouseX <= bx + box.w &&
                        mouseY >= by + box.h - RESIZE_HANDLE_SIZE && mouseY <= by + box.h) {
                    this.activeBox = box;
                    this.currentAction = EditAction.RESIZE;
                    this.dragOffsetX = (int)mouseX;
                    this.dragOffsetY = (int)mouseY;
                    this.resizeStartW = box.w;
                    this.resizeStartH = box.h;
                    return true;
                }

                if (mouseX >= bx && mouseX < bx + box.w && mouseY >= by && mouseY < by + box.h) {
                    this.activeBox = box;
                    this.currentAction = EditAction.DRAG;
                    this.dragOffsetX = (int)(mouseX - bx);
                    this.dragOffsetY = (int)(mouseY - by);
                    return true;
                }
            }
        }

        if (!this.isEditMode) {
            handleGridClick(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isEditMode && this.activeBox != null) {
            int left = (this.width - this.imageWidth) / 2;
            int top = (this.height - this.imageHeight) / 2;

            if (this.currentAction == EditAction.DRAG) {
                this.activeBox.x = (int)(mouseX - left - this.dragOffsetX);
                this.activeBox.y = (int)(mouseY - top - this.dragOffsetY);
            }
            else if (this.currentAction == EditAction.RESIZE) {
                int deltaX = (int)mouseX - this.dragOffsetX;
                int deltaY = (int)mouseY - this.dragOffsetY;
                this.activeBox.w = Math.max(30, this.resizeStartW + deltaX);
                this.activeBox.h = Math.max(30, this.resizeStartH + deltaY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isEditMode) {
            this.activeBox = null;
            this.currentAction = EditAction.NONE;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void handleGridClick(double mouseX, double mouseY) {
        LayoutBox grid = layoutMap.get("GRID");
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        int startX = left + grid.x + 5;
        int startY = top + grid.y + 5;

        if (mouseX < startX || mouseX > startX + grid.w - 10 || mouseY < startY || mouseY > startY + grid.h - 10) return;

        int relX = (int)mouseX - startX;
        int relY = (int)mouseY - startY;
        int col = relX / (GRID_ITEM_SIZE + GRID_GAP);
        int row = relY / (GRID_ITEM_SIZE + GRID_GAP);

        int index = (scrollOffset + row) * gridCols + col;

        if (index >= 0 && index < this.menu.getOffers().size()) {
            this.selectedIndex = index;
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isEditMode) {
            int totalRows = (int) Math.ceil((double) this.menu.getOffers().size() / gridCols);
            LayoutBox grid = layoutMap.get("GRID");
            int visibleRows = Math.max(1, (grid.h - 10) / (GRID_ITEM_SIZE + GRID_GAP));

            if (totalRows > visibleRows) {
                this.scrollOffset = Mth.clamp(this.scrollOffset - (int)scrollY, 0, totalRows - visibleRows);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderDetailPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        if (this.menu.getOffers().isEmpty() || selectedIndex >= this.menu.getOffers().size()) return;
        MerchantOffer offer = this.menu.getOffers().get(selectedIndex);
        ItemStack result = offer.getResult();

        gfx.drawWordWrap(this.font, result.getHoverName(), x + 5, y + 5, w - 10, COL_BORDER_BRIGHT);

        gfx.pose().pushPose();
        gfx.pose().translate(x + w/2, y + 45, 100);
        gfx.pose().scale(3.0f, 3.0f, 1.0f);
        gfx.renderItem(result, -8, -8);
        gfx.pose().popPose();

        gfx.drawString(this.font, "Recipe:", x + 5, y + 80, COL_TEXT_BLUE, false);
        gfx.renderItem(offer.getCostA(), x + 10, y + 92);
        gfx.renderItemDecorations(this.font, offer.getCostA(), x + 10, y + 92);

        // 悬停查看详细配方物品的Tooltip
        if (isHovering(x + 10, y + 92, 16, 16, x + 10, y + 92)) { // 简化判断，实际建议传mouseX/Y
            // 这里作为示例，最好还是放在 renderTooltip 里统一处理
        }

        if (!offer.getCostB().isEmpty()) {
            gfx.drawString(this.font, "+", x + 30, y + 96, COL_TEXT_GOLD, false);
            gfx.renderItem(offer.getCostB(), x + 40, y + 92);
            gfx.renderItemDecorations(this.font, offer.getCostB(), x + 40, y + 92);
        }
    }

    private void renderInventoryBg(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0x40000000);
        gfx.renderOutline(x, y, w, h, COL_BORDER_GOLD);
        gfx.drawString(this.font, "INVENTORY", x + 2, y - 10, 0xFFAAAAAA);

        int startX = x + 8;
        int startY = y + 5;
        for (int i = 0; i < 27; i++) {
            renderHextechSlot(gfx, startX + (i % 9) * 18, startY + (i / 9) * 18);
        }
        for (int i = 0; i < 9; i++) {
            renderHextechSlot(gfx, startX + i * 18, startY + 3 * 18 + 4);
        }
    }

    private void renderHextechSlot(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 16, y + 16, 0xFF050505);
        gfx.renderOutline(x - 1, y - 1, 18, 18, 0xFF333333);
    }

    private void fillBorderedRect(GuiGraphics gfx, int x, int y, int w, int h, int bgCol, int borderCol) {
        gfx.fill(x, y, x + w, y + h, bgCol);
        gfx.renderOutline(x, y, w, h, borderCol);
    }

    private void tryPurchase() {
        if (!this.menu.getOffers().isEmpty()) {
            this.menu.setSelectionHint(this.selectedIndex);
            this.minecraft.getConnection().send(new ServerboundSelectTradePacket(this.selectedIndex));
            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, 2, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, this.minecraft.player);
        }
    }

    // 必须是静态类或者能被GSON序列化的结构
    public static class LayoutBox {
        public int x, y, w, h;
        public LayoutBox(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    class PurchaseButton extends Button {
        public PurchaseButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }
        @Override
        public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            fillBorderedRect(gfx, getX(), getY(), width, height, !active ? 0xFF111111 : (isHovered ? 0xFF222222 : COL_BG_PANEL), !active ? 0xFF333333 : (isHovered ? COL_BORDER_BRIGHT : COL_BORDER_GOLD));
            gfx.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, !active ? 0xFF555555 : (isHovered ? 0xFFFFFFFF : COL_TEXT_GOLD));
        }
    }
}