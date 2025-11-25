//package com.example.examplemod.client.gui;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//import net.minecraft.client.resources.sounds.SimpleSoundInstance;
//import net.minecraft.network.chat.Component;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.inventory.Slot;
//import net.minecraft.world.item.Item; // [新增] 需要导入 Item 类来使用 TooltipContext
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.TooltipFlag;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.ContainerScreenEvent;
//import net.neoforged.neoforge.client.event.ScreenEvent;
//import net.neoforged.neoforge.common.ModConfigSpec;
//import org.apache.commons.lang3.tuple.Pair;
//
//import java.util.Collections;
//import java.util.List;
//
//@EventBusSubscriber(value = Dist.CLIENT)
//public class InventoryTooltipRenderer {
//
//    public static class TooltipConfig {
//        public static final TooltipConfig INSTANCE;
//        public static final ModConfigSpec SPEC;
//
//        static {
//            Pair<TooltipConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(TooltipConfig::new);
//            SPEC = pair.getRight();
//            INSTANCE = pair.getLeft();
//        }
//
//        public final ModConfigSpec.IntValue xOffset;
//        public final ModConfigSpec.IntValue yOffset;
//        public final ModConfigSpec.IntValue width;
//        public final ModConfigSpec.IntValue height;
//        public final ModConfigSpec.BooleanValue enabled;
//
//        TooltipConfig(ModConfigSpec.Builder builder) {
//            builder.push("InventoryTooltipWindow");
//            xOffset = builder.comment("Tooltip Window X Offset").defineInRange("xOffset", 10, -2000, 2000);
//            yOffset = builder.comment("Tooltip Window Y Offset").defineInRange("yOffset", 10, -2000, 2000);
//            width = builder.comment("Tooltip Window Width").defineInRange("width", 120, 50, 1000);
//            height = builder.comment("Tooltip Window Height").defineInRange("height", 100, 40, 1000);
//            enabled = builder.comment("Enable Tooltip Window").define("enabled", true);
//            builder.pop();
//        }
//    }
//
//    // ==========================================
//    // 状态管理
//    // ==========================================
//
//    private static boolean configLoaded = false;
//
//    private static class WindowState {
//        static int x = 10;
//        static int y = 10;
//        static int width = 120;
//        static int height = 100;
//        static boolean isEnabled = true;
//
//        static boolean isEditMode = false;
//        static boolean isMoving = false;
//        static boolean isResizing = false;
//
//        static float scrollOffset = 0; // 文本滚动
//        static final int LINE_HEIGHT = 10;
//
//        static final int MIN_WIDTH = 60;
//        static final int MIN_HEIGHT = 50;
//    }
//
//    private static final int BUTTON_SIZE = 12;
//    private static final int RESIZE_HANDLE_SIZE = 10;
//
//    // ==========================================
//    // 渲染逻辑
//    // ==========================================
//
//    @SubscribeEvent
//    public static void onRenderGuiForeground(ContainerScreenEvent.Render.Foreground event) {
//        if (!(event.getContainerScreen() instanceof AbstractContainerScreen)) return;
//
//        if (!configLoaded) {
//            WindowState.x = TooltipConfig.INSTANCE.xOffset.get();
//            WindowState.y = TooltipConfig.INSTANCE.yOffset.get();
//            WindowState.width = TooltipConfig.INSTANCE.width.get();
//            WindowState.height = TooltipConfig.INSTANCE.height.get();
//            WindowState.isEnabled = TooltipConfig.INSTANCE.enabled.get();
//            configLoaded = true;
//        }
//
//        if (!WindowState.isEnabled && !WindowState.isEditMode) return;
//
//        GuiGraphics guiGraphics = event.getGuiGraphics();
//        var screen = event.getContainerScreen();
//        int guiLeft = screen.getGuiLeft();
//        int guiTop = screen.getGuiTop();
//
//        int viewX = guiLeft + WindowState.x;
//        int viewY = guiTop + WindowState.y;
//        int viewW = WindowState.width;
//        int viewH = WindowState.height;
//
//        PoseStack pose = guiGraphics.pose();
//        pose.pushPose();
//        pose.translate(-guiLeft, -guiTop, 0);
//
//        // 1. 背景与边框
//        int borderColor = WindowState.isEditMode ? 0xFFFFD700 : 0xFF888888;
//        guiGraphics.fill(viewX, viewY, viewX + viewW, viewY + viewH, 0xCC000000);
//        guiGraphics.renderOutline(viewX, viewY, viewW, viewH, borderColor);
//
//        // 2. 调整大小手柄
//        if (WindowState.isEditMode) {
//            int handleX = viewX + viewW - RESIZE_HANDLE_SIZE;
//            int handleY = viewY + viewH - RESIZE_HANDLE_SIZE;
//            guiGraphics.fill(handleX, handleY, viewX + viewW, viewY + viewH, 0xAAFFD700);
//        }
//
//        // 3. 渲染 Tooltip 内容
//        ItemStack targetStack = ItemStack.EMPTY;
//
//        // 尝试从 InventoryModelRenderer 获取
//        Object externalTarget = InventoryModelRenderer.renderTarget;
//        if (externalTarget instanceof ItemStack stack && !stack.isEmpty()) {
//            targetStack = stack;
//        } else if (externalTarget instanceof Entity entity) {
//            try {
//                targetStack = entity.getPickResult();
//            } catch (Exception ignored) {}
//        }
//
//        // 兜底：鼠标悬停物品
//        if (targetStack == null || targetStack.isEmpty()) {
//            Slot hoveredSlot = screen.getSlotUnderMouse();
//            if (hoveredSlot != null && hoveredSlot.hasItem()) {
//                targetStack = hoveredSlot.getItem();
//            }
//        }
//
//        List<Component> tooltipLines = Collections.emptyList();
//
//        // [修复核心位置]
//        if (targetStack != null && !targetStack.isEmpty()) {
//            Minecraft mc = Minecraft.getInstance();
//            TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
//
//            // 修正：添加 Item.TooltipContext.of(mc.level) 作为第一个参数
//            if (mc.level != null) {
//                tooltipLines = targetStack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, flag);
//            } else {
//                // 理论上 GUI 中 mc.level 不会为空，但作为安全措施
//                tooltipLines = List.of(Component.literal("Loading..."));
//            }
//
//        } else if (externalTarget instanceof Entity entity) {
//            tooltipLines = List.of(
//                    Component.literal("Entity: " + entity.getDisplayName().getString()),
//                    Component.literal("Type: " + entity.getType().getDescription().getString()),
//                    Component.literal("UUID: " + entity.getUUID().toString())
//            );
//        } else {
//            tooltipLines = List.of(Component.literal("No Item Selected").withStyle(s -> s.withColor(0xAAAAAA)));
//        }
//
//        // 剪裁区域 (Scissor)
//        guiGraphics.enableScissor(viewX + 2, viewY + 2, viewX + viewW - 2, viewY + viewH - 2);
//
//        int contentHeight = tooltipLines.size() * WindowState.LINE_HEIGHT;
//        int maxScroll = Math.max(0, contentHeight - (viewH - 4));
//
//        if (WindowState.scrollOffset < 0) WindowState.scrollOffset = 0;
//        if (WindowState.scrollOffset > maxScroll) WindowState.scrollOffset = maxScroll;
//
//        int startY = (int) (viewY + 4 - WindowState.scrollOffset);
//
//        for (int i = 0; i < tooltipLines.size(); i++) {
//            Component line = tooltipLines.get(i);
//            int lineY = startY + (i * WindowState.LINE_HEIGHT);
//
//            if (lineY + WindowState.LINE_HEIGHT > viewY && lineY < viewY + viewH) {
//                guiGraphics.drawString(Minecraft.getInstance().font, line, viewX + 5, lineY, 0xFFFFFF, true);
//            }
//        }
//
//        guiGraphics.disableScissor();
//
//        // 4. 滚动条
//        if (contentHeight > (viewH - 4)) {
//            int barH = (int) ((float) (viewH - 4) / contentHeight * (viewH - 4));
//            if (barH < 10) barH = 10;
//            int barY = viewY + 2 + (int) ((WindowState.scrollOffset / maxScroll) * (viewH - 4 - barH));
//            int barX = viewX + viewW - 4;
//            guiGraphics.fill(barX, barY, barX + 2, barY + barH, 0xFFFFFFFF);
//        }
//
//        // 5. 顶部按钮
//        int btnX = viewX + viewW - BUTTON_SIZE - 2;
//        int btnY = viewY + 2;
//        boolean hoverEdit = isMouseOver(event.getMouseX(), event.getMouseY(), btnX, btnY, BUTTON_SIZE, BUTTON_SIZE);
//        guiGraphics.fill(btnX, btnY, btnX + BUTTON_SIZE, btnY + BUTTON_SIZE, WindowState.isEditMode ? 0xFFFFAA00 : (hoverEdit ? 0xFFAAAAAA : 0xFF555555));
//        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "T", btnX + BUTTON_SIZE / 2 + 1, btnY + 2, 0xFFFFFF);
//
//        pose.popPose();
//    }
//
//    // ==========================================
//    // 交互逻辑
//    // ==========================================
//
//    @SubscribeEvent
//    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
//        if (!(event.getScreen() instanceof AbstractContainerScreen screen)) return;
//
//        double mx = event.getMouseX();
//        double my = event.getMouseY();
//        int viewX = screen.getGuiLeft() + WindowState.x;
//        int viewY = screen.getGuiTop() + WindowState.y;
//        int viewW = WindowState.width;
//        int viewH = WindowState.height;
//
//        int btnX = viewX + viewW - BUTTON_SIZE - 2;
//        int btnY = viewY + 2;
//        if (WindowState.isEnabled && isMouseOver(mx, my, btnX, btnY, BUTTON_SIZE, BUTTON_SIZE)) {
//            WindowState.isEditMode = !WindowState.isEditMode;
//            playSound();
//            event.setCanceled(true);
//            return;
//        }
//
//        if (!WindowState.isEnabled && !WindowState.isEditMode) return;
//
//        if (WindowState.isEditMode) {
//            int handleX = viewX + viewW - RESIZE_HANDLE_SIZE;
//            int handleY = viewY + viewH - RESIZE_HANDLE_SIZE;
//            if (isMouseOver(mx, my, handleX, handleY, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE)) {
//                WindowState.isResizing = true;
//                event.setCanceled(true);
//                return;
//            }
//            if (isMouseOver(mx, my, viewX, viewY, viewW, viewH)) {
//                WindowState.isMoving = true;
//                event.setCanceled(true);
//                return;
//            }
//        }
//    }
//
//    @SubscribeEvent
//    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
//        WindowState.isMoving = false;
//        WindowState.isResizing = false;
//    }
//
//    @SubscribeEvent
//    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
//        if (WindowState.isMoving) {
//            WindowState.x += (int) event.getDragX();
//            WindowState.y += (int) event.getDragY();
//            event.setCanceled(true);
//        } else if (WindowState.isResizing) {
//            WindowState.width += (int) event.getDragX();
//            WindowState.height += (int) event.getDragY();
//            if (WindowState.width < WindowState.MIN_WIDTH) WindowState.width = WindowState.MIN_WIDTH;
//            if (WindowState.height < WindowState.MIN_HEIGHT) WindowState.height = WindowState.MIN_HEIGHT;
//            event.setCanceled(true);
//        }
//    }
//
//    @SubscribeEvent
//    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
//        if (!(event.getScreen() instanceof AbstractContainerScreen screen)) return;
//        if (!WindowState.isEnabled) return;
//
//        int viewX = screen.getGuiLeft() + WindowState.x;
//        int viewY = screen.getGuiTop() + WindowState.y;
//
//        if (isMouseOver(event.getMouseX(), event.getMouseY(), viewX, viewY, WindowState.width, WindowState.height)) {
//            double scrollY = event.getScrollDeltaY();
//            WindowState.scrollOffset -= (float) (scrollY * WindowState.LINE_HEIGHT);
//            event.setCanceled(true);
//        }
//    }
//
//    @SubscribeEvent
//    public static void onGuiClosed(ScreenEvent.Closing event) {
//        if (event.getScreen() instanceof AbstractContainerScreen) {
//            TooltipConfig.INSTANCE.xOffset.set(WindowState.x);
//            TooltipConfig.INSTANCE.yOffset.set(WindowState.y);
//            TooltipConfig.INSTANCE.width.set(WindowState.width);
//            TooltipConfig.INSTANCE.height.set(WindowState.height);
//            TooltipConfig.INSTANCE.enabled.set(WindowState.isEnabled);
//            TooltipConfig.SPEC.save();
//        }
//    }
//
//    private static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
//        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
//    }
//
//    private static void playSound() {
//        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//    }
//}