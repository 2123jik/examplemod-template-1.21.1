package com.example.examplemod.client.screen;

import com.example.examplemod.mixin.SlotAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ModernBrewingStandScreen extends AbstractContainerScreen<BrewingStandMenu> {

    // --- 颜色常量 (保持风格统一) ---
    private static final int COL_BG_MAIN = 0xFF010A13;
    private static final int COL_BG_PANEL = 0xFF091416;
    private static final int COL_BORDER_GOLD = 0xFF785A28;
    private static final int COL_BORDER_BRIGHT = 0xFFC8AA6E;
    private static final int COL_BORDER_EDIT = 0xFF00FF00;
    private static final int COL_HANDLE_EDIT = 0xFFFF0000;
    private static final int COL_PROGRESS_BG = 0xFF222222;
    private static final int COL_FUEL_BAR = 0xFFFFAA00; // 燃料金黄色
    private static final int COL_BREW_BAR = 0xFFFFFFFF; // 酿造进度白色/淡蓝

    // --- 布局系统 ---
    private final Map<String, LayoutBox> layoutMap = new HashMap<>();
    private boolean isEditMode = false;

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("brewing_stand_layout.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 编辑状态
    private LayoutBox activeBox = null;
    private EditAction currentAction = EditAction.NONE;
    private int dragOffsetX, dragOffsetY;
    private int resizeStartW, resizeStartH;
    private static final int RESIZE_HANDLE_SIZE = 10;

    private Button editToggleButton;

    private enum EditAction {
        NONE, DRAG, RESIZE
    }

    public ModernBrewingStandScreen(BrewingStandMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 220;
        this.inventoryLabelY = -10000;
        this.titleLabelY = -10000;

        setupDefaultLayout();
    }

    private void setupDefaultLayout() {
        // 主窗口背景
        layoutMap.put("WINDOW", new LayoutBox(0, 0, 220, 220));
        
        // 核心酿造区域 (包含药水和原料)
        layoutMap.put("BREWING_AREA", new LayoutBox(60, 20, 100, 100));
        
        // 燃料区域
        layoutMap.put("FUEL_AREA", new LayoutBox(10, 20, 40, 60));
        
        // 进度条显示区域 (箭头)
        layoutMap.put("PROGRESS_BAR", new LayoutBox(170, 40, 10, 60));
        
        // 玩家背包区域
        layoutMap.put("INVENTORY", new LayoutBox(10, 130, 200, 85));
    }

    @Override
    protected void init() {
        loadLayoutConfig();
        super.init();

        this.editToggleButton = this.addRenderableWidget(Button.builder(Component.literal("[EDIT UI]"), btn -> {
            this.isEditMode = !this.isEditMode;
            if (!this.isEditMode) {
                saveLayoutConfig();
                btn.setMessage(Component.literal("[EDIT UI] (Saved)"));
            } else {
                btn.setMessage(Component.literal("[SAVE UI]"));
            }
        }).bounds(this.leftPos + 5, this.topPos - 25, 80, 20).build());

        updateSlotsPositions();
    }

    // --- 核心渲染逻辑 ---

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (this.isEditMode) {
            updateSlotsPositions();
            // 让按钮跟随窗口移动（可选）
            this.editToggleButton.setPosition(this.leftPos + 5, this.topPos - 25);
        }

        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);

        if (this.isEditMode) {
            renderEditOverlay(gfx, mouseX, mouseY);
        }

        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        // 1. 渲染主背景
        LayoutBox win = layoutMap.get("WINDOW");
        fillBorderedRect(gfx, left + win.x, top + win.y, win.w, win.h, COL_BG_MAIN, COL_BORDER_GOLD);
        gfx.drawCenteredString(this.font, this.title, left + win.x + win.w / 2, top + win.y + 6, COL_BORDER_BRIGHT);

        // 2. 渲染各个区域背景
        LayoutBox fuelBox = layoutMap.get("FUEL_AREA");
        fillBorderedRect(gfx, left + fuelBox.x, top + fuelBox.y, fuelBox.w, fuelBox.h, COL_BG_PANEL, COL_BORDER_GOLD);
        // 绘制燃料图标/文字提示
        gfx.drawCenteredString(this.font, "FUEL", left + fuelBox.x + fuelBox.w/2, top + fuelBox.y + 4, 0xFF888888);

        LayoutBox brewBox = layoutMap.get("BREWING_AREA");
        fillBorderedRect(gfx, left + brewBox.x, top + brewBox.y, brewBox.w, brewBox.h, COL_BG_PANEL, COL_BORDER_GOLD);
        
        LayoutBox invBox = layoutMap.get("INVENTORY");
        renderInventoryBg(gfx, left + invBox.x, top + invBox.y, invBox.w, invBox.h);

        // 3. 渲染燃料进度 (垂直条)
        // 燃料最大值通常是20
        int fuel = this.menu.getFuel(); 
        int fuelHeight = (int) (fuelBox.h * 0.7); // 进度条占区域高度的70%
        int fuelY = top + fuelBox.y + 20;
        int fuelX = left + fuelBox.x + (fuelBox.w - 8) / 2;
        
        // 背景槽
        fillBorderedRect(gfx, fuelX, fuelY, 8, fuelHeight, COL_PROGRESS_BG, 0xFF444444);
        
        // 填充条
        if (fuel > 0) {
            float fuelPct = Mth.clamp(fuel / 20.0f, 0f, 1f);
            int barSize = (int) (fuelHeight * fuelPct);
            gfx.fill(fuelX + 1, fuelY + fuelHeight - barSize + 1, fuelX + 7, fuelY + fuelHeight - 1, COL_FUEL_BAR);
        }

        // 4. 渲染酿造进度 (取代原版的箭头)
        LayoutBox progressBox = layoutMap.get("PROGRESS_BAR");
        fillBorderedRect(gfx, left + progressBox.x, top + progressBox.y, progressBox.w, progressBox.h, COL_BG_PANEL, 0xFF444444);
        
        int brewTime = this.menu.getBrewingTicks();
        if (brewTime > 0) {
            // 原版酿造时间是 400 ticks? 还是 20秒 * 20 = 400. 
            // BrewingStandMenu里通常最大值是 400.
            int maxTime = 400; 
            float progress = 1.0f - (float)brewTime / maxTime; // 倒计时，所以反过来
            
            int barH = (int) ((progressBox.h - 2) * progress);
            gfx.fill(left + progressBox.x + 1, top + progressBox.y + (progressBox.h - 1) - barH, 
                     left + progressBox.x + progressBox.w - 1, top + progressBox.y + progressBox.h - 1, COL_BREW_BAR);
        }
        
        // 5. 连接线 (装饰性，画出从原料到瓶子的线条)
        int centerX = left + brewBox.x + brewBox.w / 2;
        int topY = top + brewBox.y + 25; // 原料下方
        int botY = top + brewBox.y + brewBox.h - 35; // 瓶子上方
        gfx.fill(centerX - 1, topY, centerX + 1, botY, 0xFF444444); // 垂直中线
        gfx.fill(centerX - 30, botY, centerX + 30, botY + 2, 0xFF444444); // 水平分叉线
        // 连接到三个瓶子的短线
        gfx.fill(centerX - 1, botY, centerX + 1, botY + 10, 0xFF444444); // 中间
        gfx.fill(centerX - 30, botY, centerX - 28, botY + 10, 0xFF444444); // 左边
        gfx.fill(centerX + 28, botY, centerX + 30, botY + 10, 0xFF444444); // 右边
    }

    private void updateSlotsPositions() {
        int left = this.leftPos;
        int top = this.topPos;

        // --- 1. 燃料槽 (Slot 4) ---
        LayoutBox fuelBox = layoutMap.get("FUEL_AREA");
        if (fuelBox != null) {
            setSlotPos(4, left + fuelBox.x + (fuelBox.w - 16) / 2, top + fuelBox.y + fuelBox.h - 20); // 放在燃料区底部
        }

        // --- 2. 酿造槽 (Slot 0-3) ---
        LayoutBox brewBox = layoutMap.get("BREWING_AREA");
        if (brewBox != null) {
            int centerX = left + brewBox.x + brewBox.w / 2;
            int topY = top + brewBox.y;
            
            // 原料槽 (Slot 3) - 顶部居中
            setSlotPos(3, centerX - 8, topY + 8);

            // 药水槽 (Slot 0, 1, 2) - 底部三角形
            // 注意：Slot 0=左, 1=中, 2=右 (原版逻辑)
            // 原版 GUI 参考: 左(56, 51), 中(79, 58), 右(102, 51). 中间那个反而低一些
            int baseY = topY + brewBox.h - 20;
            
            // 重新排列成稍微现代一点的三角形
            setSlotPos(0, centerX - 36, baseY - 8); // 左
            setSlotPos(1, centerX - 8,  baseY);     // 中
            setSlotPos(2, centerX + 20, baseY - 8); // 右
        }

        // --- 3. 玩家背包 (Slot 5-40) ---
        LayoutBox invBox = layoutMap.get("INVENTORY");
        if (invBox != null) {
            int startX = left + invBox.x + (invBox.w - 162) / 2; // 居中 9 * 18 = 162
            int startY = top + invBox.y + 18; // 标题下方

            // 主背包 (Slot 5 - 31, indices 9-35 in player inv) -> Menu slot 5 to 31
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 9; ++j) {
                    int slotIndex = 5 + j + i * 9;
                    setSlotPos(slotIndex, startX + j * 18, startY + i * 18);
                }
            }

            // 快捷栏 (Slot 32 - 40, indices 0-8 in player inv)
            int hotbarY = startY + 58 + 4;
            for (int k = 0; k < 9; ++k) {
                setSlotPos(32 + k, startX + k * 18, hotbarY);
            }
        }
    }

    private void setSlotPos(int index, int x, int y) {
        if (index < this.menu.slots.size()) {
            Slot slot = this.menu.slots.get(index);
            ((SlotAccessor) slot).setX(x - this.leftPos); // Slot存储的是相对坐标
            ((SlotAccessor) slot).setY(y - this.topPos);
        }
    }

    // --- 辅助绘制方法 ---

    private void renderInventoryBg(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0x40000000);
        gfx.renderOutline(x, y, w, h, COL_BORDER_GOLD);
        gfx.drawString(this.font, "INVENTORY", x + 5, y + 5, 0xFFAAAAAA);

        int startX = x + (w - 162) / 2;
        int startY = y + 18;
        
        // 绘制背包格子背景
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                renderHextechSlot(gfx, startX + j * 18, startY + i * 18);
            }
        }
        // 绘制快捷栏格子背景
        int hotbarY = startY + 58 + 4;
        for (int i = 0; i < 9; i++) {
            renderHextechSlot(gfx, startX + i * 18, hotbarY);
        }
    }

    private void renderHextechSlot(GuiGraphics gfx, int x, int y) {
        // 暗色背景 + 边框，营造槽位感
        gfx.fill(x, y, x + 16, y + 16, 0xFF000000);
        gfx.renderOutline(x - 1, y - 1, 18, 18, 0xFF333333);
    }

    private void fillBorderedRect(GuiGraphics gfx, int x, int y, int w, int h, int bgCol, int borderCol) {
        gfx.fill(x, y, x + w, y + h, bgCol);
        gfx.renderOutline(x, y, w, h, borderCol);
    }

    // --- 编辑模式逻辑 (与参考代码一致) ---

    private void renderEditOverlay(GuiGraphics gfx, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        layoutMap.forEach((name, box) -> {
            int bx = left + box.x;
            int by = top + box.y;
            gfx.renderOutline(bx, by, box.w, box.h, COL_BORDER_EDIT);
            gfx.fill(bx, by, bx + box.w, by + box.h, 0x2000FF00); // 半透明绿
            gfx.drawString(this.font, name, bx + 2, by + 2, COL_BORDER_EDIT, false);
            // 调整大小的手柄
            gfx.fill(bx + box.w - RESIZE_HANDLE_SIZE, by + box.h - RESIZE_HANDLE_SIZE,
                    bx + box.w, by + box.h, COL_HANDLE_EDIT);
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isEditMode) {
            int left = this.leftPos;
            int top = this.topPos;

            // 倒序遍历，优先选中上层
            for (String key : layoutMap.keySet()) {
                LayoutBox box = layoutMap.get(key);
                int bx = left + box.x;
                int by = top + box.y;

                // 检查 Resize 手柄
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

                // 检查拖拽主体
                if (mouseX >= bx && mouseX < bx + box.w && mouseY >= by && mouseY < by + box.h) {
                    this.activeBox = box;
                    this.currentAction = EditAction.DRAG;
                    this.dragOffsetX = (int)(mouseX - bx);
                    this.dragOffsetY = (int)(mouseY - by);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isEditMode && this.activeBox != null) {
            int left = this.leftPos;
            int top = this.topPos;

            if (this.currentAction == EditAction.DRAG) {
                this.activeBox.x = (int)(mouseX - left - this.dragOffsetX);
                this.activeBox.y = (int)(mouseY - top - this.dragOffsetY);
            }
            else if (this.currentAction == EditAction.RESIZE) {
                int deltaX = (int)mouseX - this.dragOffsetX;
                int deltaY = (int)mouseY - this.dragOffsetY;
                this.activeBox.w = Math.max(16, this.resizeStartW + deltaX);
                this.activeBox.h = Math.max(16, this.resizeStartH + deltaY);
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

    // --- 配置文件 IO ---

    private void loadLayoutConfig() {
        if (!Files.exists(CONFIG_PATH)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Map<String, LayoutBox> loaded = GSON.fromJson(reader, new TypeToken<Map<String, LayoutBox>>(){}.getType());
            if (loaded != null) {
                loaded.forEach(this.layoutMap::put);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveLayoutConfig() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this.layoutMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 数据类 ---
    public static class LayoutBox {
        public int x, y, w, h;
        public LayoutBox(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }
}