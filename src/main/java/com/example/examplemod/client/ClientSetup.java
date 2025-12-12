package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.entity.GoldenGateRenderer;
import com.example.examplemod.client.entity.SwordProjectileRenderer;


import com.example.examplemod.client.particle.BlackHoleParticle;
import com.example.examplemod.client.particle.ModParticles;
import com.example.examplemod.client.screen.AttributeEditorScreen;
import com.example.examplemod.client.screen.TradeScreen;
import com.example.examplemod.client.util.CustomCubeRenderer;
import com.example.examplemod.client.util.EffectOrbitRenderer;
import com.example.examplemod.component.*;
import com.example.examplemod.register.ModEntities;
import com.example.examplemod.register.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ClientTooltipComponentManager;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static com.example.examplemod.register.ModEffects.MAKEN_POWER;
import static com.example.examplemod.register.ModEntities.GOLDENGATEENTITY;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TRADE_MENU.get(), TradeScreen::new);
    }
    // 定义按键 (例如 'K' 键)
    public static final KeyMapping OPEN_EDITOR_KEY = new KeyMapping(
            "key.examplemod.open_editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.examplemod"
    );
    @SubscribeEvent
    public static void onclientsetup(FMLClientSetupEvent event) {
    }


    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR_KEY);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_EDITOR_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ItemStack stack = mc.player.getMainHandItem();

            // 仅限创造模式才能打开 (可选)
            if (!mc.player.isCreative()) {
                mc.player.displayClientMessage(Component.literal("需要创造模式！"), true);
                return;
            }

            if (!stack.isEmpty()) {
                // 直接在客户端打开 Screen
                // 因为这只是个 UI，不需要服务端的 Menu/Container 参与
                // 数据同步靠 UI 里的 Packet 发送
                mc.setScreen(new AttributeEditorScreen(stack));
            } else {
                mc.player.displayClientMessage(Component.literal("请手持物品！"), true);
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册你的传送门实体渲染器
        event.registerEntityRenderer(GOLDENGATEENTITY.get(), GoldenGateRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_PROJECTILE.get(), SwordProjectileRenderer::new);

    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BLACK_HOLE_MATTER.get(),
                // 注意：因为我们是 CUSTOM 渲染，不使用贴图，但 Forge 注册通常需要一个 dummy sprite set
                // 如果报错，可以传入一个空实现，或者仅仅注册 Provider 不带 SpriteSet
                (spriteSet) -> new BlackHoleParticle.Provider()
        );
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        // 过滤：只给有药水效果的实体画，或者只给玩家画
        if (entity.getActiveEffects().isEmpty()) return;

        // 距离剔除：太远就不画了，优化性能
        if (Minecraft.getInstance().player.distanceToSqr(entity) > 4096) return;

        // 调用我们的渲染器
        EffectOrbitRenderer.renderOrbitingEffects(
                entity,
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPartialTick()
        );
    }



    public static class ConditionalScaledItemModel extends BakedModelWrapper<BakedModel> {

        private final Vector3f scale;

        public ConditionalScaledItemModel(BakedModel originalModel, Vector3f scale) {
            super(originalModel);
            this.scale = scale;
        }

        @Override
        public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext context, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
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


    @SubscribeEvent
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 只在 AFTER_LEVEL 阶段进行绘制
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        // 调用我们单一职责的渲染器实例
        CustomCubeRenderer.INSTANCE.render(event);
    }
}


