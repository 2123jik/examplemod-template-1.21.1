//package com.example.examplemod.client.effect;
//
//import com.example.examplemod.ExampleMod;
//import com.example.examplemod.client.particle.ModParticles;
//import com.mojang.blaze3d.platform.GlStateManager;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.*;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.renderer.GameRenderer;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//import org.joml.Matrix4f;
//
//@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
//public class BlackHole {
//
//    private static VertexBuffer coreBuffer = null;
//    private static VertexBuffer ringBuffer = null;
//    private static final double PARTICLES_PER_SECOND =8.0;
//    private static double timeAccumulator = 0;
//    private static long lastFrameTime = System.nanoTime();
//    private static final int SEGMENTS = 16;
//    private static final float RADIUS = 1f;
//    private static final float RING_WIDTH = 0.02f;
//    private static final float HEIGHT = 0.1f;
//
//    @SubscribeEvent
//    public static void onRenderLevelStage(RenderLevelStageEvent event) {
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
//
//        if (coreBuffer == null || coreBuffer.isInvalid() || ringBuffer == null || ringBuffer.isInvalid()) {
//            bakeGeometry();
//        }
//
//        Vec3 camPos = event.getCamera().getPosition();
//        Vec3 targetPos = new Vec3(0, 80, 0);
//        Vec3 renderPos = targetPos.subtract(camPos);
//
//        var shader = GameRenderer.getPositionColorShader();
//        RenderSystem.setShader(() -> shader);
//        RenderSystem.enableBlend();
//        RenderSystem.defaultBlendFunc();
//
//        RenderSystem.enableDepthTest();
//        RenderSystem.depthMask(false); // 半透明物体通常不写入深度缓冲
//        RenderSystem.enableCull();     // 开启剔除，因为我们的几何体是闭合的
//
//        // 4. 构建基础矩阵
//        Matrix4f baseMatrix = new Matrix4f(event.getModelViewMatrix());
//        baseMatrix.translate((float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
//        Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
//
//        float gameTime = Minecraft.getInstance().level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
//
//        // --- 渲染核心 (Core) ---
//        coreBuffer.bind();
//        // 核心设为纯黑，不透明
//        RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
//        coreBuffer.drawWithShader(baseMatrix, projMatrix, shader);
//        VertexBuffer.unbind();
//
//        // --- 渲染吸积盘 (Ring) ---
//        RenderSystem.depthMask(false);
//        ringBuffer.bind();
//
//        // 动态波纹层
//        int waveLayers = 1;
//        for (int i = 0; i < waveLayers; i++) {
//            float progress = (gameTime / 60.0f + (float) i / waveLayers) % 1.0f;
//
//            float scale = 0.65F + 0.3F * progress;
//            float alpha = 1.0f - progress;
//
//            Matrix4f waveMat = new Matrix4f(baseMatrix);
//            waveMat.scale(scale, HEIGHT*1.5f, scale);
//
//            RenderSystem.setShaderColor(0.6f, 0.1f, 1.0f, alpha);
//            ringBuffer.drawWithShader(waveMat, projMatrix, shader);
//        }
//
//        // 渲染一个稳定的内圈光环，增加层次感
//        Matrix4f staticMat = new Matrix4f(baseMatrix);
//        staticMat.scale(1f,HEIGHT*1.5f,1f);
//        RenderSystem.setShaderColor(0.8f, 0.4f, 1.0f, 0.8f);
//        ringBuffer.drawWithShader(staticMat, projMatrix, shader);
//
//        VertexBuffer.unbind();
//
//        // 5. 恢复渲染状态
//        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//        RenderSystem.disableBlend();
//        RenderSystem.depthMask(true);
//        RenderSystem.disableCull(); // 恢复默认
//        // --- 新的粒子生成逻辑 ---
//        if (Minecraft.getInstance().level != null && !Minecraft.getInstance().isPaused()) {
//            long now = System.nanoTime();
//            // 计算两帧之间经过的秒数
//            double deltaTime = (now - lastFrameTime) / 1_000_000_000.0;
//            lastFrameTime = now;
//
//            // 防止游戏暂停或极低FPS导致积累过多，设置最大上限（例如最大只累积0.1秒）
//            if (deltaTime > 0.1) deltaTime = 0.1;
//
//            // 累加时间
//            timeAccumulator += deltaTime * PARTICLES_PER_SECOND;
//
//            // 计算这一帧应该生成多少个粒子 (取整)
//            int countToSpawn = (int) timeAccumulator;
//
//            // 如果需要生成
//            if (countToSpawn > 0) {
//                float emissionRadius = RADIUS + RING_WIDTH * 0.8f;
//                // 调用生成方法，传入需要生成的数量
//                spawnAccretionParticles(Minecraft.getInstance().level, targetPos, emissionRadius, countToSpawn);
//
//                // 减去已消耗的配额（保留小数部分，保证平滑）
//                timeAccumulator -= countToSpawn;
//            }
//        } else {
//            // 如果暂停，重置计时器防止暂停结束后瞬间爆发
//            lastFrameTime = System.nanoTime();
//        }
//    }
//    private static void spawnAccretionParticles(ClientLevel level, Vec3 center, float radius, int count) {
//        // 距离剔除优化：如果离黑洞太远（例如超过100格），完全不生成粒子，极其省性能
//        if (Minecraft.getInstance().cameraEntity != null) {
//            if (Minecraft.getInstance().cameraEntity.position().distanceToSqr(center) > 100 * 100) {
//                return;
//            }
//        }
//
//        for(int i = 0; i < count; i++) {
//            double angle = Math.random() * Math.PI * 2;
//
//            // 位置在圆环上
//            double spawnX = center.x + Math.cos(angle) * radius;
//            double spawnY = center.y + 0.16f;
//            double spawnZ = center.z + Math.sin(angle) * radius;
//
//            double velocityY = 0.1F;
//
//            level.addParticle(
//                    ModParticles.BLACK_HOLE_MATTER.get(),
//                    spawnX, spawnY, spawnZ,
//                    0, velocityY, 0
//            );
//        }
//    }
//    /**
//     * 烘焙几何体数据到显存
//     */
//    private static void bakeGeometry() {
//        Tesselator tesselator = Tesselator.getInstance();
//        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
//
//        // --- 1. 构建核心 (实心圆柱) ---
//        // 参数：builder, 半径, 高度, 分段数, 颜色(白)
//        MeshBuilder.addSolidCylinder(builder, RADIUS, HEIGHT, SEGMENTS, 255, 255, 255, 255);
//        MeshData coreData = builder.build();
//        coreBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
//        coreBuffer.bind();
//        coreBuffer.upload(coreData);
//
//        // --- 2. 构建圆环 (空心管) ---
//        builder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
//        // 参数：builder, 内径, 外径, 高度, 分段数, 颜色(白)
//        MeshBuilder.addHollowRing(builder, RADIUS, RADIUS + RING_WIDTH, HEIGHT, SEGMENTS, 255, 255, 255, 255);
//        MeshData ringData = builder.build();
//        ringBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
//        ringBuffer.bind();
//        ringBuffer.upload(ringData);
//
//        VertexBuffer.unbind();
//    }
//
//    /**
//     * 内部工具类：用于生成标准几何体网格
//     * 可提取到单独的文件中供其他类使用
//     */
//    public static class MeshBuilder {
//
//        /**
//         * 构建一个实心的圆柱体（或圆盘）
//         */
//        public static void addSolidCylinder(BufferBuilder builder, float radius, float height, int segments, int r, int g, int b, int a) {
//            float halfH = height / 2.0f;
//
//            for (int i = 0; i < segments; i++) {
//                double angle1 = 2.0 * Math.PI * i / segments;
//                double angle2 = 2.0 * Math.PI * (i + 1) / segments;
//
//                float x1 = (float) Math.sin(angle1) * radius;
//                float z1 = (float) Math.cos(angle1) * radius;
//                float x2 = (float) Math.sin(angle2) * radius;
//                float z2 = (float) Math.cos(angle2) * radius;
//
//                // 顶面 (中心点 -> 边缘1 -> 边缘2)
//                builder.addVertex(0, halfH, 0).setColor(r, g, b, a);
//                builder.addVertex(x1, halfH, z1).setColor(r, g, b, a);
//                builder.addVertex(x2, halfH, z2).setColor(r, g, b, a);
//
//                // 底面 (注意顶点顺序以保证法线朝下)
//                builder.addVertex(0, -halfH, 0).setColor(r, g, b, a);
//                builder.addVertex(x2, -halfH, z2).setColor(r, g, b, a);
//                builder.addVertex(x1, -halfH, z1).setColor(r, g, b, a);
//
//                // 侧面 (四边形拆分为两个三角形)
//                addQuad(builder,
//                        x2, halfH, z2,  // Top-Right
//                        x1, halfH, z1,  // Top-Left
//                        x1, -halfH, z1, // Bottom-Left
//                        x2, -halfH, z2, // Bottom-Right
//                        r, g, b, a);
//            }
//        }
//
//        /**
//         * 构建一个空心的圆环（类似垫圈）
//         */
//        public static void addHollowRing(BufferBuilder builder, float innerRadius, float outerRadius, float height, int segments, int r, int g, int b, int a) {
//            float halfH = height / 2.0f;
//
//            for (int i = 0; i < segments; i++) {
//                double angle1 = 2.0 * Math.PI * i / segments;
//                double angle2 = 2.0 * Math.PI * (i + 1) / segments;
//
//                float sin1 = (float) Math.sin(angle1);
//                float cos1 = (float) Math.cos(angle1);
//                float sin2 = (float) Math.sin(angle2);
//                float cos2 = (float) Math.cos(angle2);
//
//                // 当前角度的内外点
//                float x1_in = sin1 * innerRadius;
//                float z1_in = cos1 * innerRadius;
//                float x1_out = sin1 * outerRadius;
//                float z1_out = cos1 * outerRadius;
//
//                // 下一角度的内外点
//                float x2_in = sin2 * innerRadius;
//                float z2_in = cos2 * innerRadius;
//                float x2_out = sin2 * outerRadius;
//                float z2_out = cos2 * outerRadius;
//
//                // 1. 顶面 (Top Face)
//                addQuad(builder,
//                        x1_out, halfH, z1_out,
//                        x2_out, halfH, z2_out,
//                        x2_in,  halfH, z2_in,
//                        x1_in,  halfH, z1_in,
//                        r, g, b, a);
//
//                // 2. 底面 (Bottom Face)
//                addQuad(builder,
//                        x1_in,  -halfH, z1_in,
//                        x2_in,  -halfH, z2_in,
//                        x2_out, -halfH, z2_out,
//                        x1_out, -halfH, z1_out,
//                        r, g, b, a);
//
//                // 3. 外侧壁 (Outer Wall)
//                addQuad(builder,
//                        x2_out, halfH,  z2_out,
//                        x1_out, halfH,  z1_out,
//                        x1_out, -halfH, z1_out,
//                        x2_out, -halfH, z2_out,
//                        r, g, b, a);
//
//                // 4. 内侧壁 (Inner Wall)
//                addQuad(builder,
//                        x1_in, halfH,  z1_in,
//                        x2_in, halfH,  z2_in,
//                        x2_in, -halfH, z2_in,
//                        x1_in, -halfH, z1_in,
//                        r, g, b, a);
//            }
//        }
//
//        /**
//         * 基础四边形绘制助手 (自动拆分三角形)
//         */
//        private static void addQuad(BufferBuilder builder,
//                                    float x1, float y1, float z1,
//                                    float x2, float y2, float z2,
//                                    float x3, float y3, float z3,
//                                    float x4, float y4, float z4,
//                                    int r, int g, int b, int a) {
//            // Triangle 1
//            builder.addVertex(x1, y1, z1).setColor(r, g, b, a);
//            builder.addVertex(x2, y2, z2).setColor(r, g, b, a);
//            builder.addVertex(x4, y4, z4).setColor(r, g, b, a);
//            // Triangle 2
//            builder.addVertex(x2, y2, z2).setColor(r, g, b, a);
//            builder.addVertex(x3, y3, z3).setColor(r, g, b, a);
//            builder.addVertex(x4, y4, z4).setColor(r, g, b, a);
//        }
//    }
//}