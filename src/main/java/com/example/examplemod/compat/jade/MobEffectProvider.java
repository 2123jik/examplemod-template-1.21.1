package com.example.examplemod.compat.jade;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.util.SmartCooldownRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum MobEffectProvider implements IEntityComponentProvider, StreamServerDataProvider<EntityAccessor, List<MobEffectInstance>> {
    INSTANCE;

    private static final StreamCodec<RegistryFriendlyByteBuf, List<MobEffectInstance>> STREAM_CODEC = ByteBufCodecs.<RegistryFriendlyByteBuf, MobEffectInstance>list()
            .apply(MobEffectInstance.STREAM_CODEC);


    @Override
    public void appendTooltip(ITooltip iTooltip, EntityAccessor entityAccessor, IPluginConfig iPluginConfig) {
        List<MobEffectInstance> effects = this.decodeFromData(entityAccessor).orElse(List.of());
        if (!effects.isEmpty()) {
            IElementHelper helper = IElementHelper.get();
            ITooltip box = helper.tooltip();

            for (MobEffectInstance effect : effects) {
                box.add(new MobEffectElement(effect));
                box.append(helper.spacer(3, 0));
            }

            iTooltip.add(helper.box(box, BoxStyle.getNestedBox()));
        }
    }

    // --- 自定义 UI 元素 (包含你的渲染逻辑) ---

    private static class MobEffectElement implements IElement {
        private static final int ICON_SIZE = 12;
        // 缓存最大持续时间，用于计算冷却比例
        private static final Map<MobEffect, Integer> EFFECT_MAX_DURATION_CACHE = new HashMap<>();

        private final MobEffectInstance effect;

        public MobEffectElement(MobEffectInstance effect) {
            this.effect = effect;
        }

        @Override
        public Vec2 getSize() {
            return new Vec2(ICON_SIZE, ICON_SIZE);
        }

        @Override
        public void render(GuiGraphics guiGraphics, float x, float y, float maxX, float maxY) {
            Minecraft mc = Minecraft.getInstance();
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effect.getEffect());

            // 1. 绘制图标
            RenderSystem.setShaderTexture(0, sprite.atlasLocation());
            RenderSystem.enableBlend();
            // 注意：Jade通常只给18x18的空间，这里y坐标调整取决于你的贴图
            guiGraphics.blit((int) x, (int) y, 0, ICON_SIZE, ICON_SIZE, sprite);


            // 2. 复用逻辑计算比例
            float ratio = SmartCooldownRenderer.calculateRatio(effect, EFFECT_MAX_DURATION_CACHE);

            // 3. 复用逻辑绘制
            // Jade Tooltip 层级不同，这里 Z offset 给一个很小的值 (如 0.01f) 防止Z-fighting即可
            SmartCooldownRenderer.renderCooldown(guiGraphics, x, y, ICON_SIZE, ratio, 0.01f);
            guiGraphics.drawString(mc.font, String.valueOf(effect.getAmplifier()+1), (int)(x+ICON_SIZE/2), (int)(y+ICON_SIZE-2), effect.getEffect().value().getColor());
        }

        @Override public IElement size(@Nullable Vec2 size) { return this; }
        @Override public Vec2 getCachedSize() { return getSize(); }
        @Override public IElement align(Align align) { return this; }
        @Override public Align getAlignment() { return Align.LEFT; }
        @Override public IElement translate(Vec2 translation) { return this; }
        @Override public Vec2 getTranslation() { return Vec2.ZERO; }
        @Override public IElement tag(ResourceLocation tag) { return this; }
        @Override public ResourceLocation getTag() { return null; }
        @Override public @Nullable String getCachedMessage() { return null; }
        @Override public IElement clearCachedMessage() { return this; }
        @Override public IElement message(@Nullable String message) { return this; }
    }

    // --- 其他常规实现 ---
    public static Component getEffectName(MobEffectInstance mobEffectInstance) {
        MutableComponent mutableComponent = mobEffectInstance.getEffect().value().getDisplayName().copy();
        if (mobEffectInstance.getAmplifier() >= 1 && mobEffectInstance.getAmplifier() <= 9) {
            mutableComponent.append(CommonComponents.SPACE).append(Component.translatable(
                    "enchantment.level." + (mobEffectInstance.getAmplifier() + 1)));
        }
        return mutableComponent;
    }

    @Override
    @Nullable
    public List<MobEffectInstance> streamData(EntityAccessor accessor) {
        List<MobEffectInstance> effects = ((LivingEntity) accessor.getEntity()).getActiveEffects()
                .stream()
                .filter(MobEffectInstance::isVisible)
                .toList();
        return effects.isEmpty() ? null : effects;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, List<MobEffectInstance>> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public boolean shouldRequestData(EntityAccessor accessor) {
        return accessor.getEntity() instanceof LivingEntity;
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "mob_effects_jade_plugin");
    }
}