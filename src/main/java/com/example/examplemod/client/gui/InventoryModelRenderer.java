package com.example.examplemod.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Quaternionf;

import java.util.*;
import java.util.stream.Collectors;

@EventBusSubscriber(value = Dist.CLIENT)
public class InventoryModelRenderer {

    public static class ClientConfig {
        public static final ClientConfig INSTANCE;
        public static final ModConfigSpec SPEC;

        static {
            Pair<ClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ClientConfig::new);
            SPEC = pair.getRight();
            INSTANCE = pair.getLeft();
        }

        public final ModConfigSpec.IntValue xOffset;
        public final ModConfigSpec.IntValue yOffset;
        public final ModConfigSpec.IntValue width;
        public final ModConfigSpec.IntValue height;
        public final ModConfigSpec.DoubleValue scale;
        public final ModConfigSpec.IntValue selectorWidth;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("InventoryCustomModel");
            xOffset = builder.comment("Window X Offset").defineInRange("xOffset", -90, -2000, 2000);
            yOffset = builder.comment("Window Y Offset").defineInRange("yOffset", 10, -2000, 2000);
            width = builder.comment("Window Width").defineInRange("width", 80, 50, 1000);
            height = builder.comment("Window Height").defineInRange("height", 120, 60, 1000);
            scale = builder.comment("Model Scale").defineInRange("scale", 30.0, 5.0, 300.0);
            // [新增]
            selectorWidth = builder.comment("Selector List Width").defineInRange("selectorWidth", 100, 60, 400);
            builder.pop();
        }
    }

    // ==========================================
    // 1. 状态管理
    // ==========================================

    private static boolean configLoaded = false;
    public static Object renderTarget = null;

    private static final Map<EntityType<?>, Entity> ENTITY_CACHE = new HashMap<>();
    private static final Set<EntityType<?>> BROKEN_ENTITIES = new HashSet<>();
    private static List<EntityType<?>> SORTED_ENTITY_TYPES = null;

    private static class LayoutState {
        static int xOffset = -90;
        static int yOffset = 10;
        static int width = 80;
        static int height = 120;
        static int selectorWidth = 100; // [新增]

        static boolean isEditMode = false;
        static boolean isMovingWindow = false;
        static boolean isResizingWindow = false;
        // [新增] 列表调整状态
        static boolean isResizingSelector = false;

        static final int MIN_WIDTH = 60;
        static final int MIN_HEIGHT = 80;
    }

    private static class PreviewState {
        static float rotX = 0;
        static float rotY = 0;
        static float scale = 90;
        static float panX = 0;
        static float panY = 0;
        static boolean isManipulatingModel = false;
        static int dragType = 0;

        static void reset() {
            rotX = 0; rotY = 0; panX = 0; panY = 0;
            scale = ClientConfig.INSTANCE.scale.get().floatValue();
        }
    }

    private static class SelectorState {
        static boolean isOpen = false;
        static float scrollOffset = 0;
        static final int ITEM_HEIGHT = 12;
        static final int SCROLLBAR_WIDTH = 4;

        static void toggle() {
            isOpen = !isOpen;
            if (isOpen && SORTED_ENTITY_TYPES == null) {
                loadEntityTypes();
            }
        }

        static void loadEntityTypes() {
            SORTED_ENTITY_TYPES = BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(t -> t != EntityType.MARKER )
                    // 原代码：按显示名称排序
                    // .sorted(Comparator.comparing(t -> t.getDescription().getString()))

                    // 新代码：按 Registry Key (namespace:path) 排序
                    // 这会先按 modid 排序，再按实体的 registry name 排序
                    .sorted(Comparator.comparing(BuiltInRegistries.ENTITY_TYPE::getKey))

                    .collect(Collectors.toList());
        }
    }

    private static final int BUTTON_SIZE = 12;
    private static final int RESIZE_HANDLE_SIZE = 10;

    // ==========================================
    // 2. 渲染事件
    // ==========================================

    @SubscribeEvent
    public static void onRenderGuiForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof AbstractContainerScreen)) return;

        if (!configLoaded) {
            LayoutState.xOffset = ClientConfig.INSTANCE.xOffset.get();
            LayoutState.yOffset = ClientConfig.INSTANCE.yOffset.get();
            LayoutState.width = ClientConfig.INSTANCE.width.get();
            LayoutState.height = ClientConfig.INSTANCE.height.get();
            LayoutState.selectorWidth = ClientConfig.INSTANCE.selectorWidth.get(); // [新增]
            PreviewState.scale = ClientConfig.INSTANCE.scale.get().floatValue();
            configLoaded = true;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        var screen = event.getContainerScreen();
        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();

        int viewX = guiLeft + LayoutState.xOffset;
        int viewY = guiTop + LayoutState.yOffset;
        int viewW = LayoutState.width;
        int viewH = LayoutState.height;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(-guiLeft, -guiTop, 0);

        // 1. 渲染主窗口
        int borderColor = LayoutState.isEditMode ? 0xFFFFD700 : 0xFFFFFFFF;
        guiGraphics.fill(viewX, viewY, viewX + viewW, viewY + viewH, 0xFF000000);
        guiGraphics.renderOutline(viewX, viewY, viewW, viewH, borderColor);

        if (LayoutState.isEditMode) {
            // 渲染主窗口调整手柄
            int handleX = viewX + viewW - RESIZE_HANDLE_SIZE;
            int handleY = viewY + viewH - RESIZE_HANDLE_SIZE;
            guiGraphics.fill(handleX, handleY, viewX + viewW, viewY + viewH, 0xAAFFD700);
        }

        // 2. 3D 视口剪裁
        guiGraphics.enableScissor(viewX + 1, viewY + 1, viewX + viewW - 1, viewY + viewH - 1);

        Object target = renderTarget;
        if (target == null) target = Minecraft.getInstance().player;

        RenderSystem.backupProjectionMatrix();
        try {
            renderPreviewContent(guiGraphics, viewX + viewW / 2, viewY + viewH / 2, target);
        } catch (Exception e) {
            renderTarget = null;
        } finally {
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        guiGraphics.disableScissor();

        // 3. UI 按钮
        int editBtnX = viewX + viewW - BUTTON_SIZE - 2;
        int editBtnY = viewY + 2;
        boolean hoverEdit = isMouseOver(event.getMouseX(), event.getMouseY(), editBtnX, editBtnY, BUTTON_SIZE, BUTTON_SIZE);
        guiGraphics.fill(editBtnX, editBtnY, editBtnX + BUTTON_SIZE, editBtnY + BUTTON_SIZE, LayoutState.isEditMode ? 0xFFFFAA00 : (hoverEdit ? 0xFFAAAAAA : 0xFF555555));
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, LayoutState.isEditMode ? "V" : "E", editBtnX + BUTTON_SIZE / 2 + 1, editBtnY + 2, 0xFFFFFF);

        int listBtnX = viewX + 2;
        int listBtnY = viewY + 2;
        boolean hoverList = isMouseOver(event.getMouseX(), event.getMouseY(), listBtnX, listBtnY, BUTTON_SIZE, BUTTON_SIZE);
        guiGraphics.fill(listBtnX, listBtnY, listBtnX + BUTTON_SIZE, listBtnY + BUTTON_SIZE, SelectorState.isOpen ? 0xFF00AA00 : (hoverList ? 0xFFAAAAAA : 0xFF555555));
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "L", listBtnX + BUTTON_SIZE / 2 + 1, listBtnY + 2, 0xFFFFFF);
        // 4. 渲染列表选择器 (使用动态宽度)
        if (SelectorState.isOpen && SORTED_ENTITY_TYPES != null) {
            // [新增] 传入当前配置的宽度
            renderEntitySelector(guiGraphics, viewX + viewW, viewY, LayoutState.selectorWidth, viewH, event.getMouseX(), event.getMouseY());
        }

        pose.popPose();
    }


    private static void renderPreviewContent(GuiGraphics guiGraphics, int x, int y, Object target) {
        if (target == null) return;

        if (target instanceof Entity e && BROKEN_ENTITIES.contains(e.getType())) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "ERROR", x, y - 10, 0xFFFF0000);
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // [修复点] 动态 Z 轴深度计算
        // 问题原因：如果 scale 很大，模型会膨胀。如果 Z 轴位移不够（比如只有 150），模型前半部分就会穿过 Z=0 的屏幕平面被切掉。
        // 解决方案：Z 轴位移至少为 250，并且随着 scale 增大而增大 (scale * 1.5)。
        // 这样保证模型永远在“屏幕里面”。
        float safeZ = Math.max(250.0F, PreviewState.scale * 2.0F);

        poseStack.translate(x + PreviewState.panX, y + PreviewState.panY, safeZ);

        poseStack.mulPose(new Quaternionf().rotationXYZ(
                (float) Math.toRadians(PreviewState.rotX),
                (float) Math.toRadians(PreviewState.rotY),
                0
        ));
        RenderSystem.enableDepthTest();

        try {
            if (target instanceof ItemStack itemStack) {
                renderItemCustom(guiGraphics, poseStack, itemStack);
            } else if (target instanceof Entity entity) {
                renderEntityCustom(guiGraphics, poseStack, entity);
            }
        } catch (Throwable t) {
            if (target instanceof Entity e) {
                BROKEN_ENTITIES.add(e.getType());
            }
        } finally {
            guiGraphics.flush();
        }

        RenderSystem.disableDepthTest();
        poseStack.popPose();
    }

    private static void renderEntityCustom(GuiGraphics guiGraphics, PoseStack poseStack, Entity entity) {
        float entityScale = PreviewState.scale;
        poseStack.scale(entityScale, entityScale, -entityScale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        Lighting.setupForEntityInInventory();

        // 备份实体旋转状态
        float yBodyRotOld = 0, yRotOld, xRotOld, yHeadRotOOld = 0, yHeadRotOld = 0;
        if (entity instanceof LivingEntity living) {
            yBodyRotOld = living.yBodyRot;
            yRotOld = living.getYRot();
            xRotOld = living.getXRot();
            yHeadRotOOld = living.yHeadRotO;
            yHeadRotOld = living.yHeadRot;
            living.yBodyRot = 0;
            living.setYRot(0);
            living.setXRot(0);
            living.yHeadRot = 0;
            living.yHeadRotO = 0;
        } else {
            yRotOld = entity.getYRot();
            xRotOld = entity.getXRot();
            entity.setYRot(0);
            entity.setXRot(0);
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);

        try {
            RenderSystem.runAsFancy(() -> {
                dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, guiGraphics.bufferSource(), 0xF000F0);
            });
            guiGraphics.flush();
        } catch (Exception ignored) {
        } finally {
            dispatcher.setRenderShadow(true);
            // 恢复状态
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = yBodyRotOld;
                living.setYRot(yRotOld);
                living.setXRot(xRotOld);
                living.yHeadRotO = yHeadRotOOld;
                living.yHeadRot = yHeadRotOld;
            } else {
                entity.setYRot(yRotOld);
                entity.setXRot(xRotOld);
            }
            Lighting.setupFor3DItems();
        }
    }

    private static void renderItemCustom(GuiGraphics guiGraphics, PoseStack poseStack, ItemStack itemStack) {
        if (itemStack.isEmpty()) return;
        float itemScale = PreviewState.scale * 2.0f;
        poseStack.scale(itemScale, -itemScale, itemScale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        Minecraft mc = Minecraft.getInstance();
        Lighting.setupFor3DItems();

        poseStack.pushPose();
        BakedModel bakedModel = mc.getItemRenderer().getModel(itemStack, mc.level, mc.player, 0);
        if (!bakedModel.isGui3d()) poseStack.translate(0, 0, -0.05f);

        mc.getItemRenderer().render(itemStack, ItemDisplayContext.GROUND, false, poseStack, guiGraphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, bakedModel);
        guiGraphics.flush();

        poseStack.popPose();
    }

    // ==========================================
    // 4. 列表选择器 (支持宽度调整)
    // ==========================================
    private static void renderEntitySelector(GuiGraphics guiGraphics, int x, int y, int w, int h, double mx, double my) {
        // 背景
        guiGraphics.fill(x, y, x + w, y + h, 0xFF000000);

        guiGraphics.renderOutline(x, y, w, h, 0xFF888888);

        // [新增] 渲染右侧的拖拽手柄条
        // 当鼠标悬停在右边缘 (x+w-4 到 x+w) 时，高亮显示
        boolean hoverResize = mx >= x + w - 4 && mx <= x + w + 2 && my >= y && my <= y + h;
        int dragColor = (LayoutState.isResizingSelector || hoverResize) ? 0xAAFFD700 : 0x00000000;
        guiGraphics.fill(x + w - 3, y, x + w, y + h, dragColor);

        guiGraphics.enableScissor(x, y + 1, x + w - 4, y + h - 1);

        List<EntityType<?>> list = SORTED_ENTITY_TYPES;
        int totalHeight = list.size() * SelectorState.ITEM_HEIGHT;

        int maxScroll = Math.max(0, totalHeight - h);
        if (SelectorState.scrollOffset < 0) SelectorState.scrollOffset = 0;
        if (SelectorState.scrollOffset > maxScroll) SelectorState.scrollOffset = maxScroll;

        int startIndex = (int) (SelectorState.scrollOffset / SelectorState.ITEM_HEIGHT);
        int visibleCount = (h / SelectorState.ITEM_HEIGHT) + 2;

        for (int i = startIndex; i < startIndex + visibleCount && i < list.size(); i++) {
            EntityType<?> type = list.get(i);
            int itemY = (int) (y + (i * SelectorState.ITEM_HEIGHT) - SelectorState.scrollOffset);

            // 判断悬停（减去滚动条宽度和拖拽条宽度）
            boolean isHovered = mx >= x && mx < x + w - 4 && my >= itemY && my < itemY + SelectorState.ITEM_HEIGHT;
            if (isHovered) guiGraphics.fill(x + 1, itemY, x + w - 4, itemY + SelectorState.ITEM_HEIGHT, 0x40FFFFFF);

            boolean isSelected = renderTarget instanceof Entity e && e.getType() == type;
            boolean isBroken = BROKEN_ENTITIES.contains(type);

            if (isSelected) guiGraphics.fill(x, itemY, x + 2, itemY + SelectorState.ITEM_HEIGHT, 0xFF00FF00);

            Component name = type.getDescription();
            int color = isBroken ? 0xFF5555 : (isSelected ? 0x00FF00 : (isHovered ? 0xFFFFFF : 0xAAAAAA));

            // 根据当前宽度截取文字
            String nameStr = Minecraft.getInstance().font.plainSubstrByWidth(name.getString(), w - 15);
            if (isBroken) nameStr = "[X] " + nameStr;

            guiGraphics.drawString(Minecraft.getInstance().font, nameStr, x + 5, itemY + 2, color, false);
        }

        guiGraphics.disableScissor();

        // 滚动条逻辑
        if (totalHeight > h) {
            int barHeight = (int) ((float) h / totalHeight * h);
            if (barHeight < 10) barHeight = 10;
            int barY = y + (int) ((SelectorState.scrollOffset / maxScroll) * (h - barHeight));
            int barX = x + w - SelectorState.SCROLLBAR_WIDTH - 4; // 让出调整把手的位置
            guiGraphics.fill(barX, barY, barX + SelectorState.SCROLLBAR_WIDTH, barY + barHeight, 0xFF888888);
        }
    }

    // ==========================================
    // 5. 交互逻辑 (新增列表拖拽)
    // ==========================================

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen screen)) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();
        int viewX = screen.getGuiLeft() + LayoutState.xOffset;
        int viewY = screen.getGuiTop() + LayoutState.yOffset;
        int viewW = LayoutState.width;
        int viewH = LayoutState.height;
        int listX = viewX + viewW; // 列表的起始X

        // 0. 中键拾取物品逻辑 (保持不变)
        if (event.getButton() == 2) {
            Slot hoveredSlot = screen.getSlotUnderMouse();
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                ItemStack stack = hoveredSlot.getItem();
                if (stack.getItem() instanceof SpawnEggItem egg) {
                    renderTarget = getCachedEntity(egg.getType(stack));
                } else {
                    renderTarget = stack.copy();
                }
                playSound();
                event.setCanceled(true);
                return;
            }
        }

        // 1. 列表控制
        if (SelectorState.isOpen && SORTED_ENTITY_TYPES != null) {
            int listW = LayoutState.selectorWidth;

            // [新增] 检测是否点击了列表右边缘 (调整大小)
            if (mx >= listX + listW - 4 && mx <= listX + listW + 2 && my >= viewY && my <= viewY + viewH) {
                LayoutState.isResizingSelector = true;
                event.setCanceled(true);
                return;
            }

            // 列表项点击逻辑
            if (mx >= listX && mx < listX + listW - 4 && my >= viewY && my < viewY + viewH) {
                double relY = my - viewY + SelectorState.scrollOffset;
                int index = (int) (relY / SelectorState.ITEM_HEIGHT);
                if (index >= 0 && index < SORTED_ENTITY_TYPES.size()) {
                    EntityType<?> type = SORTED_ENTITY_TYPES.get(index);
                    if (!BROKEN_ENTITIES.contains(type)) {
                        renderTarget = getCachedEntity(type);
                    } else {
                        renderTarget = null;
                    }
                    playSound();
                }
                event.setCanceled(true);
                return;
            }
        }

        // 2. UI 按钮 (保持不变)
        int listBtnX = viewX + 2;
        int listBtnY = viewY + 2;
        if (isMouseOver(mx, my, listBtnX, listBtnY, BUTTON_SIZE, BUTTON_SIZE)) {
            if (event.getButton() == 0) SelectorState.toggle();
            else if (event.getButton() == 1) { renderTarget = null; PreviewState.reset(); }
            playSound();
            event.setCanceled(true);
            return;
        }

        int editBtnX = viewX + viewW - BUTTON_SIZE - 2;
        int editBtnY = viewY + 2;
        if (isMouseOver(mx, my, editBtnX, editBtnY, BUTTON_SIZE, BUTTON_SIZE)) {
            LayoutState.isEditMode = !LayoutState.isEditMode;
            playSound();
            event.setCanceled(true);
            return;
        }

        // 3. 主窗口编辑模式
        if (LayoutState.isEditMode) {
            int handleX = viewX + viewW - RESIZE_HANDLE_SIZE;
            int handleY = viewY + viewH - RESIZE_HANDLE_SIZE;
            if (isMouseOver(mx, my, handleX, handleY, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE)) {
                LayoutState.isResizingWindow = true;
                event.setCanceled(true);
                return;
            }
            if (isMouseOver(mx, my, viewX, viewY, viewW, viewH)) {
                LayoutState.isMovingWindow = true;
                event.setCanceled(true);
                return;
            }
        }
        // 4. 视图操作 (旋转/平移)
        else {
            if (isMouseOver(mx, my, viewX, viewY, viewW, viewH)) {
                if (event.getButton() == 2) {
                    PreviewState.reset();
                    playSound();
                } else {
                    PreviewState.isManipulatingModel = true;
                    PreviewState.dragType = event.getButton();
                }
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        LayoutState.isMovingWindow = false;
        LayoutState.isResizingWindow = false;
        LayoutState.isResizingSelector = false; // [新增]
        PreviewState.isManipulatingModel = false;
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        // [新增] 列表宽度拖动处理
        if (LayoutState.isResizingSelector) {
            LayoutState.selectorWidth += (int) event.getDragX();
            if (LayoutState.selectorWidth < 60) LayoutState.selectorWidth = 60;
            if (LayoutState.selectorWidth > 400) LayoutState.selectorWidth = 400;
            return;
        }

        if (LayoutState.isEditMode) {
            if (LayoutState.isMovingWindow) {
                LayoutState.xOffset += (int) event.getDragX();
                LayoutState.yOffset += (int) event.getDragY();
            } else if (LayoutState.isResizingWindow) {
                LayoutState.width += (int) event.getDragX();
                LayoutState.height += (int) event.getDragY();
                if (LayoutState.width < LayoutState.MIN_WIDTH) LayoutState.width = LayoutState.MIN_WIDTH;
                if (LayoutState.height < LayoutState.MIN_HEIGHT) LayoutState.height = LayoutState.MIN_HEIGHT;
            }
        } else if (PreviewState.isManipulatingModel) {
            if (PreviewState.dragType == 0) {
                PreviewState.rotY += (float) event.getDragX();
                PreviewState.rotX += (float) event.getDragY();
            } else if (PreviewState.dragType == 1) {
                PreviewState.panX += (float) event.getDragX();
                PreviewState.panY += (float) event.getDragY();
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen screen)) return;
        int viewX = screen.getGuiLeft() + LayoutState.xOffset;
        int viewY = screen.getGuiTop() + LayoutState.yOffset;
        double mx = event.getMouseX();
        double my = event.getMouseY();

        if (SelectorState.isOpen) {
            int listX = viewX + LayoutState.width;
            // [修正] 使用 selectorWidth
            if (isMouseOver(mx, my, listX, viewY, LayoutState.selectorWidth, LayoutState.height)) {
                double scrollY = event.getScrollDeltaY();
                SelectorState.scrollOffset -= (float) (scrollY * SelectorState.ITEM_HEIGHT);
                event.setCanceled(true);
                return;
            }
        }
        if (isMouseOver(mx, my, viewX, viewY, LayoutState.width, LayoutState.height) && !LayoutState.isEditMode) {
            double scrollY = event.getScrollDeltaY();
            if (scrollY != 0) {
                PreviewState.scale += (float) (scrollY * 2.0f);
                if (PreviewState.scale < 1) PreviewState.scale = 1;
                if (PreviewState.scale > 500) PreviewState.scale = 500;
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiClosed(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen) {
            ClientConfig.INSTANCE.xOffset.set(LayoutState.xOffset);
            ClientConfig.INSTANCE.yOffset.set(LayoutState.yOffset);
            ClientConfig.INSTANCE.width.set(LayoutState.width);
            ClientConfig.INSTANCE.height.set(LayoutState.height);
            ClientConfig.INSTANCE.scale.set((double) PreviewState.scale);
            // [新增] 保存列表宽度
            ClientConfig.INSTANCE.selectorWidth.set(LayoutState.selectorWidth);
            ClientConfig.SPEC.save();
        }
    }

    // 工具方法
    private static Entity getCachedEntity(EntityType<?> type) {
        if (Minecraft.getInstance().level == null) return null;
        return ENTITY_CACHE.computeIfAbsent(type, t -> {
            try { return t.create(Minecraft.getInstance().level); } catch (Exception e) { return null; }
        });
    }

    private static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void playSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}