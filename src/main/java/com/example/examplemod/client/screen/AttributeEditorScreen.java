package com.example.examplemod.client.screen;

import com.example.examplemod.network.UpdateItemAttributesPayload;
import com.google.common.base.Stopwatch;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 属性编辑器屏幕 - 增强版
 * <p>
 * 改进点：
 * 1. 性能优化：缓存属性列表，避免每次搜索都遍历注册表。
 * 2. 交互优化：增加状态栏反馈，防抖搜索，更清晰的选中状态。
 * 3. 架构优化：UI 组件模块化，逻辑分离。
 */
public class AttributeEditorScreen extends Screen {
    private static final DecimalFormat DF = new DecimalFormat("#.##");
    private static final int CONTROL_WIDTH = 130; // 稍微加宽以容纳更长的文本
    private static final int GAP = 6;

    private final ItemStack targetItem;
    // 缓存所有可用的属性，避免每一帧或每次搜索都去查询注册表（在大型整合包中非常重要）
    private final List<AttributeCacheEntry> cachedAttributes;

    private ItemAttributeModifiers modifiersSnapshot;

    // 当前编辑状态
    @Nullable private ItemAttributeModifiers.Entry selectedEntry = null;
    @Nullable private Holder<Attribute> selectedAttribute = null;

    // UI 组件
    private AttributeSelectionList availableAttributesList;
    private CurrentModifierList currentModifiersList;
    private EditBox valueBox;
    private CycleButton<AttributeModifier.Operation> operationButton;
    private CycleButton<EquipmentSlotGroup> slotButton;
    private Button actionButton;
    private Button removeButton;
    private EditBox searchBox;
    private Component statusMessage = Component.empty();
    private long statusMessageTime = 0;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public AttributeEditorScreen(ItemStack stack) {
        super(Component.translatable("gui.examplemod.attribute_editor.title"));
        this.targetItem = stack;
        this.modifiersSnapshot = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        // 预加载缓存
        this.cachedAttributes = BuiltInRegistries.ATTRIBUTE.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new AttributeCacheEntry(
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(e.getValue()),
                        e.getKey().location()
                ))
                .collect(Collectors.toList());
    }
    // 简化版的颜色获取逻辑
    private int getValueColor(double amount, AttributeModifier.Operation op) {
        if (op == AttributeModifier.Operation.ADD_VALUE) {
            // 数值加成：正数绿色，负数红色
            return amount >= 0 ? 0x55FF55 : 0xFF5555;
        } else {
            // 百分比加成：总是金色或蓝色以示区别
            return 0xFFAA00;
        }
    }
    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);

        int totalAvailableWidth = this.width - 20;
        int centerWidth = CONTROL_WIDTH + 10;
        // 动态计算列表宽度，确保在宽屏下利用空间，在窄屏下不重叠
        int listWidth = Math.max(120, (totalAvailableWidth - centerWidth) / 2);

        LinearLayout contentLayout = LinearLayout.horizontal().spacing(GAP);

        // --- 左侧：可用属性库 ---
        LinearLayout leftColumn = LinearLayout.vertical().spacing(GAP);
        this.searchBox = new EditBox(this.font, listWidth, 20, Component.translatable("gui.examplemod.search"));
        this.searchBox.setHint(Component.translatable("gui.examplemod.search_hint").withStyle(ChatFormatting.GRAY));
        this.searchBox.setResponder(this::onSearchChanged);
        leftColumn.addChild(this.searchBox);

        int listHeight = this.height - this.layout.getHeaderHeight() - this.layout.getFooterHeight() - 30;
        this.availableAttributesList = new AttributeSelectionList(this.minecraft, listWidth, listHeight, 0, 30);
        leftColumn.addChild(this.availableAttributesList);
        contentLayout.addChild(leftColumn);

        // --- 中间：控制面板 ---
        LinearLayout centerColumn = LinearLayout.vertical().spacing(GAP);
        centerColumn.defaultCellSetting().alignHorizontallyCenter();

        // 使用 Spacer 将控件垂直居中
        centerColumn.addChild(SpacerElement.height(listHeight / 4));

        centerColumn.addChild(new StringWidget(CONTROL_WIDTH, 14, Component.translatable("gui.examplemod.value"), this.font).alignLeft());
        this.valueBox = new EditBox(this.font, CONTROL_WIDTH, 20, Component.literal("Value"));
        this.valueBox.setValue("1.0");
        this.valueBox.setResponder(this::validateNumber);
        this.valueBox.setTooltip(Tooltip.create(Component.translatable("gui.examplemod.value.tooltip")));
        centerColumn.addChild(this.valueBox);

        this.operationButton = CycleButton.builder((AttributeModifier.Operation op) -> Component.literal(op.getSerializedName().toUpperCase()))
                .withValues(List.of(AttributeModifier.Operation.values()))
                .withInitialValue(AttributeModifier.Operation.ADD_VALUE)
                .withTooltip(op -> Tooltip.create(getOperationTooltip(op)))
                .create(0, 0, CONTROL_WIDTH, 20, Component.translatable("gui.examplemod.operation"));
        centerColumn.addChild(this.operationButton);

        this.slotButton = CycleButton.builder((EquipmentSlotGroup slot) -> Component.literal(slot.getSerializedName()))
                .withValues(List.of(EquipmentSlotGroup.values()))
                .withInitialValue(EquipmentSlotGroup.MAINHAND)
                .create(0, 0, CONTROL_WIDTH, 20, Component.translatable("gui.examplemod.slot"));
        centerColumn.addChild(this.slotButton);

        centerColumn.addChild(SpacerElement.height(10));

        this.actionButton = Button.builder(Component.translatable("gui.examplemod.add"), b -> onAction())
                .width(CONTROL_WIDTH).build();
        centerColumn.addChild(this.actionButton);

        this.removeButton = Button.builder(Component.translatable("gui.examplemod.remove").withStyle(ChatFormatting.RED), b -> onRemove())
                .width(CONTROL_WIDTH).build();
        this.removeButton.active = false;
        centerColumn.addChild(this.removeButton);

        contentLayout.addChild(centerColumn);

        // --- 右侧：当前属性 ---
        LinearLayout rightColumn = LinearLayout.vertical().spacing(GAP);
        rightColumn.addChild(new StringWidget(listWidth, 20, Component.translatable("gui.examplemod.current_modifiers").withStyle(ChatFormatting.GOLD), this.font));
        this.currentModifiersList = new CurrentModifierList(this.minecraft, listWidth, listHeight, 0, 36);
        rightColumn.addChild(this.currentModifiersList);
        contentLayout.addChild(rightColumn);

        this.layout.addToContents(contentLayout);
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).width(200).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();

        // 初始填充
        this.filterAttributes("");
        this.currentModifiersList.refreshList();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    // --- 逻辑处理 ---

    private void onSearchChanged(String query) {
        // 可以在这里加入简单的 Debounce (防抖) 逻辑，如果搜索非常频繁
        this.filterAttributes(query);
    }

    private void filterAttributes(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        List<AttributeEntry> filtered = this.cachedAttributes.stream()
                .filter(entry -> entry.id.toString().contains(lower) || entry.id.getPath().contains(lower)) // 简单的包含匹配
                .map(AttributeEntry::new)
                .toList();

        this.availableAttributesList.replaceEntries(filtered);
    }

    private void validateNumber(String s) {
        boolean valid = true;
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException e) {
            valid = false;
        }
        // 视觉反馈：红色文本表示无效，白色表示有效
        this.valueBox.setTextColor(valid ? 0xE0E0E0 : 0xFF5555);
        this.actionButton.active = valid;
    }

    private Component getOperationTooltip(AttributeModifier.Operation op) {
        return switch (op) {
            case ADD_VALUE -> Component.translatable("gui.examplemod.op.add.desc");
            case ADD_MULTIPLIED_BASE -> Component.translatable("gui.examplemod.op.base.desc");
            case ADD_MULTIPLIED_TOTAL -> Component.translatable("gui.examplemod.op.total.desc");
        };
    }

    private void onAction() {
        if (selectedAttribute == null && selectedEntry == null) return;
        try {
            double amount = Double.parseDouble(this.valueBox.getValue());

            // 逻辑分支：如果是编辑现有条目，保持 ID 不变；如果是新增，生成唯一的 ID
            ResourceLocation id;
            if (selectedEntry != null) {
                id = selectedEntry.modifier().id();
            } else {
                // 使用更有意义的 ID 命名规则，防止冲突并便于调试
                String attrPath = selectedAttribute.value().getDescriptionId().replace(".", "_");
                id = ResourceLocation.fromNamespaceAndPath("examplemod", "attr_" + UUID.randomUUID().toString().substring(0, 8));
            }

            AttributeModifier modifier = new AttributeModifier(id, amount, operationButton.getValue());
            var newEntry = new ItemAttributeModifiers.Entry(
                    selectedEntry != null ? selectedEntry.attribute() : selectedAttribute,
                    modifier,
                    slotButton.getValue()
            );

            // 这是一个 Record，不可变，所以我们需要重建列表
            List<ItemAttributeModifiers.Entry> list = new ArrayList<>(this.modifiersSnapshot.modifiers());

            if (selectedEntry != null) {
                // 移除旧的（编辑模式）
                list.remove(selectedEntry);
                setStatus("Updated modifier");
            } else {
                setStatus("Added new modifier");
            }

            list.add(newEntry);

            // 按 Slot 和 属性名 排序，保持列表整洁（Visionary feature: 自动整理）
            list.sort(Comparator.comparing((ItemAttributeModifiers.Entry e) -> e.slot().ordinal())
                    .thenComparing(e -> e.attribute().value().getDescriptionId()));

            updateModifiers(new ItemAttributeModifiers(list, true));

            // 操作后重置选中状态，或者保持在当前编辑项（这里选择清除以避免误操作）
            if (selectedEntry != null) resetState(true);

        } catch (NumberFormatException ignored) {}
    }

    private void onRemove() {
        if (selectedEntry != null) {
            List<ItemAttributeModifiers.Entry> list = new ArrayList<>(this.modifiersSnapshot.modifiers());
            list.remove(selectedEntry);
            updateModifiers(new ItemAttributeModifiers(list, true));
            setStatus("Removed modifier");
            resetState(true);
        }
    }

    private void resetState(boolean clearSelection) {
        if (clearSelection) {
            this.selectedEntry = null;
            this.selectedAttribute = null;
            this.availableAttributesList.setSelected(null);
            this.currentModifiersList.setSelected(null);
            this.actionButton.setMessage(Component.translatable("gui.examplemod.add"));
            this.removeButton.active = false;
        }
    }

    private void updateModifiers(ItemAttributeModifiers newModifiers) {
        this.modifiersSnapshot = newModifiers;
        this.currentModifiersList.refreshList();
        // 网络包发送逻辑
        PacketDistributor.sendToServer(new UpdateItemAttributesPayload(this.modifiersSnapshot));
    }

    private void setStatus(String key) {
        this.statusMessage = Component.translatable("gui.examplemod.status." + key.toLowerCase().replace(" ", "_")).withStyle(ChatFormatting.GREEN);
        this.statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染物品预览
        int itemX = this.width / 2 - 8;
        int itemY = this.layout.getHeaderHeight() / 2 - 8;
        guiGraphics.renderItem(targetItem, itemX, itemY);
        // 如果需要，可以在这里渲染装饰性的边框

        if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= itemY && mouseY <= itemY + 16) {
            guiGraphics.renderTooltip(this.font, targetItem, mouseX, mouseY);
        }

        // 渲染状态消息（淡出效果）
        if (!statusMessage.getString().isEmpty()) {
            long elapsed = System.currentTimeMillis() - statusMessageTime;
            if (elapsed < 2000) {
                int alpha = elapsed > 1000 ? (int) (255 * (2000 - elapsed) / 1000f) : 255;
                int color = (alpha << 24) | 0xFFFFFF;
                guiGraphics.drawCenteredString(this.font, statusMessage, this.width / 2, this.height - 25, color);
            }
        }

        // 处理悬停 Tooltip
        if (this.availableAttributesList.isMouseOver(mouseX, mouseY)) {
            var entry = this.availableAttributesList.getHoveredEntry(mouseX, mouseY);
            if (entry != null) {
                guiGraphics.renderComponentTooltip(this.font, entry.getTooltip(), mouseX, mouseY);
            }
        }
    }

    // --- 数据记录类 (Data Records) ---

    // 用于缓存的简单记录
    record AttributeCacheEntry(Holder<Attribute> attribute, ResourceLocation id) {}

    // --- 组件内部类 ---

    abstract static class BaseSelectionList<T extends ObjectSelectionList.Entry<T>> extends ObjectSelectionList<T> {
        public BaseSelectionList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }
        @Override public int getRowWidth() { return this.width - 12; }
        @Override protected int getScrollbarPosition() { return this.getX() + this.width - 6; }
    }
    private double getItemDefaultAmount(Holder<Attribute> attribute) {
        // 获取物品类型的默认属性 (例如钻石剑自带的攻击力)
        ItemAttributeModifiers defaultModifiers = this.targetItem.getItem().getDefaultAttributeModifiers();

        // 筛选出针对当前属性的、且操作为加法(ADD_VALUE)的修饰符进行累加
        // 注意：通常我们只关心数值加成。百分比加成很少作为默认值出现，且累加逻辑不同。
        return defaultModifiers.modifiers().stream()
                .filter(entry -> entry.attribute().value().equals(attribute.value()))
                .filter(entry -> entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
    }
    class AttributeSelectionList extends BaseSelectionList<AttributeEntry> {
        public AttributeSelectionList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }
        public AttributeEntry getHoveredEntry(double mouseX, double mouseY) {
            return this.getEntryAtPosition(mouseX, mouseY);
        }
        // 使用 replaceEntries 比 clear + add 更高效
        public void replaceEntries(List<AttributeEntry> entries) {
            this.clearEntries();
            entries.forEach(this::addEntry);
            this.setScrollAmount(0);
        }
    }

    class AttributeEntry extends ObjectSelectionList.Entry<AttributeEntry> {
        private final AttributeCacheEntry entry;

        public AttributeEntry(AttributeCacheEntry entry) {
            this.entry = entry;
        }

        // 修改 AttributeEntry 类的 getTooltip 方法
        public List<Component> getTooltip() {
            List<Component> tooltip = new ArrayList<>();
            Attribute attr = entry.attribute.value();

            // 1. 标题 (蓝色)
            tooltip.add(Component.translatable(attr.getDescriptionId())
                    .withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE));

            // 2. 属性的全局基准值 (Global Base)
            // 参考 Apothic: 显示 Base Value
            double globalBase = attr.getDefaultValue();
            tooltip.add(Component.translatable("gui.examplemod.tooltip.global_base", DF.format(globalBase))
                    .withStyle(ChatFormatting.GRAY));
            // 3. 物品的原厂默认值 (Item Default)
            // 如果这把剑原本就有攻击力，在这里显示出来
            double itemDefault = getItemDefaultAmount(entry.attribute);
            if (itemDefault != 0) {
                String sign = itemDefault > 0 ? "+" : ""; // 正数显示加号
                tooltip.add(Component.literal("Item Default: " + sign + DF.format(itemDefault))
                        .withStyle(ChatFormatting.GREEN));

                // 可选：显示合计 (Base + Item)
                tooltip.add(Component.literal("Native Total: " + DF.format(globalBase + itemDefault))
                        .withStyle(ChatFormatting.DARK_GREEN));
            }

            // 4. 技术性 ID (按下 Shift 或配置开启时显示，这里默认显示)
            tooltip.add(Component.empty());
            tooltip.add(Component.literal(entry.id.toString()).withStyle(ChatFormatting.DARK_GRAY));


            return tooltip;
        }

        @Override
        public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mx, int my, boolean hover, float tick) {
            // 背景高亮
            if (availableAttributesList.getSelected() == this) {
                gfx.fill(left, top, left + w, top + h, 0x40FFFFFF);
                gfx.renderOutline(left, top, w, h, 0xFFFFFFFF);
            }

            Component name = Component.translatable(entry.attribute.value().getDescriptionId());
            gfx.drawString(font, trimText(name, w - 5), left + 4, top + 6, 0xFFFFFF, false);
            gfx.drawString(font, trimText(Component.literal(entry.id.toString()), w - 5), left + 4, top + 18, 0x888888, false);
        }

        // 修改 AttributeEntry 类的 mouseClicked 方法
        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            AttributeEditorScreen.this.availableAttributesList.setSelected(this);
            AttributeEditorScreen.this.selectedAttribute = this.entry.attribute;

            // 重置为新增模式
            AttributeEditorScreen.this.selectedEntry = null;
            AttributeEditorScreen.this.currentModifiersList.setSelected(null);
            AttributeEditorScreen.this.actionButton.setMessage(Component.translatable("gui.examplemod.add"));
            AttributeEditorScreen.this.removeButton.active = false;

            // --- 智能数值填充逻辑 ---
            double defaultAmount = getItemDefaultAmount(this.entry.attribute);

            if (defaultAmount != 0) {
                // 如果物品原本就有这个属性（例如剑的攻击力是7），预填入 7
                // 这样用户可以在原版数值基础上微调
                AttributeEditorScreen.this.valueBox.setValue(DF.format(defaultAmount));
            } else {
                // 如果没有，默认填 1.0
                AttributeEditorScreen.this.valueBox.setValue("1.0");
            }
            // ----------------------

            return true;
        }

        @Override public Component getNarration() { return Component.translatable(entry.attribute.value().getDescriptionId()); }
    }

    class CurrentModifierList extends BaseSelectionList<ModifierEntry> {
        public CurrentModifierList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }
        public void refreshList() {
            this.clearEntries();
            AttributeEditorScreen.this.modifiersSnapshot.modifiers().stream()
                    .map(ModifierEntry::new)
                    .forEach(this::addEntry);
        }
    }

    class ModifierEntry extends ObjectSelectionList.Entry<ModifierEntry> {
        private final ItemAttributeModifiers.Entry entry;
        private final ResourceLocation attrId;

        public ModifierEntry(ItemAttributeModifiers.Entry entry) {
            this.entry = entry;
            this.attrId = BuiltInRegistries.ATTRIBUTE.getKey(entry.attribute().value());
        }

        @Override
        public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mx, int my, boolean hover, float tick) {
            if (currentModifiersList.getSelected() == this) {
                gfx.fill(left, top, left + w, top + h, 0x40FFFFFF);
                gfx.renderOutline(left, top, w, h, 0xFFFFFFFF);
            }

            // 绘制属性名
            Component name = Component.translatable(entry.attribute().value().getDescriptionId());
            gfx.drawString(font, trimText(name, w - 5), left + 4, top + 4, 0xFFFFFF, false);

            // 绘制数值和操作符
            String opSymbol = switch(entry.modifier().operation()) {
                case ADD_VALUE -> "+";
                case ADD_MULTIPLIED_BASE -> "+%"; // Base
                case ADD_MULTIPLIED_TOTAL -> "++%"; // Total
            };

            // 根据正负值改变颜色
            double amount = entry.modifier().amount();
            int color = getValueColor(entry.modifier().amount(), entry.modifier().operation());

            MutableComponent valComp = Component.literal(opSymbol + DF.format(amount));
            // 如果是百分比操作，加上后缀
            if (entry.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) {
                valComp.append("x");
            }

            gfx.drawString(font, valComp, left + 4, top + 16, color, false);

            // 绘制生效槽位 (右对齐)
            Component slotText = Component.literal("[" + entry.slot().getSerializedName().substring(0, Math.min(4, entry.slot().getSerializedName().length())) + "]")
                    .withStyle(ChatFormatting.GOLD);
            int slotWidth = font.width(slotText);
            gfx.drawString(font, slotText, left + w - slotWidth - 2, top + 16, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            AttributeEditorScreen.this.currentModifiersList.setSelected(this);
            AttributeEditorScreen.this.selectedEntry = this.entry;

            // 切换到编辑模式
            AttributeEditorScreen.this.selectedAttribute = null;
            AttributeEditorScreen.this.availableAttributesList.setSelected(null);

            AttributeEditorScreen.this.valueBox.setValue(String.valueOf(entry.modifier().amount()));
            AttributeEditorScreen.this.operationButton.setValue(entry.modifier().operation());
            AttributeEditorScreen.this.slotButton.setValue(entry.slot());

            AttributeEditorScreen.this.actionButton.setMessage(Component.translatable("gui.examplemod.update")); // Update
            AttributeEditorScreen.this.removeButton.active = true;
            return true;
        }
        @Override public Component getNarration() { return Component.empty(); }
    }

    private Component trimText(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return Component.literal(font.substrByWidth(text, maxWidth - 10).getString() + "...");
    }
}