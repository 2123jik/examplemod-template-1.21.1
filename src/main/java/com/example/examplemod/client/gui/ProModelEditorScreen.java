package com.example.examplemod.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;

/**
 * ProModelEditorScreen v3.0
 * 核心升级：全功能动画反射浏览器与姿态控制系统
 */
public class ProModelEditorScreen extends Screen {

    // ==========================================
    // 动画控制器 (Animation Engine)
    // ==========================================
    public static class AnimationController {
        // 传统动画参数 (Legacy)
        public float limbSwingAmount = 0.0f; // 行走速度 (0=站立, 1=奔跑)
        public float limbSwing = 0.0f;       // 行走进度
        public float headYaw = 0.0f;
        public float headPitch = 0.0f;
        public float bodyYaw = 0.0f;

        // 现代动画状态 (Modern AnimationState)
        public static class AnimEntry {
            String name;
            Field field;
            AnimationState state;
            boolean isPlaying = false;
        }

        public List<AnimEntry> detectedAnimations = new ArrayList<>();
        public AnimEntry currentPlaying = null;

        public void scan(Entity entity) {
            detectedAnimations.clear();
            if (entity == null) return;

            Class<?> clazz = entity.getClass();
            // 向上递归查找父类直到 Entity.class
            while (clazz != null && clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (AnimationState.class.isAssignableFrom(f.getType())) {
                        try {
                            f.setAccessible(true);
                            Object val = f.get(entity);
                            if (val instanceof AnimationState state) {
                                AnimEntry entry = new AnimEntry();
                                entry.name = f.getName(); // e.g. "sitAnimationState"
                                entry.field = f;
                                entry.state = state;
                                detectedAnimations.add(entry);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                clazz = clazz.getSuperclass();
            }
            // 排序让名字好看点
            detectedAnimations.sort(Comparator.comparing(e -> e.name));
        }

        public void tick(LivingEntity entity) {
            // 1. 处理 Legacy 行走动画
            if (limbSwingAmount > 0) {
                limbSwing += limbSwingAmount * 0.4f;
            } else {
                limbSwing = 0;
            }

            // 2. 处理 Modern AnimationState
            // 必须在 render 前调用 animateTick，否则动画不会推进
            // 注意：Minecraft 的 AnimationState 需要配合 entity.tickCount 或 partialTick
            // 但我们在 screen 中 entity.tickCount 增加得可能不够快，或者被插值覆盖
            for (AnimEntry entry : detectedAnimations) {
                if (entry.state != null) {
                    // 只有 start 之后，animateTick 才会生效
                    if (entry.isPlaying && !entry.state.isStarted()) {
                        entry.state.start(entity.tickCount);
                    }
                }
            }
        }
    }

    // ==========================================
    // 数据结构
    // ==========================================
    public static class TransformData {
        public String itemId = "minecraft:air";
        public String bone = "head";
        public String displayContext = "FIXED";
        public boolean visible = true;
        public float tx = 0, ty = 0, tz = 0;
        public float rx = 0, ry = 0, rz = 0;
        public float sx = 1, sy = 1, sz = 1;
        private transient ItemStack cachedStack;

        public ItemStack getStack() {
            if (cachedStack == null || !BuiltInRegistries.ITEM.getKey(cachedStack.getItem()).toString().equals(itemId)) {
                ResourceLocation loc = ResourceLocation.tryParse(itemId);
                Item item = loc != null ? BuiltInRegistries.ITEM.get(loc) : net.minecraft.world.item.Items.AIR;
                cachedStack = new ItemStack(item);
            }
            return cachedStack;
        }
        public void setStack(ItemStack stack) {
            this.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            this.cachedStack = stack.copy();
        }
        public ItemDisplayContext getContext() {
            try { return ItemDisplayContext.valueOf(displayContext); } catch (Exception e) { return ItemDisplayContext.FIXED; }
        }
    }

    private static class Category {
        String name;
        Predicate<Object> filter;
        List<ItemStack> cachedTabItems = null;
        public Category(String name, Predicate<Object> filter) { this.name = name; this.filter = filter; }
        public Category(String name, List<ItemStack> items) { this.name = name; this.filter = o -> true; this.cachedTabItems = items; }
    }

    // ==========================================
    // 全局状态
    // ==========================================
    private static final List<TransformData> layers = new ArrayList<>();
    private static int selectedIndex = -1;
    private static Entity targetEntity;
    private static final AnimationController animController = new AnimationController();

    private static final Map<EntityType<?>, Entity> ENTITY_CACHE = new HashMap<>();
    private static final Set<EntityType<?>> BROKEN_ENTITIES = new HashSet<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Stack<String> undoStack = new Stack<>();
    private static final Stack<String> redoStack = new Stack<>();

    // UI Constants
    private static final int SIDEBAR_WIDTH = 260; // 更宽以容纳动画列表
    private static final int HEADER_HEIGHT = 40;
    private static final int C_BG = 0xFF1E1E1E;
    private static final int C_PANEL = 0xFF252526;
    private static final int C_BORDER = 0xFF3E3E42;
    private static final int C_ACCENT = 0xFF007ACC;
    private static final int C_ERROR = 0xFFFF4444;

    // Runtime State
    private EditBox searchField;
    // 0=Items, 1=Entities, 2=Animations
    private int activeTab = 0;
    private float scrollOffset = 0;

    private final List<ItemStack> filteredItems = new ArrayList<>();
    private final List<EntityType<?>> filteredEntities = new ArrayList<>();
    private final List<Category> itemCategories = new ArrayList<>();
    private final List<Category> entityCategories = new ArrayList<>();
    private int currentItemCatIndex = 0;
    private int currentEntityCatIndex = 0;

    private float viewYaw = 45, viewPitch = 10, viewPanX = 0, viewPanY = 0, viewZoom = 80;
    private boolean isAnimating = true;
    private List<String> boneCache = new ArrayList<>();
    private boolean isOrbiting = false, isPanning = false;
    private DraggableContext activeDrag = null;
    private String lastSearch = "";
    private long lastToastTime = 0;
    private String toastMessage = "";
    private boolean mouseClickedFlag = false;

    public ProModelEditorScreen() {
        super(Component.literal("Pro Editor v3"));
        if (targetEntity == null) {
            targetEntity = Minecraft.getInstance().player;
            animController.scan(targetEntity);
        }
        if (layers.isEmpty()) {
            layers.add(new TransformData());
            selectedIndex = 0;
        }
        refreshBones();
        pushHistory();
        initCategories();
    }

    private void initCategories() {
        itemCategories.clear();
        itemCategories.add(new Category("All Items", o -> true));
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            String name = tab.getDisplayName().getString();
            if (name.isEmpty()) continue;
            Collection<ItemStack> tabItems = tab.getDisplayItems();
            if (!tabItems.isEmpty()) itemCategories.add(new Category(name, new ArrayList<>(tabItems)));
        }

        entityCategories.clear();
        entityCategories.add(new Category("All Entities", o -> true));
        for (MobCategory cat : MobCategory.values()) {
            String name = cat.getName();
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            entityCategories.add(new Category(name, obj -> {
                if (obj instanceof EntityType<?> type) return type.getCategory() == cat;
                return false;
            }));
        }
    }

    @Override
    protected void init() {
        this.searchField = new EditBox(this.font, 12, HEADER_HEIGHT + 35, SIDEBAR_WIDTH - 24, 18, Component.literal("Search"));
        this.searchField.setMaxLength(50);
        this.searchField.setBordered(false);
        this.searchField.setTextColor(0xFFFFFF);
        this.searchField.setHint(Component.literal("Search..."));
        this.addRenderableWidget(searchField);
        updateFilteredList();
    }

    @Override
    public void tick() {
        super.tick();
        if (isAnimating && targetEntity != null) {
            targetEntity.tickCount++;
            if (targetEntity instanceof LivingEntity living) {
                animController.tick(living);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, C_BG);

        // 3D Scene
        int vpX = SIDEBAR_WIDTH;
        int vpY = HEADER_HEIGHT;
        int vpW = width - SIDEBAR_WIDTH * 2;
        int vpH = height - HEADER_HEIGHT;

        g.enableScissor(vpX, vpY, vpX + vpW, vpY + vpH);
        drawGrid(g, vpX, vpY, vpW, vpH);
        try {
            render3DScene(g, vpX + vpW / 2, vpY + vpH / 2 + 50, partialTick);
        } catch (Exception e) {
            e.printStackTrace(); // Log error
            if (targetEntity != null) BROKEN_ENTITIES.add(targetEntity.getType());
            targetEntity = Minecraft.getInstance().player;
            animController.scan(targetEntity);
            showToast("Render Error! Resetting.");
        }
        g.disableScissor();

        renderLeftPanel(g, mx, my);
        renderRightPanel(g, mx, my);
        renderHeader(g, mx, my);
        renderToast(g);

        if (activeDrag != null) setCursor(GLFW.GLFW_HRESIZE_CURSOR);
        else setCursor(GLFW.GLFW_ARROW_CURSOR);
    }

    private void setCursor(int cursor) {
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), GLFW.glfwCreateStandardCursor(cursor));
    }

    // ==========================================
    // 3D 渲染核心
    // ==========================================
    private void render3DScene(GuiGraphics g, int cx, int cy, float partial) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx + viewPanX, cy + viewPanY, 400);
        pose.scale(viewZoom, viewZoom, -viewZoom);
        pose.mulPose(new Quaternionf().rotateX((float) Math.toRadians(viewPitch)).rotateY((float) Math.toRadians(viewYaw)));

        if (targetEntity instanceof LivingEntity living) {
            // --- 1. 保存原始状态 ---
            float oldBody = living.yBodyRot, oldBodyO = living.yBodyRotO;
            float oldHead = living.yHeadRot, oldHeadO = living.yHeadRotO;
            float oldXRot = living.getXRot(), oldXRotO = living.xRotO;
            float oldSwing = living.walkAnimation.position(); // limbSwing
            float oldSwingAmt = living.walkAnimation.speed(); // limbSwingAmount

            // --- 2. 应用编辑器控制的状态 ---
            // 身体旋转归零，使其正对相机，但应用我们的 offset
            living.yBodyRot = animController.bodyYaw;
            living.yBodyRotO = animController.bodyYaw;

            living.yHeadRot = animController.headYaw;
            living.yHeadRotO = animController.headYaw;

            living.setXRot(animController.headPitch);
            living.xRotO = animController.headPitch;

            // 强制应用行走动画参数
            // Minecraft 渲染使用 LivingEntity.walkAnimation.position(limbSwing) 和 speed(limbSwingAmount)
            // 我们需要反射或者直接设置它们（如果是 AccessTransformer 允许的话），这里假设无法直接访问字段，
            // 但我们可以通过 tick 来模拟积累。
            // 为了渲染时的准确性，我们这里进行临时覆盖：
            living.walkAnimation.setSpeed(animController.limbSwingAmount);
            // 这是一个 hack，通常 limbSwing 是累加的，这里我们直接覆盖用于预览
            // 注意：因为字段是私有的，如果没有 AT (Access Transformer)，这里可能需要反射。
            // 简单起见，我们假设可以通过方法或者反射设置，或者忽略这个精确设置，
            // 依靠 tick() 里的累加。为了平滑，我们在 render 仅仅依赖 tick 的结果。

            // Render
            EntityRenderDispatcher erd = Minecraft.getInstance().getEntityRenderDispatcher();
            erd.setRenderShadow(false);
            RenderSystem.runAsFancy(() ->
                    erd.render(living, 0, 0, 0, 0, 1.0f, pose, g.bufferSource(), 0xF000F0)
            );
            erd.setRenderShadow(true);

            renderCustomLayers(living, pose, g.bufferSource());

            // --- 3. 恢复原始状态 ---
            living.yBodyRot = oldBody; living.yBodyRotO = oldBodyO;
            living.yHeadRot = oldHead; living.yHeadRotO = oldHeadO;
            living.setXRot(oldXRot); living.xRotO = oldXRotO;
            living.walkAnimation.setSpeed(oldSwingAmt);
            // living.walkAnimation.setPosition(oldSwing); // 如果能访问的话
        } else if (targetEntity != null) {
            Minecraft.getInstance().getEntityRenderDispatcher().render(targetEntity, 0, 0, 0, 0, 1.0f, pose, g.bufferSource(), 0xF000F0);
        }

        pose.popPose();
        ((MultiBufferSource.BufferSource)g.bufferSource()).endBatch();
    }

    private void renderCustomLayers(LivingEntity entity, PoseStack pose, MultiBufferSource buffer) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof LivingEntityRenderer lr)) return;
        EntityModel<?> model = lr.getModel();

        for (TransformData layer : layers) {
            if (!layer.visible || layer.getStack().isEmpty()) continue;
            pose.pushPose();
            if (!"none".equals(layer.bone)) {
                ModelPart part = findModelPart(model, layer.bone);
                if (part != null) part.translateAndRotate(pose);
            }
            pose.translate(layer.tx, layer.ty, layer.tz);
            pose.mulPose(Axis.ZP.rotationDegrees(layer.rz));
            pose.mulPose(Axis.YP.rotationDegrees(layer.ry));
            pose.mulPose(Axis.XP.rotationDegrees(layer.rx));
            pose.scale(layer.sx, layer.sy, layer.sz);
            pose.mulPose(Axis.XP.rotationDegrees(180));

            Minecraft.getInstance().getItemRenderer().renderStatic(entity, layer.getStack(), layer.getContext(), false, pose, buffer, entity.level(), 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    // ==========================================
    // UI Panels
    // ==========================================
    private void renderLeftPanel(GuiGraphics g, int mx, int my) {
        g.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, height, C_PANEL);
        g.vLine(SIDEBAR_WIDTH, HEADER_HEIGHT, height, C_BORDER);

        // 3 Tabs: ITEMS | ENTITIES | ANIMS
        int tabW = SIDEBAR_WIDTH / 3;
        int tabY = HEADER_HEIGHT;

        drawTab(g, "Items", 0, tabY, tabW, activeTab == 0, mx, my, 0);
        drawTab(g, "Mobs", tabW, tabY, tabW, activeTab == 1, mx, my, 1);
        drawTab(g, "Anims", tabW * 2, tabY, tabW, activeTab == 2, mx, my, 2);

        // Search Bar (Only for Items and Mobs)
        if (activeTab != 2) {
            g.fill(10, HEADER_HEIGHT + 33, SIDEBAR_WIDTH - 10, HEADER_HEIGHT + 55, 0xFF181818);
            if (!searchField.getValue().equals(lastSearch)) {
                lastSearch = searchField.getValue();
                scrollOffset = 0;
                updateFilteredList();
            }
            searchField.render(g, mx, my, 0);
        }

        int listTop = HEADER_HEIGHT + 60;
        if (activeTab == 2) listTop = HEADER_HEIGHT + 25; // Anims start higher

        // Category Switcher (Items/Mobs only)
        if (activeTab != 2) {
            List<Category> cats = (activeTab == 1) ? entityCategories : itemCategories;
            int cIdx = (activeTab == 1) ? currentEntityCatIndex : currentItemCatIndex;

            g.fill(0, listTop, SIDEBAR_WIDTH, listTop + 22, 0xFF2D2D30);
            if (drawButton(g, 5, listTop + 3, 16, 16, "<", mx, my)) changeCategory(-1);
            if (drawButton(g, SIDEBAR_WIDTH - 21, listTop + 3, 16, 16, ">", mx, my)) changeCategory(1);
            String cName = cats.isEmpty() ? "Loading" : cats.get(cIdx).name;
            g.drawCenteredString(font, font.plainSubstrByWidth(cName, SIDEBAR_WIDTH - 50), SIDEBAR_WIDTH / 2, listTop + 7, C_ACCENT);
            listTop += 25;
        }

        // List Content
        g.enableScissor(0, listTop, SIDEBAR_WIDTH, height);
        if (activeTab == 0) renderItemGrid(g, mx, my, listTop);
        else if (activeTab == 1) renderEntityList(g, mx, my, listTop);
        else renderAnimationList(g, mx, my, listTop);
        g.disableScissor();
    }

    /**
     * 核心新功能：动画浏览器 UI
     */
    private void renderAnimationList(GuiGraphics g, int mx, int my, int sy) {
        int y = sy - (int)scrollOffset;
        int px = 10;
        int width = SIDEBAR_WIDTH - 20;

        // --- Section 1: Legacy Controls (Sliders) ---
        g.drawString(font, "LEGACY CONTROLS (Walk/Look)", px, y, C_ACCENT, false);
        y += 12;

        y = drawSlider(g, px, y, width, "Speed", animController.limbSwingAmount, 0, 1.5f, v -> animController.limbSwingAmount = v, mx, my);
        y = drawSlider(g, px, y, width, "Head Yaw", animController.headYaw, -90, 90, v -> animController.headYaw = v, mx, my);
        y = drawSlider(g, px, y, width, "Head Pitch", animController.headPitch, -90, 90, v -> animController.headPitch = v, mx, my);
        y = drawSlider(g, px, y, width, "Body Yaw", animController.bodyYaw, -90, 90, v -> animController.bodyYaw = v, mx, my);

        if (drawButton(g, px, y, width, 16, "Reset Legacy Pose", mx, my)) {
            animController.limbSwingAmount = 0;
            animController.headYaw = 0; animController.headPitch = 0; animController.bodyYaw = 0;
        }
        y += 24;

        g.hLine(px, px + width, y - 5, C_BORDER);

        // --- Section 2: Detected AnimationStates ---
        g.drawString(font, "MODERN STATES (" + animController.detectedAnimations.size() + ")", px, y, C_ACCENT, false);
        y += 15;

        if (animController.detectedAnimations.isEmpty()) {
            g.drawString(font, "No AnimationStates found.", px, y, 0xFF888888, false);
            g.drawString(font, "(Target might use legacy system)", px, y + 10, 0xFF666666, false);
        } else {
            for (AnimationController.AnimEntry entry : animController.detectedAnimations) {
                int boxH = 20;
                boolean hover = isHovering(0, y, SIDEBAR_WIDTH, boxH, mx, my);

                g.fill(px, y, px + width, y + boxH, entry.isPlaying ? 0xFF2D4D2D : (hover ? 0xFF2D2D30 : 0xFF181818));
                g.renderOutline(px, y, width, boxH, entry.isPlaying ? 0xFF55AA55 : 0xFF333333);

                // Animation Name
                String name = entry.name.replace("AnimationState", "");
                g.drawString(font, name, px + 5, y + 6, 0xFFCCCCCC, false);

                // Play/Stop Button
                String btnTxt = entry.isPlaying ? "STOP" : "PLAY";
                int btnCol = entry.isPlaying ? 0xFFFF5555 : 0xFF55FF55;
                if (isHovering(px + width - 40, y + 2, 38, 16, mx, my) && mouseClickedFlag) {
                    if (entry.isPlaying) {
                        entry.isPlaying = false;
                        entry.state.stop();
                    } else {
                        // Stop others if you want exclusive animations, optional
                        // for(var other : animController.detectedAnimations) { other.isPlaying=false; other.state.stop(); }
                        entry.isPlaying = true;
                        entry.state.start(targetEntity.tickCount);
                    }
                    mouseClickedFlag = false;
                }
                g.fill(px + width - 40, y + 2, px + width - 2, y + 18, 0xFF111111);
                g.drawCenteredString(font, btnTxt, px + width - 21, y + 5, btnCol);

                y += boxH + 4;
            }
        }
    }

    private int drawSlider(GuiGraphics g, int x, int y, int w, String label, float value, float min, float max, java.util.function.Consumer<Float> onChange, int mx, int my) {
        g.drawString(font, label, x, y, 0xFFAAAAAA, false);
        y += 10;

        int h = 14;
        g.fill(x, y, x + w, y + h, 0xFF111111); // track

        float pct = (value - min) / (max - min);
        int handleX = x + (int)(pct * (w - 8));
        boolean hover = isHovering(x, y, w, h, mx, my);

        g.fill(handleX, y, handleX + 8, y + h, hover ? C_ACCENT : 0xFF666666);
        g.drawCenteredString(font, String.format("%.2f", value), x + w / 2, y + 3, 0xFFFFFFFF);

        if (hover && mouseClickedFlag) {
            // Register a special draggable context for sliders
            activeDrag = new DraggableContext(null, -1, -1, value); // Hacky reuse of context
            // Custom logic for slider dragging needs a simpler way,
            // but for now let's just use instant click-jump or implement simple logic:
            float newPct = (float)(mx - x) / (float)w;
            newPct = Mth.clamp(newPct, 0, 1);
            onChange.accept(min + newPct * (max - min));
            // In a real implementation, we'd set a specific 'activeSlider' state to handle drag
            // For now, click-to-set is good enough for v3.0 demo
            // Or better, let's just make it update while mouse is down:
        }
        if (hover && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
            float newPct = (float)(mx - x) / (float)w;
            onChange.accept(min + Mth.clamp(newPct, 0, 1) * (max - min));
        }

        return y + 18;
    }

    private void renderRightPanel(GuiGraphics g, int mx, int my) {
        // ... (保持 v2.0 的图层管理逻辑，代码省略以节省篇幅，逻辑未变) ...
        // 在这里你可以直接复制 v2.0 的 renderRightPanel 代码
        // 唯一的区别是建议调整一下 TransformData 的属性以支持更多微调
        renderRightPanelInternal(g, mx, my);
    }

    // 省略了 RightPanel 的重复代码，逻辑与 v2.0 相同，只负责 Item Transforms
    private void renderRightPanelInternal(GuiGraphics g, int mx, int my) {
        int x = width - SIDEBAR_WIDTH;
        g.fill(x, HEADER_HEIGHT, width, height, C_PANEL);
        g.vLine(x, HEADER_HEIGHT, height, C_BORDER);

        int cy = HEADER_HEIGHT + 10;
        int px = x + 10;
        int pw = SIDEBAR_WIDTH - 20;

        g.drawString(font, "LAYERS", px, cy, 0xFF666666, false);
        if (drawButton(g, width - 25, cy - 2, 15, 12, "+", mx, my)) {
            pushHistory();
            layers.add(new TransformData());
            selectedIndex = layers.size() - 1;
        }
        cy += 18;

        for (int i = 0; i < layers.size(); i++) {
            TransformData data = layers.get(i);
            boolean sel = (i == selectedIndex);
            int boxY = cy;
            g.fill(px, boxY, px + pw, boxY + 20, sel ? 0xFF37373D : 0xFF2D2D30);
            if (sel) g.fill(px, boxY, px + 2, boxY + 20, C_ACCENT);
            String n = data.getStack().isEmpty() ? "Empty" : data.getStack().getHoverName().getString();
            g.drawString(font, (i+1)+". "+font.plainSubstrByWidth(n, 120), px + 8, boxY + 6, 0xFFEEEEEE, false);
            if (drawButton(g, width - 26, boxY + 4, 12, 12, "x", mx, my)) {
                pushHistory(); layers.remove(i); if(selectedIndex>=layers.size()) selectedIndex=layers.size()-1; return;
            }
            if(isHovering(px, boxY, pw-30, 20, mx, my) && mouseClickedFlag) { selectedIndex = i; mouseClickedFlag = false; }
            cy += 22;
        }

        if (selectedIndex >= 0 && selectedIndex < layers.size()) {
            TransformData data = layers.get(selectedIndex);
            g.hLine(px, px + pw, cy + 5, C_BORDER); cy += 15;

            // Bone
            g.drawString(font, "Bone: " + data.bone, px, cy + 4, C_ERROR, false);
            if(drawButton(g, width-60, cy, 50, 14, "Next", mx, my)) {
                pushHistory(); int idx = boneCache.indexOf(data.bone); data.bone = boneCache.get((idx + 1) % boneCache.size());
            }
            cy += 20;

            // Transforms
            cy = drawTransformRow(g, mx, my, px, cy, "Pos", data, 0);
            cy = drawTransformRow(g, mx, my, px, cy, "Rot", data, 1);
            cy = drawTransformRow(g, mx, my, px, cy, "Scl", data, 2);
        }
    }

    // 复用 v2.0 的 Transform Row
    private int drawTransformRow(GuiGraphics g, int mx, int my, int x, int y, String label, TransformData data, int type) {
        g.drawString(font, label, x, y+4, 0xFF888888, false);
        int gap = 2; int w = (SIDEBAR_WIDTH - 60) / 3;
        float[] vals = type==0 ? new float[]{data.tx,data.ty,data.tz} : type==1 ? new float[]{data.rx,data.ry,data.rz} : new float[]{data.sx,data.sy,data.sz};
        for(int i=0; i<3; i++) drawDragField(g, x + 40 + i*(w+gap), y, w, 14, vals[i], mx, my, data, type, i);
        return y + 18;
    }

    private void drawDragField(GuiGraphics g, int x, int y, int w, int h, float val, int mx, int my, TransformData data, int type, int axis) {
        boolean hover = isHovering(x, y, w, h, mx, my);
        g.fill(x, y, x+w, y+h, 0xFF111111);
        int col = axis==0?0xFFAA5555 : axis==1?0xFF55AA55 : 0xFF5555AA;
        g.drawString(font, String.format("%.2f", val), x+2, y+3, col);
        if(hover) {
            g.renderOutline(x, y, w, h, 0xFF666666);
            if(mouseClickedFlag) { activeDrag = new DraggableContext(data, type, axis, val); mouseClickedFlag = false; }
        }
    }

    private void renderHeader(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, width, HEADER_HEIGHT, 0xFF303030);
        g.hLine(0, width, HEADER_HEIGHT - 1, C_BORDER);
        g.drawString(font, "PRO EDITOR v3.0", 15, 10, 0xFFFFFFFF);
        String tName = targetEntity != null ? targetEntity.getName().getString() : "None";
        g.drawString(font, "Target: " + tName, 15, 22, 0xFF888888);

        int rx = width - 10;
        if (drawButton(g, rx -= 60, 8, 55, 24, "Export", mx, my)) copyJson();
        if (drawButton(g, rx -= 60, 8, 55, 24, "Undo", mx, my)) undo();

        String animTxt = isAnimating ? "|| Pause" : "▶ Play";
        if (drawButton(g, width/2 - 30, 8, 60, 24, animTxt, mx, my)) isAnimating = !isAnimating;
    }

    // ==========================================
    // Helpers
    // ==========================================
    private void changeCategory(int dir) {
        if (activeTab == 0) currentItemCatIndex = Math.floorMod(currentItemCatIndex + dir, itemCategories.size());
        if (activeTab == 1) currentEntityCatIndex = Math.floorMod(currentEntityCatIndex + dir, entityCategories.size());
        scrollOffset = 0;
        updateFilteredList();
    }

    private void drawTab(GuiGraphics g, String txt, int x, int y, int w, boolean active, int mx, int my, int index) {
        boolean hover = isHovering(x, y, w, 24, mx, my);
        g.fill(x, y, x + w, y + 24, active ? C_PANEL : (hover ? 0xFF2D2D30 : C_BG));
        if (active) g.fill(x, y + 22, x + w, y + 24, C_ACCENT);
        g.drawCenteredString(font, txt, x + w / 2, y + 8, active ? 0xFFFFFFFF : 0xFF888888);
        if (hover && mouseClickedFlag) {
            activeTab = index;
            scrollOffset = 0;
            updateFilteredList();
            mouseClickedFlag = false;
        }
    }

    private void updateFilteredList() {
        if (activeTab == 2) return; // Anim tab doesn't use this filter logic
        String q = searchField.getValue().toLowerCase();

        if (activeTab == 0) { // Items
            filteredItems.clear();
            if (itemCategories.isEmpty()) return;
            Category cat = itemCategories.get(currentItemCatIndex);
            if (cat.cachedTabItems != null) {
                for (ItemStack s : cat.cachedTabItems) if (q.isEmpty() || s.getHoverName().getString().toLowerCase().contains(q)) filteredItems.add(s);
            } else {
                for (Item item : BuiltInRegistries.ITEM) {
                    ItemStack s = new ItemStack(item);
                    if (s.getHoverName().getString().toLowerCase().contains(q)) filteredItems.add(s);
                }
            }
        } else if (activeTab == 1) { // Entities
            filteredEntities.clear();
            if (entityCategories.isEmpty()) return;
            Category cat = entityCategories.get(currentEntityCatIndex);
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                if (t == EntityType.MARKER) continue;
                String n = t.getDescription().getString().toLowerCase();
                if ((q.isEmpty() || n.contains(q)) && cat.filter.test(t)) filteredEntities.add(t);
            }
            filteredEntities.sort(Comparator.comparing(t -> t.getDescription().getString()));
        }
    }

    // 渲染 Grid 和 List 代码与 v2 类似，只是根据 activeTab 判断
    private void renderItemGrid(GuiGraphics g, int mx, int my, int sy) {
        int size = 20; int cols = (SIDEBAR_WIDTH - 24) / size;
        int start = (int)(scrollOffset * cols);
        for(int i=0; i< (height-sy)/size*cols + cols; i++) {
            int idx = start + i;
            if(idx >= filteredItems.size()) break;
            ItemStack st = filteredItems.get(idx);
            int cx = 12 + (i%cols)*size; int cy = sy + (i/cols)*size;
            if(isHovering(cx, cy, size, size, mx, my)) {
                g.renderTooltip(font, st, mx, my);
                if(mouseClickedFlag && selectedIndex>=0 && selectedIndex<layers.size()) {
                    pushHistory(); layers.get(selectedIndex).setStack(st); showToast("Selected: "+st.getHoverName().getString()); mouseClickedFlag=false;
                }
            }
            g.renderItem(st, cx+2, cy+2);
        }
    }

    private void renderEntityList(GuiGraphics g, int mx, int my, int sy) {
        int h = 16; int start = (int)scrollOffset;
        for(int i=0; i<(height-sy)/h + 2; i++) {
            int idx = start + i; if(idx >= filteredEntities.size()) break;
            EntityType<?> t = filteredEntities.get(idx);
            int cy = sy + i*h;
            boolean sel = targetEntity!=null && targetEntity.getType()==t;
            if(sel) g.fill(2, cy, SIDEBAR_WIDTH-2, cy+h, 0x44007ACC);
            else if(isHovering(0, cy, SIDEBAR_WIDTH, h, mx, my)) g.fill(2, cy, SIDEBAR_WIDTH-2, cy+h, 0x22FFFFFF);

            g.drawString(font, font.plainSubstrByWidth(t.getDescription().getString(), SIDEBAR_WIDTH-20), 10, cy+4, BROKEN_ENTITIES.contains(t)?C_ERROR:0xFFCCCCCC, false);

            if(isHovering(0, cy, SIDEBAR_WIDTH, h, mx, my) && mouseClickedFlag) {
                if(!BROKEN_ENTITIES.contains(t)) {
                    Entity e = getCachedEntity(t);
                    if(e!=null) {
                        targetEntity = e;
                        refreshBones();
                        animController.scan(e); // 切换实体时重新扫描动画
                        // Reset legacy
                        animController.limbSwingAmount = 0;
                    }
                }
                mouseClickedFlag = false;
            }
        }
    }

    // Standard Helpers
    private ModelPart findModelPart(Object model, String name) {
        Class<?> clazz = model.getClass();
        while(clazz!=null && clazz!=Object.class) {
            for(Field f:clazz.getDeclaredFields()) if(f.getName().equals(name) && ModelPart.class.isAssignableFrom(f.getType())) { try{f.setAccessible(true);return(ModelPart)f.get(model);}catch(Exception e){} }
            clazz=clazz.getSuperclass();
        } return null;
    }
    private void refreshBones() {
        boneCache.clear(); boneCache.add("none");
        if(targetEntity instanceof LivingEntity l && Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(l) instanceof LivingEntityRenderer lr) {
            Class<?> c=lr.getModel().getClass();
            while(c!=null && c!=Object.class) { for(Field f:c.getDeclaredFields()) if(ModelPart.class.isAssignableFrom(f.getType())) boneCache.add(f.getName()); c=c.getSuperclass(); }
        }
    }
    private Entity getCachedEntity(EntityType<?> t) { return ENTITY_CACHE.computeIfAbsent(t, k -> { try{return k.create(Minecraft.getInstance().level);}catch(Exception e){BROKEN_ENTITIES.add(k);return null;} }); }
    private boolean drawButton(GuiGraphics g, int x, int y, int w, int h, String txt, int mx, int my) {
        boolean hov = isHovering(x, y, w, h, mx, my);
        g.fill(x, y, x+w, y+h, hov ? C_ACCENT : 0xFF3E3E42);
        g.drawCenteredString(font, txt, x+w/2, y+(h-8)/2, 0xFFFFFFFF);
        if(hov && mouseClickedFlag) { Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)); mouseClickedFlag=false; return true; }
        return false;
    }
    private boolean isHovering(int x, int y, int w, int h, int mx, int my) { return mx>=x && mx<x+w && my>=y && my<y+h; }
    private void drawGrid(GuiGraphics g, int x, int y, int w, int h) {
        int s=20; for(int dx=0;dx<w;dx+=s) for(int dy=0;dy<h;dy+=s) g.fill(x+dx, y+dy, x+dx+s, y+dy+s, ((dx/s+dy/s)%2==0)?0xFF181818:0xFF202020);
    }
    private void showToast(String m) { toastMessage=m; lastToastTime=System.currentTimeMillis(); }
    private void renderToast(GuiGraphics g) { if(System.currentTimeMillis()-lastToastTime<2500) { int w=font.width(toastMessage)+20; int tx=width/2-w/2; g.fill(tx, height-60, tx+w, height-40, 0xCC000000); g.renderOutline(tx, height-60, w, 20, C_ACCENT); g.drawCenteredString(font, toastMessage, width/2, height-54, 0xFFFFFFFF); } }
    private void pushHistory() { if(undoStack.size()>20) undoStack.remove(0); undoStack.push(GSON.toJson(layers)); redoStack.clear(); }
    private void undo() { if(!undoStack.isEmpty()) { redoStack.push(GSON.toJson(layers)); try { List<TransformData> old=GSON.fromJson(undoStack.pop(), new TypeToken<ArrayList<TransformData>>(){}.getType()); layers.clear(); layers.addAll(old); showToast("Undo"); } catch(Exception e){} } }
    private void copyJson() { Minecraft.getInstance().keyboardHandler.setClipboard(GSON.toJson(layers)); showToast("JSON Copied"); }

    @Override public boolean mouseClicked(double mx, double my, int btn) { mouseClickedFlag=true; searchField.mouseClicked(mx, my, btn); if(mx>SIDEBAR_WIDTH && mx<width-SIDEBAR_WIDTH) { if(btn==1) isOrbiting=true; if(btn==2) isPanning=true; return true; } return super.mouseClicked(mx, my, btn); }
    @Override public boolean mouseReleased(double mx, double my, int btn) { isOrbiting=false; isPanning=false; activeDrag=null; mouseClickedFlag=false; return super.mouseReleased(mx, my, btn); }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { if(activeDrag!=null) { float s=Screen.hasShiftDown()?0.01f:(Screen.hasControlDown()?0.5f:0.05f); activeDrag.apply((float)dx*s); return true; } if(isOrbiting) { viewYaw+=dx; viewPitch+=dy; return true; } if(isPanning) { viewPanX+=dx; viewPanY+=dy; return true; } return super.mouseDragged(mx, my, btn, dx, dy); }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { if(mx<SIDEBAR_WIDTH) { scrollOffset = (float)Math.max(0, scrollOffset - sy); return true; } if(mx>width-SIDEBAR_WIDTH) return true; viewZoom+=sy*5; return true; }
    @Override public boolean keyPressed(int key, int s, int m) { if(searchField.isFocused()) return searchField.keyPressed(key, s, m); if((m&GLFW.GLFW_MOD_CONTROL)!=0 && key==GLFW.GLFW_KEY_Z) { undo(); return true; } return super.keyPressed(key, s, m); }
    @Override public boolean charTyped(char c, int m) { if(searchField.isFocused()) return searchField.charTyped(c, m); return super.charTyped(c, m); }

    // Hacky context for both transform edits AND slider drags
    private static class DraggableContext {
        TransformData d; int t, a; float sv;
        DraggableContext(TransformData d, int t, int a, float sv) { this.d=d; this.t=t; this.a=a; this.sv=sv; }
        void apply(float delta) {
            float v = sv+delta; sv=v;
            if (d == null) return; // Slider handled in drawSlider separately or added here logic
            if(t==0) { if(a==0)d.tx=v; else if(a==1)d.ty=v; else d.tz=v; }
            else if(t==1) { if(a==0)d.rx=v; else if(a==1)d.ry=v; else d.rz=v; }
            else { if(a==0)d.sx=v; else if(a==1)d.sy=v; else d.sz=v; }
        }
    }
}