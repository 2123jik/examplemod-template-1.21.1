package com.example.examplemod.client.util;

// ... (省略所有导入，假设与上一个回答一致)
import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomCubeRenderer {

    // 【核心改动 1】：VBO 缓存 Map
    private static final Map<ResourceLocation, VertexBuffer> EFFECT_VBO_CACHE = new ConcurrentHashMap<>();
    private static boolean isInitialized = false;

    private static final float CUBE_SCALE = 0.5F;
    private static final float ICON_SPACING = CUBE_SCALE + 0.1F;

    public static final CustomCubeRenderer INSTANCE = new CustomCubeRenderer();
    private CustomCubeRenderer() {
        if (!isInitialized) {
            ExampleMod.LOGGER.info("Client | CustomCubeRenderer: Initializing VBO Cache System.");
            isInitialized = true;
        }
    }

    /**
     * 【核心改动 2】：用于创建 VBO 的新方法
     */
    private VertexBuffer bakeVBO(TextureAtlasSprite sprite) {
        var tesselator = Tesselator.getInstance();
        var bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        // 填充顶点和动态 UV
        for (int i = 0; i < CubeData.CUBE_POSITIONS.length; i++) {
            float u = CubeData.CUBE_UVS[i][0];
            float v = CubeData.CUBE_UVS[i][1];

            // 核心：UV 映射
            float finalU = Mth.lerp(u, sprite.getU0(), sprite.getU1());
            float finalV = Mth.lerp(v, sprite.getV0(), sprite.getV1());

            bufferBuilder
                    .addVertex(CubeData.CUBE_POSITIONS[i][0], CubeData.CUBE_POSITIONS[i][1], CubeData.CUBE_POSITIONS[i][2])
                    .setUv(finalU, finalV);
        }
        var meshData = bufferBuilder.build();

        // 创建新的 VBO 并上传数据
        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC); // STATIC VBO
        vbo.bind();
        vbo.upload(meshData);
        VertexBuffer.unbind();

        return vbo;
    }


    /**
     * 渲染方法：在 RenderLevelStageEvent.Stage.AFTER_LEVEL 阶段调用
     */
    public void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Tesselator tesselator = Tesselator.getInstance();
        if (mc.level == null || mc.player == null) return;

//        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
//        Vec3 cameraPos = event.getCamera().getPosition();
//        Matrix4f originalModelView = event.getModelViewMatrix();
//
//        RenderSystem.setShader(GameRenderer::getRendertypeCutoutShader);
//        RenderSystem.enableBlend();
//        RenderSystem.defaultBlendFunc();
//        RenderSystem.enableDepthTest();
//        RenderSystem.depthFunc(515); // 使用 LEQUAL 深度测试以防Z-Fighting
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//
//        ResourceLocation effectAtlasLocation = mc.getMobEffectTextures().get(MobEffects.JUMP).atlasLocation();
//        RenderSystem.setShaderTexture(0, effectAtlasLocation);
//        for(UUID entityUuid : ClientEffectMapUpdater.getEntitiesWithEffects()) {
//            // ... (省略实体查找和 LivingEntity 检查)
//            Entity entity = null;
//            for (Entity currentEntity : mc.level.entitiesForRendering()) {
//                if (currentEntity.getUUID().equals(entityUuid)) {
//                    entity = currentEntity;
//                    break;
//                }
//            }
//            if (entity == null || entity.isRemoved() || !(entity instanceof LivingEntity livingEntity)) continue;
//
//            // --- 基础位置计算 (包含高度修正) ---
//            float halfHeight = livingEntity.getBbHeight() / 2.0F;
//            float iconHeightOffset = halfHeight + (CUBE_SCALE / 2.0F) + 0.1F; // 抬高图标
//
//            double x0 = Mth.lerp(partialTicks, livingEntity.xOld, livingEntity.getX());
//            double y0 = Mth.lerp(partialTicks, livingEntity.yOld, livingEntity.getY());
//            double z0 = Mth.lerp(partialTicks, livingEntity.zOld, livingEntity.getZ());
//            var dispatcher = mc.getEntityRenderDispatcher();
//            Vec3 offset = dispatcher.getRenderer(livingEntity).getRenderOffset(livingEntity, partialTicks);
//            Vec3 worldPos = new Vec3(x0 + offset.x(), y0 + offset.y() + iconHeightOffset, z0 + offset.z());
//            Vec3 localPos = worldPos.subtract(cameraPos);
//            // ------------------------------------
//
//            // 3. 【核心修正】：创建 ModelView 矩阵副本并操作
//            Matrix4f baseMatrix = new Matrix4f(originalModelView); // 复制原始矩阵
//
//            // 4. 基础变换 (平移到世界坐标，并应用自转/缩放)
//            baseMatrix.translate((float) localPos.x, (float) localPos.y +1.5F, (float) localPos.z);
//            // 【核心改动 3】：平滑跟随实体朝向 (Yaw)
//            // 1. 计算平滑的 Yaw 角
//            float smoothYaw = Mth.lerp(partialTicks, livingEntity.yBodyRotO, livingEntity.yBodyRot);
//            // 2. 绕 Y 轴旋转，使其面向实体的前方。Minecraft Yaw 是绕 Y 轴的。
//            //    注意：通常需要取负值来匹配世界坐标系和实体朝向。
//            baseMatrix.rotate(Axis.YP.rotationDegrees(-smoothYaw));
//
//            baseMatrix.scale(CUBE_SCALE, CUBE_SCALE, CUBE_SCALE/128);
//
//            var effectIds = ClientEffectMapUpdater.getEntityEffectIds(entityUuid);
//            int effectCount = effectIds.size();
//            int index = 0;
//
//            for (ResourceLocation effectId : effectIds) {
//                // ... (MobEffect 和 Sprite 检查)
//                MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get((effectId));
//                if (mobEffect == null) continue;
//
//                TextureAtlasSprite sprite = mc.getMobEffectTextures().get(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect));
//                if (sprite == null) continue;
//
//                // 5. VBO 缓存/烘焙
//                VertexBuffer vbo = EFFECT_VBO_CACHE.computeIfAbsent(effectId, k -> bakeVBO(sprite));
//
//                // 6. 单独图标偏移
//                float currentOffset = (index - (effectCount - 1) / 2.0F) * ICON_SPACING;
//
//                // 7. 【最终矩阵】：再次复制 baseMatrix，并应用偏移
//                Matrix4f finalMatrix = new Matrix4f(baseMatrix);
//                finalMatrix.translate(currentOffset, 0.0f, 0.0f);
//
//                // 8. VBO 绘制
//                vbo.bind();
//                vbo.drawWithShader(finalMatrix, RenderSystem.getProjectionMatrix(), GameRenderer.getPositionTexShader());
//                VertexBuffer.unbind();
//
//                index++;
//            }
//        }
//
//        RenderSystem.depthFunc(513); // 恢复默认的 GL_LESS (513)
//        RenderSystem.disableDepthTest();
//        RenderSystem.disableBlend();



    }
    // 【重要】：实现资源清理
    public static void onClientStop() {
        EFFECT_VBO_CACHE.values().forEach(VertexBuffer::close);
        EFFECT_VBO_CACHE.clear();
        ExampleMod.LOGGER.info("Client | VBO Cache System: Cleaned up all cached VBOs.");
    }
}
// ❗ 别忘了在您的客户端停止/退出事件（例如 Minecraft.close() 或类似事件）中调用 CustomCubeRenderer.onClientStop()。