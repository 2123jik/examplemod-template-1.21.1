package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.entity.GoldenGateRenderer;
import com.example.examplemod.client.entity.SwordProjectileRenderer;


import com.example.examplemod.component.ModDataComponents;
import com.example.examplemod.init.ModEffects;
import com.example.examplemod.register.ModEntities;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.*;

import static com.example.examplemod.init.ModEffects.MAKEN_POWER;
import static com.example.examplemod.register.ModEntities.GOLDENGATEENTITY;
import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

@EventBusSubscriber(modid = ExampleMod.MODID,value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册你的传送门实体渲染器
        event.registerEntityRenderer(GOLDENGATEENTITY.get(), GoldenGateRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_PROJECTILE.get(), SwordProjectileRenderer::new);
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

//    @EventBusSubscriber(value = Dist.CLIENT)
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
}
