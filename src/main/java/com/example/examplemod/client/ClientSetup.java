package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.entity.GoldenGateRenderer;
import com.example.examplemod.client.entity.SwordProjectileRenderer;
import com.example.examplemod.client.gui.OffsetConfigScreen;

import com.example.examplemod.component.ModDataComponents;
import com.example.examplemod.init.ModEffects;
import com.example.examplemod.register.ModEntities;
import com.example.examplemod.util.TargetDetector;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.client.AttributeModifierComponent;
import dev.shadowsoffire.apothic_attributes.client.ModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSourceType;
import dev.shadowsoffire.placebo.PlaceboClient;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.BooleanAttribute;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Vector2ic;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;

import static com.example.examplemod.init.ModEffects.MAKEN_POWER;
import static com.example.examplemod.register.ModEntities.GOLDENGATEENTITY;
import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
import static net.minecraft.world.item.Items.LIGHTNING_ROD;
import static net.minecraft.world.item.Items.TNT;

@EventBusSubscriber(modid = ExampleMod.MODID,value = Dist.CLIENT)
public class ClientSetup {
    public static final KeyMapping INSPECT_KEY = new KeyMapping(
            "key.yourmod.inspect", // 翻译键，用于在控制菜单中显示
            InputConstants.Type.KEYSYM, // 按键类型
            GLFW.GLFW_KEY_I, // 默认按键 (这里是 'I' 键)
            "key.categories.yourmod" // 按键分类的翻译键
    );
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.examplemod.open_render_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // 默认按 K 键
            "key.categories.misc"
    );
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (true) {
            while (OPEN_CONFIG_KEY.consumeClick()) {
                // 打开我们刚才写的 GUI
                Minecraft.getInstance().setScreen(new OffsetConfigScreen());

            }
        }
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册你的传送门实体渲染器
        event.registerEntityRenderer(GOLDENGATEENTITY.get(), GoldenGateRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_PROJECTILE.get(), SwordProjectileRenderer::new);
        // 如果你的法术还会召唤其他实体（如 UnstableWeaponEntity），也必须在这里注册！
        // 例如:
        // event.registerEntityRenderer(EntityRegistry.UNSTABLE_SWORD.get(), UnstableWeaponRenderer::new);
    }
    @SubscribeEvent
    public static void regkey(RegisterKeyMappingsEvent event)
    {
        event.register(INSPECT_KEY);
    }

    @EventBusSubscriber(modid = ExampleMod.MODID,value = Dist.CLIENT)
    public static class ChestScreenOverlay {
        @SubscribeEvent
        public static void onRenderGuiForeground(ContainerScreenEvent.Render.Foreground event) {
            var guiGraphics = event.getGuiGraphics();
            var screen = event.getContainerScreen();

            for (Slot slot : screen.getMenu().slots) {
                if (slot.hasItem() && slot.getItem().has(Apoth.Components.RARITY)) {
                    int argbColor = slot.getItem().get(Apoth.Components.RARITY).get().color().getValue()| 0xFF000000;
                    int x = slot.x;
                    int y = slot.y;
                    int size = 2;
                    guiGraphics.fill(x, y, x + size, y + size, 0, argbColor);
                }
                if (slot.hasItem() && slot.getItem().has(Apoth.Components.PURITY)) {
                    int argbColor = slot.getItem().get(Apoth.Components.PURITY).getColor().getValue();
                    int x = slot.x;
                    int y = slot.y;
                    int size = 2;
                    guiGraphics.fill(x, y, x + size, y + size, 0, argbColor);
                }
            }
        }
        @SubscribeEvent
        public static void onComputeFov(ComputeFovModifierEvent event) {
            // 确保是客户端玩家
            if (!(event.getPlayer() instanceof AbstractClientPlayer)) {
                return;
            }
            event.setNewFovModifier(1f);
        }

    }

    public static class TargetAttributesScreen extends Screen {

        public static final ResourceLocation TEXTURES = ApothicAttributes.loc("textures/gui/attributes_gui.png");
        public static final int ENTRY_HEIGHT = 22;
        public static final int MAX_ENTRIES = 6;
        public static final int WIDTH = 131;
        public static final int HEIGHT = 166;
        private final Map<AttributeModifier.Operation, Map<ResourceLocation, AttributeModifier>> modifiersByOperation = Maps.newEnumMap(
                AttributeModifier.Operation.class
        );
        // 这些静态字段可以在不同的目标实体GUI之间保持状态（滚动位置和过滤器设置）
        protected static float scrollOffset = 0;
        protected static boolean hideUnchanged = false;

        protected final LivingEntity targetEntity;
        protected final Font font;
        protected HideUnchangedButton hideUnchangedBtn;

        protected int leftPos, topPos;
        protected boolean scrolling;
        protected int startIndex;
        protected List<AttributeInstance> data = new ArrayList<>();
        protected long lastRenderTick = -1;

        public TargetAttributesScreen(LivingEntity target) {
            // 使用实体的名字作为标题，或者提供一个自定义的Component
            super(target.getDisplayName());
            this.targetEntity = target;
            this.minecraft = Minecraft.getInstance();
            this.font = this.minecraft.font;
            this.refreshData();
        }

        @Override
        protected void init() {
            super.init();
            // 居中显示GUI
            this.leftPos = (this.width - WIDTH) / 2;
            this.topPos = (this.height - HEIGHT) / 2;

            this.hideUnchangedBtn = new HideUnchangedButton(this.leftPos + 7, this.topPos + 151);
            this.addRenderableWidget(this.hideUnchangedBtn);
        }

        @SuppressWarnings("deprecation")
        public void refreshData() {
            this.data.clear();
            BuiltInRegistries.ATTRIBUTE.holders()
                    .map(this.targetEntity::getAttribute) // 从目标实体获取属性
                    .filter(Objects::nonNull)
                    .filter(ai -> !ALConfig.hiddenAttributes.contains(ai.getAttribute().unwrapKey().get().location()))
                    .filter(ai -> !hideUnchanged || (ai.getBaseValue() != ai.getValue()))
                    .forEach(this.data::add);
            this.data.sort(this::compareAttrs);
            this.startIndex = (int) (scrollOffset * this.getOffScreenRows() + 0.5D);
        }

        protected int compareAttrs(AttributeInstance a1, AttributeInstance a2) {
            String name = I18n.get(a1.getAttribute().value().getDescriptionId());
            String name2 = I18n.get(a2.getAttribute().value().getDescriptionId());
            return name.compareTo(name2);
        }

        @Override
        public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
            super.render(gfx, mouseX, mouseY, partialTicks); // 渲染背景

            if (this.lastRenderTick != PlaceboClient.ticks) {
                this.lastRenderTick = PlaceboClient.ticks;
                this.refreshData();
            }

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            int left = this.leftPos;
            int top = this.topPos;

            gfx.blit(TEXTURES, left, top, 0, 0, WIDTH, HEIGHT);
            int scrollbarPos = (int) (117 * scrollOffset);
            gfx.blit(TEXTURES, left + 111, top + 16 + scrollbarPos, 244, this.isScrollBarActive() ? 0 : 15, 12, 15);

            int idx = this.startIndex;
            while (idx < this.startIndex + MAX_ENTRIES && idx < this.data.size()) {
                this.renderEntry(gfx, this.data.get(idx), this.leftPos + 8, this.topPos + 16 + ENTRY_HEIGHT * (idx - this.startIndex), mouseX, mouseY);
                idx++;
            }

            // 渲染按钮（由Screen的renderables处理）和标签
            gfx.drawString(font, Component.translatable("apothic_attributes.gui.attributes"), this.leftPos + 8, this.topPos + 5, 0x404040, false);
            gfx.drawString(font, ApothicAttributes.lang("text", "hide_unchanged"), this.leftPos + 20, this.topPos + 152, 0x404040, false);

            // 在所有内容之上渲染工具提示
            this.renderTooltip(gfx, mouseX, mouseY);
        }

        @SuppressWarnings("deprecation")
        protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
            AttributeInstance inst = this.getHoveredSlot(mouseX, mouseY);
            if (inst != null) {
                Attribute attr = inst.getAttribute().value();
                boolean isDynamic = inst.getAttribute().is(ALObjects.Tags.DYNAMIC_BASE_ATTRIBUTES);

                List<Component> list = new ArrayList<>();
                MutableComponent name = Component.translatable(attr.getDescriptionId()).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true));

                if (isDynamic) {
                    name.append(CommonComponents.SPACE);
                    name.append(Component.translatable("apothic_attributes.gui.dynamic").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false)));
                }

                if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
                    Style style = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false);
                    name.append(Component.literal(" [" + BuiltInRegistries.ATTRIBUTE.getKey(attr) + "]").withStyle(style));
                }

                list.add(name);

                String key = attr.getDescriptionId() + ".desc";

                if (I18n.exists(key)) {
                    Component txt = Component.translatable(key).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC);
                    list.add(txt);
                }
                else if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
                    Component txt = Component.literal(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
                    list.add(txt);
                }

                int color = getValueColor(inst, ChatFormatting.GRAY.getColor());

                Component valueComp = attr.toValueComponent(null, inst.getValue(), ApothicAttributes.getTooltipFlag()).withColor(color);
                Component baseComp = attr.toValueComponent(null, inst.getBaseValue(), ApothicAttributes.getTooltipFlag()).withStyle(ChatFormatting.GRAY);

                if (!isDynamic) {
                    list.add(CommonComponents.EMPTY);
                    list.add(Component.translatable("apothic_attributes.gui.current", valueComp).withStyle(ChatFormatting.GRAY));

                    Component base = Component.translatable("apothic_attributes.gui.base", baseComp).withStyle(ChatFormatting.GRAY);

                    if (attr instanceof RangedAttribute ra) {
                        Component min = attr.toValueComponent(null, ra.getMinValue(), ApothicAttributes.getTooltipFlag());
                        min = Component.translatable("apothic_attributes.gui.min", min);
                        Component max = attr.toValueComponent(null, ra.getMaxValue(), ApothicAttributes.getTooltipFlag());
                        max = Component.translatable("apothic_attributes.gui.max", max);
                        list.add(Component.translatable("%s \u2507 %s \u2507 %s", base, min, max).withStyle(ChatFormatting.GRAY));
                    }
                    else {
                        list.add(base);
                    }
                }

                List<ClientTooltipComponent> finalTooltip = new ArrayList<>(list.size());
                for (Component txt : list) {
                    this.addComp(txt, finalTooltip);
                }

                if (inst.getModifiers().stream().anyMatch(modif -> modif.amount() != 0)) {
                    this.addComp(CommonComponents.EMPTY, finalTooltip);
                    this.addComp(Component.translatable("apothic_attributes.gui.modifiers").withStyle(ChatFormatting.GOLD), finalTooltip);

                    Map<ResourceLocation, ModifierSource<?>> modifiersToSources = new HashMap<>();

                    for (ModifierSourceType<?> type : ModifierSourceType.getTypes()) {
                        type.extract(this.targetEntity, (modif, source) -> modifiersToSources.put(modif.id(), source)); // 使用目标实体
                    }

                    MutableComponent[] opValues = new MutableComponent[3];
                    double[] numericValues = new double[3];

                    for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
                        double baseValue = op == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? 1 : 0;
                        List<AttributeModifier> modifiers = new ArrayList<>(this.modifiersByOperation.computeIfAbsent(op, operation-> new Object2ObjectOpenHashMap<>()).values());
                        double opValue = modifiers.stream().mapToDouble(AttributeModifier::amount).reduce(baseValue, (res, elem) -> op == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? res * (1 + elem) : res + elem);

                        modifiers.sort(ModifierSourceType.compareBySource(modifiersToSources));
                        for (AttributeModifier modif : modifiers) {
                            if (modif.amount() != 0) {
                                Component comp = attr.toComponent(modif, ApothicAttributes.getTooltipFlag());
                                var src = modifiersToSources.get(modif.id());
                                finalTooltip.add(new AttributeModifierComponent(src, comp, this.font, this.leftPos - 16));
                            }
                        }

                        color = getValueColor(attr, opValue, baseValue, ChatFormatting.GRAY.getColor());
                        Component valueComp2 = attr.toValueComponent(op, opValue, ApothicAttributes.getTooltipFlag()).withStyle(Style.EMPTY.withColor(color));
                        MutableComponent comp = Component.translatable("apothic_attributes.gui." + op.name().toLowerCase(Locale.ROOT), valueComp2).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

                        opValues[op.ordinal()] = comp;
                        numericValues[op.ordinal()] = opValue;
                    }

                    this.addComp(CommonComponents.EMPTY, finalTooltip);
                    this.addComp(Component.translatable("apothic_attributes.gui.formula").withStyle(ChatFormatting.GOLD), finalTooltip);

                    Component base = isDynamic ? Component.translatable("apothic_attributes.gui.formula.base") : baseComp;
                    Component value = isDynamic ? Component.translatable("apothic_attributes.gui.formula.value") : valueComp;

                    Component formula = buildFormula(base, value, numericValues, attr);
                    this.addComp(formula, finalTooltip);
                }
                else if (isDynamic) {
                    this.addComp(CommonComponents.EMPTY, finalTooltip);
                    this.addComp(Component.translatable("apothic_attributes.gui.no_modifiers").withStyle(ChatFormatting.GOLD), finalTooltip);
                }

                int tooltipX = this.leftPos - 16 - finalTooltip.stream().map(c -> c.getWidth(this.font)).max(Integer::compare).get();
                if (!finalTooltip.isEmpty()) {
                    net.neoforged.neoforge.client.event.RenderTooltipEvent.Pre preEvent = net.neoforged.neoforge.client.ClientHooks.onRenderTooltipPre(ItemStack.EMPTY, gfx, mouseX, mouseY, gfx.guiWidth(), gfx.guiHeight(), finalTooltip, font, DefaultTooltipPositioner.INSTANCE);
                    if (preEvent.isCanceled()) return;
                    int i = 0;
                    int j = finalTooltip.size() == 1 ? -2 : 0;

                    for (ClientTooltipComponent clienttooltipcomponent : finalTooltip) {
                        int k = clienttooltipcomponent.getWidth(preEvent.getFont());
                        if (k > i) {
                            i = k;
                        }

                        j += clienttooltipcomponent.getHeight();
                    }

                    int i2 = i;
                    int j2 = j;
                    Vector2ic vector2ic = DefaultTooltipPositioner.INSTANCE.positionTooltip(gfx.guiWidth(), gfx.guiHeight(), preEvent.getX(), preEvent.getY(), i2, j2);
                    int l = vector2ic.x();
                    int i1 = vector2ic.y();
                    gfx.pose().pushPose();
                    int j1 = 400;
                    net.neoforged.neoforge.client.event.RenderTooltipEvent.Color colorEvent = net.neoforged.neoforge.client.ClientHooks.onRenderTooltipColor(ItemStack.EMPTY, gfx, l, i1, preEvent.getFont(), finalTooltip);
                    gfx.drawManaged(() -> TooltipRenderUtil.renderTooltipBackground(gfx, l, i1, i2, j2, 400, colorEvent.getBackgroundStart(), colorEvent.getBackgroundEnd(), colorEvent.getBorderStart(), colorEvent.getBorderEnd()));
                    gfx.pose().translate(0.0F, 0.0F, 400.0F);
                    int k1 = i1;

                    for (int l1 = 0; l1 < finalTooltip.size(); l1++) {
                        ClientTooltipComponent clienttooltipcomponent1 = finalTooltip.get(l1);
                        clienttooltipcomponent1.renderText(preEvent.getFont(), l, k1, gfx.pose().last().pose(), gfx.bufferSource());
                        k1 += clienttooltipcomponent1.getHeight() + (l1 == 0 ? 2 : 0);
                    }

                    k1 = i1;

                    for (int k2 = 0; k2 < finalTooltip.size(); k2++) {
                        ClientTooltipComponent clienttooltipcomponent2 = finalTooltip.get(k2);
                        clienttooltipcomponent2.renderImage(preEvent.getFont(), l, k1, gfx);
                        k1 += clienttooltipcomponent2.getHeight() + (k2 == 0 ? 2 : 0);
                    }

                    gfx.pose().popPose();
                }
            }
        }


        // 以下方法几乎是直接从 AttributesGui 复制的，无需修改或只需微小修改

        private void addComp(Component comp, List<ClientTooltipComponent> finalTooltip) {
            if (comp == CommonComponents.EMPTY) {
                finalTooltip.add(ClientTooltipComponent.create(comp.getVisualOrderText()));
            }
            else {
                for (FormattedText fTxt : this.font.getSplitter().splitLines(comp, this.leftPos - 16, comp.getStyle())) {
                    finalTooltip.add(ClientTooltipComponent.create(Language.getInstance().getVisualOrder(fTxt)));
                }
            }
        }

        private void renderEntry(GuiGraphics gfx, AttributeInstance inst, int x, int y, int mouseX, int mouseY) {
            boolean hover = this.getHoveredSlot(mouseX, mouseY) == inst;
            gfx.blit(TEXTURES, x, y, 142, hover ? ENTRY_HEIGHT : 0, 100, ENTRY_HEIGHT);

            Component txt = Component.translatable(inst.getAttribute().value().getDescriptionId());
            int splitWidth = 60;
            List<FormattedCharSequence> lines = this.font.split(txt, splitWidth);
            while (lines.size() > 2) {
                splitWidth += 10;
                lines = this.font.split(txt, splitWidth);
            }

            PoseStack stack = gfx.pose();
            stack.pushPose();
            float scale = 1;
            int maxWidth = lines.stream().map(this.font::width).max(Integer::compareTo).get();
            if (maxWidth > 66) {
                scale = 66F / maxWidth;
                stack.scale(scale, scale, 1);
            }

            for (int i = 0; i < lines.size(); i++) {
                var line = lines.get(i);
                float width = this.font.width(line) * scale;
                float lineX = (x + 1 + (68 - width) / 2) / scale;
                float lineY = (y + (lines.size() == 1 ? 7 : 2) + i * 10) / scale;
                gfx.drawString(font, line, lineX, lineY, 0x404040, false);
            }
            stack.popPose();
            stack.pushPose();

            MutableComponent value = inst.getAttribute().value().toValueComponent(null, inst.getValue(), TooltipFlag.Default.NORMAL);

            if (inst.getAttribute().is(ALObjects.Tags.DYNAMIC_BASE_ATTRIBUTES)) {
                value = Component.literal("\uFFFD");
            }

            scale = 1;
            if (this.font.width(value) > 27) {
                scale = 27F / this.font.width(value);
                stack.scale(scale, scale, 1);
            }

            int color = getValueColor(inst, ChatFormatting.WHITE.getColor());
            gfx.drawString(font, value, (int) ((x + 72 + (27 - this.font.width(value) * scale) / 2) / scale), (int) ((y + 7) / scale), color, true);
            stack.popPose();
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (this.isScrollBarActive()) {
                this.scrolling = false;
                int left = this.leftPos + 111;
                int top = this.topPos + 15;
                if (pMouseX >= left && pMouseX < left + 12 && pMouseY >= top && pMouseY < top + 155) {
                    this.scrolling = true;
                    int i = this.topPos + 15;
                    int j = i + 138;
                    scrollOffset = ((float) pMouseY - i - 7.5F) / (j - i - 15.0F);
                    scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
                    this.startIndex = (int) (scrollOffset * this.getOffScreenRows() + 0.5D);
                    return true;
                }
            }
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        @Override
        public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
            if (this.scrolling && this.isScrollBarActive()) {
                int i = this.topPos + 15;
                int j = i + 138;
                scrollOffset = ((float) pMouseY - i - 7.5F) / (j - i - 15.0F);
                scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
                this.startIndex = (int) (scrollOffset * this.getOffScreenRows() + 0.5D);
                return true;
            }
            return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        }

        @Override
        public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
            if (this.isScrollBarActive()) {
                int i = this.getOffScreenRows();
                scrollOffset = (float) (scrollOffset - pScrollY / i);
                scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
                this.startIndex = (int) (scrollOffset * i + 0.5D);
                return true;
            }
            return super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
        }

        private boolean isScrollBarActive() {
            return this.data.size() > MAX_ENTRIES;
        }

        protected int getOffScreenRows() {
            return Math.max(0, this.data.size() - MAX_ENTRIES);
        }

        @Nullable
        public AttributeInstance getHoveredSlot(int mouseX, int mouseY) {
            for (int i = 0; i < MAX_ENTRIES; i++) {
                if (this.startIndex + i < this.data.size()) {
                    if (this.isHovering(8, 14 + ENTRY_HEIGHT * i, 100, ENTRY_HEIGHT, mouseX, mouseY)) return this.data.get(this.startIndex + i);
                }
            }
            return null;
        }

        protected boolean isHovering(int pX, int pY, int pWidth, int pHeight, double pMouseX, double pMouseY) {
            int i = this.leftPos;
            int j = this.topPos;
            pMouseX -= i;
            pMouseY -= j;
            return pMouseX >= pX - 1 && pMouseX < pX + pWidth + 1 && pMouseY >= pY - 1 && pMouseY < pY + pHeight + 1;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        private static DecimalFormat f = IAttributeExtension.FORMAT;

        public static String format(int n) {
            int log = (int) StrictMath.log10(n);
            if (log <= 4) return String.valueOf(n);
            if (log == 5) return f.format(n / 1000D) + "K";
            if (log <= 8) return f.format(n / 1000000D) + "M";
            else return f.format(n / 1000000000D) + "B";
        }

        public static int getValueColor(AttributeInstance inst, int fallbackColor) {
            return getValueColor(inst.getAttribute().value(), inst.getValue(), inst.getBaseValue(), fallbackColor);
        }

        public static int getValueColor(Attribute attr, double value, double base, int fallbackColor) {
            if (value == base) {
                return fallbackColor;
            }

            if (attr instanceof RangedAttribute) {
                boolean isPositive = value > base;
                return translateColor(attr.getStyle(isPositive));
            }
            else if (attr instanceof BooleanAttribute) {
                boolean isPositive = value > 0;
                return translateColor(attr.getStyle(isPositive));
            }

            return fallbackColor;
        }

        private static int translateColor(ChatFormatting color) {
            return switch (color) {
                case BLUE -> 0x55DD55;
                case RED -> 0xFF6060;
                case GRAY -> 0xFFFFFF;
                default -> color.getColor();
            };
        }

        public static Component buildFormula(Component base, Component value, double[] numericValues, Attribute attr) {
            double add = numericValues[0];
            double mulBase = numericValues[1];
            double mulTotal = numericValues[2];

            boolean isAddNeg = add < 0;
            boolean isMulNeg = mulBase < 0;

            String addSym = isAddNeg ? "-" : "+";
            add = Math.abs(add);

            String mulBaseSym = isMulNeg ? "-" : "+";
            mulBase = Math.abs(mulBase);

            String addStr = f.format(add);
            String mulBaseStr = f.format(mulBase);
            String mulTotalStr = f.format(mulTotal);

            String formula = "%2$s";

            if (add != 0) {
                ChatFormatting color = getColor(attr, isAddNeg);
                formula = formula + " " + colored(addSym + " " + addStr, color);
            }

            if (mulBase != 0) {
                String withParens = add == 0 ? formula : "(%s)".formatted(formula);
                ChatFormatting color = getColor(attr, isMulNeg);
                formula = withParens + " " + colored(mulBaseSym + " " + mulBaseStr + " * ", color) + withParens;
            }

            if (mulTotal != 1) {
                String withParens = add == 0 && mulBase == 0 ? formula : "(%s)".formatted(formula);
                ChatFormatting color = getColor(attr, mulTotal < 1);
                formula = colored(mulTotalStr + " * ", color) + withParens;
            }

            return Component.translatable("%1$s = " + formula, value, base).withStyle(ChatFormatting.GRAY);
        }

        private static ChatFormatting getColor(Attribute attr, boolean isNegative) {
            ChatFormatting color = attr.getStyle(!isNegative);
            return color == ChatFormatting.BLUE ? ChatFormatting.YELLOW : color;
        }

        private static String colored(String str, ChatFormatting color) {
            return "" + ChatFormatting.PREFIX_CODE + color.getChar() + str + ChatFormatting.PREFIX_CODE + ChatFormatting.RESET.getChar();
        }

        public class HideUnchangedButton extends AbstractButton {

            public HideUnchangedButton(int pX, int pY) {
                super(pX, pY, 10, 10, ApothicAttributes.lang("button", "hide_unchanged"));
                // 按钮默认可见
            }

            @Override
            public void onPress() {
                hideUnchanged = !hideUnchanged;
            }

            @Override
            public void renderWidget(GuiGraphics gfx, int pMouseX, int pMouseY, float pPartialTick) {
                int u = 131, v = 20;
                int vOffset = hideUnchanged ? 0 : 10;
                if (this.isHovered) {
                    vOffset += 20;
                }

                RenderSystem.enableDepthTest();
                gfx.blit(TEXTURES, this.getX(), this.getY(), u, v + vOffset, 10, 10, 256, 256);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
                this.defaultButtonNarrationText(pNarrationElementOutput);
            }
        }
    }

    public static class ConditionalScaledItemModel extends BakedModelWrapper<BakedModel> {

        private final Vector3f scale;

        public ConditionalScaledItemModel(BakedModel originalModel, Vector3f scale) {
            super(originalModel);
            this.scale = scale;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
            super.applyTransform(context, poseStack, applyLeftHandTransform);

            if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {

                Player player = Minecraft.getInstance().player;
                if (shouldScale(player, context)) {
                    poseStack.scale(this.scale.x(), this.scale.y(), this.scale.z());
                }
            }

            return this;
        }

        private boolean shouldScale(Player player, ItemDisplayContext context) {
            if (player == null) {
                return false;
            }

            boolean isMainHandContext = (player.getMainArm().name().equals("RIGHT") && context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                                     || (player.getMainArm().name().equals("LEFT") && context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);

            if (!isMainHandContext) {
                return false;
            }

            return player.hasEffect(MAKEN_POWER)
                && player.getMainHandItem().has(ModDataComponents.MAKEN_SWORD.get());
        }
    }

    @EventBusSubscriber(modid = ExampleMod.MODID,value = Dist.CLIENT)
    public static class EntityAccessoryRenderer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            event.getSkins().forEach(skinName -> {
                PlayerRenderer renderer = event.getSkin(skinName);
                if (renderer != null) {
                    renderer.addLayer(new EntityAccessoryRenderer<>(renderer));
                }
            });

    //        for (EntityType<?> entityType : event.getEntityTypes()) {
    //            var renderer = event.getRenderer(entityType);
    //            if (renderer instanceof LivingEntityRenderer livingRenderer) {
    //                // Apply the accessory layer to any entity model that exposes a head (HeadedModel)
    //                // but exclude Creeper, as it is handled separately for its specific TNT logic in the renderer itself.
    //                if (livingRenderer.getModel() instanceof HeadedModel && entityType != EntityType.CREEPER) {
    //                    livingRenderer.addLayer(new EntityAccessoryRenderer(livingRenderer));
    //                }
    //            }
    //        }
    //
    //        // --- 3. Add the Player-specific animated layer (Warden Arm) ---
    //        // The original code targeted the PLAYER renderer, not the CREEPER renderer.
    //        LivingEntityRenderer<Player, PlayerModel<Player>> playerRenderer = event.getRenderer(EntityType.PLAYER);
    //        if (playerRenderer != null) {
    //            System.out.println("Adding AnimatedHatRenderer to Player");
    //            playerRenderer.addLayer(new AnimatedHatRenderer(playerRenderer));
    //        }
        }
        public EntityAccessoryRenderer(RenderLayerParent<T, M> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
    //        // --- 1. Creeper TNT Logic ---
    //        if (this.renderCreeperAccessory(poseStack, buffer, packedLight, entity)) {
    //            return;
    //        }
    //
    //        // --- 2. Witch Lightning Rod Logic ---
    //        if (this.renderWitchAccessory(poseStack, buffer, packedLight, entity)) {
    //            return;
    //        }

            // --- 3. Curios Item Logic (Generic) ---
            this.renderCurioAccessory(poseStack, buffer, packedLight, entity);
        }

        private boolean renderCreeperAccessory(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity) {
            if (entity instanceof Creeper creeper) {
                ItemStack tnt = new ItemStack(TNT);
                poseStack.pushPose();

                // CreeperModel provides its parts directly
                CreeperModel<Creeper> model = (CreeperModel<Creeper>) this.getParentModel();
                model.root().getChild("head").translateAndRotate(poseStack);

                // Positioning adjustment
                poseStack.translate(0, -1D, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-180F));

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        tnt,
                        ItemDisplayContext.HEAD,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        buffer,
                        creeper.level(),
                        creeper.getId()
                );
                poseStack.popPose();
                return true;
            }
            return false;
        }

        private boolean renderWitchAccessory(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity) {
            if (entity instanceof Witch witch) {
                if (this.getParentModel() instanceof HeadedModel headedModel) {
                    ItemStack lightningRod = new ItemStack(LIGHTNING_ROD);
                    poseStack.pushPose();

                    headedModel.getHead().translateAndRotate(poseStack);
                    poseStack.translate(-0.49D, -1.8D, 0.05D);

                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            lightningRod,
                            ItemDisplayContext.HEAD,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            poseStack,
                            buffer,
                            witch.level(),
                            witch.getId()
                    );
                    poseStack.popPose();
                    return true;
                }
            }
            return false;
        }

        private void renderCurioAccessory(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity) {
            String slotIdentifier = "artifact_head";
            ItemStack curioToRender = getCurioItem(entity, slotIdentifier);

            if (!curioToRender.isEmpty() && curioToRender.getItem().getDescriptionId().contains("l2artifacts.damocles_head")) {

                if (this.getParentModel() instanceof HeadedModel headedModel) {
                    poseStack.pushPose();
                    float scale = 0.675F;

                    headedModel.getHead().translateAndRotate(poseStack);

                    poseStack.translate(-0.58125D * scale, -1.0D * scale, -0.58125D * scale);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
                    poseStack.scale(scale, scale, scale);

                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            curioToRender,
                            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            poseStack,
                            buffer,
                            entity.level(),
                            entity.getId()
                    );
                    poseStack.popPose();
                }
            }
        }


        private static ItemStack getCurioItem(Entity entity, String slotId) {
            if (ModList.get().isLoaded("curios") && entity instanceof LivingEntity livingEntity) {
                Optional<ICuriosItemHandler> curiosHandlerOptional = CuriosApi.getCuriosInventory(livingEntity);
                if (curiosHandlerOptional.isPresent()) {
                    ICuriosItemHandler curiosHandler = curiosHandlerOptional.get();
                    Optional<SlotResult> foundSlot = curiosHandler.findFirstCurio(item -> true, slotId);
                    return foundSlot.map(SlotResult::stack).orElse(ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }
    }

    @EventBusSubscriber(modid = ExampleMod.MODID,value = Dist.CLIENT)
    public static class KeyInputHandler {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            // 检查按键是否被按下
            if (INSPECT_KEY.consumeClick()) {
                LivingEntity target = TargetDetector.getTargetedEntity();

                // 如果成功获取到目标
                if (target != null) {
                    // 打开我们的新 GUI，并把目标实体传递进去
                    Minecraft.getInstance().setScreen(new TargetAttributesScreen(target));
                }
            }
        }

    }

    @EventBusSubscriber(modid = ExampleMod.MODID,value = Dist.CLIENT)
    public static class LayerAddHandler {

        @SubscribeEvent
        public static void addRenderLayer(EntityRenderersEvent.AddLayers event) {
            for (PlayerSkin.Model playerModel : event.getSkins()){
                EntityRenderer<? extends Player> renderer = event.getSkin(playerModel);
                if (renderer instanceof LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new FearEffectLayer(livingRenderer));
                }
            }


            for (EntityType<?> type : event.getEntityTypes()) {
                EntityRenderer<?> renderer = event.getRenderer(type);
                if (renderer instanceof LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new FearEffectLayer(livingRenderer));
                }
            }
        }

    }

    //@EventBusSubscriber(value = Dist.CLIENT)
    public static class Test {

        public static final VertexFormat vertexFormat=VertexFormat.builder()
                .add("Position", VertexFormatElement.POSITION)
                .add("Color", VertexFormatElement.COLOR)
                .add("Normal",VertexFormatElement.NORMAL)
                .add("UV2", VertexFormatElement.UV2)
                .build();
        public static RenderType custom = RenderType.create(
                "custom",
                vertexFormat,
                VertexFormat.Mode.TRIANGLES,
                4194304,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setTextureState(RenderStateShard.NO_TEXTURE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .createCompositeState(RenderType.OutlineProperty.NONE)
        );

        public static final float[][] CUBE = {
                // 0-5: 前面 (Front Face), 法线朝向 +Z
                {0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1},

                // 6-11: 后面 (Back Face), 法线朝向 -Z
                {0, 0, -1}, {0, 0, -1}, {0, 0, -1}, {0, 0, -1}, {0, 0, -1}, {0, 0, -1},

                // 12-17: 上面 (Top Face), 法线朝向 +Y
                {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0},

                // 18-23: 下面 (Bottom Face), 法线朝向 -Y
                {0, -1, 0}, {0, -1, 0}, {0, -1, 0}, {0, -1, 0}, {0, -1, 0}, {0, -1, 0},

                // 24-29: 右面 (Right Face), 法线朝向 +X
                {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0},

                // 30-35: 左面 (Left Face), 法线朝向 -X
                {-1, 0, 0}, {-1, 0, 0}, {-1, 0, 0}, {-1, 0, 0}, {-1, 0, 0}, {-1, 0, 0},
        };
        public static final float[][] CUBE_UV0 = {
                // 0-5: 前面 (Front Face)
                {0, 1}, {1, 1}, {1, 0}, // 三角形 1 (左下, 右下, 右上)
                {0, 1}, {1, 0}, {0, 0}, // 三角形 2 (左下, 右上, 左上)

                // 6-11: 后面 (Back Face) - 注意UV为了正面显示可能需要翻转
                {1, 1}, {0, 1}, {0, 0},
                {1, 1}, {0, 0}, {1, 0},

                // 12-17: 上面 (Top Face)
                {0, 1}, {1, 1}, {1, 0},
                {0, 1}, {1, 0}, {0, 0},

                // 18-23: 下面 (Bottom Face)
                {0, 1}, {1, 1}, {1, 0},
                {0, 1}, {1, 0}, {0, 0},

                // 24-29: 右面 (Right Face)
                {0, 1}, {1, 1}, {1, 0},
                {0, 1}, {1, 0}, {0, 0},

                // 30-35: 左面 (Left Face)
                {0, 1}, {1, 1}, {1, 0},
                {0, 1}, {1, 0}, {0, 0},
        };

        public static final Vec3 staticposition=new Vec3(60,-58,60);
        public static VertexBuffer vertexBuffer =null;
        public static final ResourceLocation ShaderTexture=ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,"textures/140903_top5.png");
    //    @SubscribeEvent
    //    private static void onRenderLevelStage(RenderLevelStageEvent event) {
    //        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
    //        if(vertexBuffer==null||vertexBuffer.isInvalid()){
    //            bake();
    //        }
    //        var camera = event.getCamera().getPosition();
    //        var position = staticposition.subtract(camera);
    //        var shader=GameRenderer.getPositionTexShader();
    //        RenderSystem.disableCull();
    //        vertexBuffer.bind();
    //        RenderSystem.setShaderTexture(0,ShaderTexture);
    //        RenderSystem.setShader(() -> shader);
    //        var mv=event.getModelViewMatrix();
    //        mv.translate((float) position.x, (float) position.y, (float) position.z);
    //        mv.scale(2.5f,2f,2f);
    //        vertexBuffer.drawWithShader(mv, RenderSystem.getProjectionMatrix(), shader);
    //        VertexBuffer.unbind();
    //        RenderSystem.enableCull();
    //    }
    //    private static void bake() {
    //        var tesselator = Tesselator.getInstance();
    //        var bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_TEX);
    //
    //        for (int i = 0; i< CUBE.length; i++) {
    //            float[] pos = CUBE[i];
    //            float[] uv0 = CUBE_UV0[i];
    //            bufferBuilder
    //                    .addVertex(pos[0], pos[1], pos[2])
    //                    .setUv(uv0[0],uv0[1]);
    //        }
    //        var meshData = bufferBuilder.build();
    //        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    //        vertexBuffer.bind();
    //        vertexBuffer.upload(meshData);
    //        VertexBuffer.unbind();
    //    }
    //@SubscribeEvent
    //private static void onRenderLevelStage(RenderLevelStageEvent event) {
    //    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
    //    if(vertexBuffer==null||vertexBuffer.isInvalid()){
    //        bake();
    //    }
    //    var camera = event.getCamera().getPosition();
    //    var position = staticposition.subtract(camera);
    //    var shader=GameRenderer.getRendertypeEntitySolidShader();
    //    vertexBuffer.bind();
    //    RenderSystem.setShaderTexture(0,ShaderTexture);
    //    RenderSystem.setShader(() -> shader);
    //    var mv=event.getModelViewMatrix();
    //    mv.translate((float) position.x, (float) position.y, (float) position.z);
    //    mv.scale(2.5f,2f,2f);
    //    vertexBuffer.drawWithShader(mv, RenderSystem.getProjectionMatrix(), shader);
    //    VertexBuffer.unbind();
    //
    //}
    //    private static void bake() {
    //        var tesselator = Tesselator.getInstance();
    //        var bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.NEW_ENTITY);
    //
    //        for (int i = 0; i< CUBE.length; i++) {
    //            float[] pos = CUBE[i];
    //            float[] uv0 = CUBE_UV0[i];
    //            bufferBuilder
    //                    .addVertex(pos[0], pos[1], pos[2])
    //                    .setColor(1.0f,1.0f,1.0f,1.0f)
    //                    .setUv(uv0[0],uv0[1])
    //                    .setOverlay(OverlayTexture.NO_OVERLAY)
    //                    .setLight(FULL_BRIGHT)
    //                    .setNormal(0,0,1);
    //        }
    //        var meshData = bufferBuilder.build();
    //        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    //        vertexBuffer.bind();
    //        vertexBuffer.upload(meshData);
    //        VertexBuffer.unbind();
    //    }
//    @SubscribeEvent
//    private static void onRenderLevelStage(RenderLevelStageEvent event) {
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
//        if(vertexBuffer==null||vertexBuffer.isInvalid()){
//            bake();
//        }
//        var camera = event.getCamera().getPosition();
//        var position = staticposition.subtract(camera);
//        var shader=GameRenderer.getPositionColorShader();
//        vertexBuffer.bind();
//        RenderSystem.setShaderTexture(0,ShaderTexture);
//        RenderSystem.setShader(() -> shader);
//        var mv=event.getModelViewMatrix();
//        mv.translate((float) position.x, (float) position.y, (float) position.z);
//    //    mv.scale(2.5f,2f,2f);
//        vertexBuffer.drawWithShader(mv, RenderSystem.getProjectionMatrix(), shader);
//        VertexBuffer.unbind();
//
//    }
        static void bake(int color) {
            var tesselator = Tesselator.getInstance();
            var bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR_LIGHTMAP);

            for (int i = 0; i< CUBE.length; i++) {
                float[] pos = CUBE[i];
                float[] uv0 = CUBE_UV0[i];
                bufferBuilder
                        .addVertex(pos[0], pos[1], pos[2])
                        .setColor(color)
                        .setLight(FULL_BRIGHT);
            }
            var meshData = bufferBuilder.build();
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vertexBuffer.bind();
            vertexBuffer.upload(meshData);
            VertexBuffer.unbind();
        }
    //    private static void renderStaticItemEffect(RenderLevelStageEvent event, ItemStack stack, Level level) {
    //        Minecraft mc = Minecraft.getInstance();
    //        BakedModel bakedModel = mc.getItemRenderer().getModel(stack, level, mc.player, 0);
    //
    //        // *** 核心改动：获取渲染位置的动态光照值 ***
    //        int packedLight = mc.levelRenderer.getLightColor(level, BlockPos.containing(POS));
    //
    //        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
    //        PoseStack poseStack = event.getPoseStack();
    //        poseStack.pushPose();
    //
    //        applyWorldTransforms(event,poseStack, mc);
    //        PoseStack.Pose pose = poseStack.last();
    //
    //        List<RenderType> renderTypes = List.of(RenderTypeHelper.getFallbackItemRenderType(stack, bakedModel, false));
    //        for (RenderType renderType : renderTypes) {
    //            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
    //            buildOriginalModelVertices(vertexConsumer,pose,bakedModel,packedLight);
    //        }
    //        poseStack.popPose();
    //    }

    //    private static void applyWorldTransforms(RenderLevelStageEvent event, PoseStack poseStack, Minecraft mc) {
    //        Vec3 cam=mc.gameRenderer.getMainCamera().getPosition();
    //        var p=POS.subtract(cam);
    //        poseStack.translate(p.x,p.y,p.z);
    //        poseStack.scale(2,2,2);
    //    }

        // 方法签名增加一个参数来接收光照值
    //    public static void buildOriginalModelVertices(VertexConsumer buffer, PoseStack.Pose pose, BakedModel bakedModel, int packedLight) {
    //        RandomSource random = RandomSource.create();
    //        List<BakedQuad> allQuads = Lists.newArrayList();
    //
    //        allQuads.addAll(bakedModel.getQuads(null, null, random, ModelData.EMPTY, null));
    //        for (Direction side : Direction.values()) {
    //            allQuads.addAll(bakedModel.getQuads(null, side, random, ModelData.EMPTY, null));
    //        }
    //
    //        if (allQuads.isEmpty()) return;
    //
    //        for (BakedQuad quad : allQuads) {
    //            buffer.putBulkData(
    //                    pose,
    //                    quad,
    //                    0f, // Red tint
    //                    1.0f, // Green tint
    //                    1.0f, // Blue tint
    //                    1f, // Alpha
    //                    packedLight, // *** 使用动态获取的光照值 ***
    //                    OverlayTexture.NO_OVERLAY,
    //                    false
    //            );
    //        }
    //    }

    }

    public static class FearEffectLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        private static final ItemStack WITHER_SKULL_STACK = new ItemStack(Items.NETHERITE_SWORD);

        private static final float R = 100 / 255.0f;
        private static final float G = 230 / 255.0f;
        private static final float B= 255 / 255.0f;
        private static final float A = 0.675f;
        public FearEffectLayer(RenderLayerParent<T, M> pRenderer) {
            super(pRenderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!entity.hasEffect(ModEffects.FEAR)) {
                return;
            }
            poseStack.pushPose();
            AABB entityBounds = entity.getBoundingBox();
            double topY = entityBounds.maxY - entity.getY();
            poseStack.translate(0, -0.5-topY, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
            Minecraft mc = Minecraft.getInstance();
            BakedModel bakedModel = mc.getItemRenderer().getModel(WITHER_SKULL_STACK, mc.level, mc.player, 0);
            List<RenderType> renderTypes = List.of(RenderTypeHelper.getFallbackItemRenderType(WITHER_SKULL_STACK, bakedModel, false));
            for (RenderType renderType : renderTypes) {
                VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
                // 将计算出的光照值传递给顶点构建方法
                renderModelQuads(vertexConsumer, poseStack.last(), bakedModel, packedLight);
            }
            poseStack.popPose();
        }
        private void renderModelQuads(VertexConsumer buffer, PoseStack.Pose pose, BakedModel bakedModel, int packedLight) {
            RandomSource random = RandomSource.create();

            List<BakedQuad> allQuads = Lists.newArrayList(bakedModel.getQuads(null, null, random, ModelData.EMPTY, null));
            for (Direction side : Direction.values()) {
                allQuads.addAll(bakedModel.getQuads(null, side, random, ModelData.EMPTY, null));
            }

            if (allQuads.isEmpty()) {
                return;
            }

            for (BakedQuad quad : allQuads) {
                buffer.putBulkData(
                        pose,
                        quad,
                        R,
                        G,
                        B,
                        A,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        false
                );
            }
        }
    }

//    @SubscribeEvent
//    public static void onRenderLevelStage(RenderLevelStageEvent event) {
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {return;}
//        Player player = Minecraft.getInstance().player;
//        Level level = Minecraft.getInstance().level;
//        if (player == null || level == null || player.getMainHandItem().isEmpty() || !player.getMainHandItem().has(DataComponents.POTION_CONTENTS)) {return;}
//        Optional<Vec3> targetOpt = findFirstNonAirBlockOnTrajectory(player, level);
//        targetOpt.ifPresent(targetPos -> {
//            int color = player.getMainHandItem().get(DataComponents.POTION_CONTENTS).getColor();
//            Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
//            Test.custom.setupRenderState();
//            bake(color);
//            var mv=event.getModelViewMatrix();
//            vertexBuffer.bind();
//            vertexBuffer.drawWithShader(mv.translate(targetPos.subtract(cameraPos).toVector3f()),event.getProjectionMatrix(),GameRenderer.getRendertypeLeashShader());
//            VertexBuffer.unbind();
//            Test.custom.clearRenderState();
//        });
//    }
//    static void bake(int color) {
//        var tesselator = Tesselator.getInstance();
//        var bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR_LIGHTMAP);
//
//        for (int i = 0; i< CUBE.length; i++) {
//            float[] pos = CUBE[i];
//            float[] uv0 = CUBE_UV0[i];
//            bufferBuilder
//                    .addVertex(pos[0], pos[1], pos[2])
//                    .setColor(color)
//                    .setLight(FULL_BRIGHT);
//        }
//        var meshData = bufferBuilder.build();
//        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
//        vertexBuffer.bind();
//        vertexBuffer.upload(meshData);
//        VertexBuffer.unbind();
//    }
//
//    /**
//     * 沿着抛物线轨迹，生成间距为1的离散点，并返回第一个位于非空气方块的点的位置。
//     */
//    private static Optional<Vec3> findFirstNonAirBlockOnTrajectory(Player player, Level level) {
//        // --- 物理参数初始化 ---
//        Vec3 position = player.getEyePosition().subtract(0, 0.1, 0);
//        Vec3 velocity = player.getLookAngle().scale(3F);
//        final float gravity = 0.03F;
//        final float drag = 0.99F;
//
//        double totalDistanceTraveled = 0.0;
//        double nextCheckDistance = 1.0; // 第一个检查点在距离为1的位置
//
//        // --- 模拟与检查循环 ---
//        for (int tick = 0; tick < 300; tick++) { // 安全上限，防止无限循环
//            Vec3 segmentStart = position;
//            Vec3 segmentEnd = position.add(velocity);
//            Vec3 segmentVector = segmentEnd.subtract(segmentStart);
//            double segmentLength = segmentVector.length();
//
//            // 如果当前模拟步长内可能包含一个或多个检查点
//            while (totalDistanceTraveled + segmentLength >= nextCheckDistance) {
//                // 计算这个检查点在当前路径段上的精确位置
//                double distanceIntoSegment = nextCheckDistance - totalDistanceTraveled;
//                Vec3 pointToCheck = segmentStart.add(segmentVector.normalize().scale(distanceIntoSegment));
//                BlockPos blockPosToCheck = BlockPos.containing(pointToCheck);
//
//                // 检查该点的方块是否为非空气
//                if (!level.getBlockState(blockPosToCheck).isAir()) {
//                    // 找到了！这就是离初始点最近的非空气方块在离散点上
//                    return Optional.of(pointToCheck);
//                }
//
//                // 准备检查下一个点
//                nextCheckDistance += 1.0;
//            }
//
//            // --- 更新物理状态以进行下一次模拟 ---
//            totalDistanceTraveled += segmentLength;
//            position = segmentEnd;
//            velocity = velocity.scale(drag).add(0, -gravity, 0);
//
//            if (velocity.lengthSqr() < 0.001) {
//                break; // 速度过低，终止模拟
//            }
//        }
//
//        // 在模拟范围内没有找到任何满足条件的点
//        return Optional.empty();
//    }
    private static final int MERGE_LENGTH_THRESHOLD = 6;

//    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        List<Component> tooltip = event.getToolTip();

        mergeConsecutiveShortLines(tooltip);
    }

    private static void mergeConsecutiveShortLines(List<Component> tooltipLines) {
        if (tooltipLines.isEmpty()) return;
        List<Component> newTooltip = new ArrayList<>();
        int i = 0;
        while (i < tooltipLines.size()) {
            List<Component> shortLineSequence = new ArrayList<>();
            int j = i;
            while (j < tooltipLines.size()) {
                Component lineToCheck = tooltipLines.get(j);
                String text = lineToCheck.getString();
                if (!text.trim().isEmpty() && text.length() < MERGE_LENGTH_THRESHOLD) {
                    shortLineSequence.add(lineToCheck);
                    j++;
                } else {
                    break;
                }
            }
            if (shortLineSequence.size() > 1) {
                MutableComponent mergedLine = Component.empty();
                for (int k = 0; k < shortLineSequence.size(); k++) {
                    mergedLine.append(shortLineSequence.get(k));
                }
                newTooltip.add(mergedLine);
                i = j;
            } else {
                newTooltip.add(tooltipLines.get(i));
                i++;
            }
        }
        tooltipLines.clear();
        tooltipLines.addAll(newTooltip);
    }

}
