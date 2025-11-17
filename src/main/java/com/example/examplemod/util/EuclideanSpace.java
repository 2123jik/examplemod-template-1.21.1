package com.example.examplemod.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 一个用于在3D空间中渲染几何形状的最终工具类。
 * <p>
 * 该类提供了绘制、变换和构建各种形状（如多边形、星形和球体）的静态方法。
 * 它封装了复杂的顶点操作和矩阵变换，提供了一个简单易用的API。
 * <p>
 * [重构后] 所有渲染方法现在都接受 Level 和 Camera 参数，以实现默认的真实世界光影效果。
 * <p>
 * 所有方法都是静态的，因此该类不能被实例化。
 * <p>
 * 注意：所有渲染方法都使用普通三角形顶点格式。
 */
public final class EuclideanSpace {

    public static void renderShape(PoseStack poseStack, VertexConsumer buffer, Consumer<PoseStack> transformApplier, BiConsumer<VertexConsumer, PoseStack.Pose> vertexBuilder) {
        poseStack.pushPose();
        transformApplier.accept(poseStack);
        vertexBuilder.accept(buffer, poseStack.last());
        poseStack.popPose();
    }

// --- 可重用的通用变换逻辑 ---

    /**
     * 【通用方法】对 PoseStack 应用静态变换（平移和旋转朝向）。
     * <p>
     * 这是一个被提取出来的、可重用的变换逻辑。它将 PoseStack 平移到指定中心，并将其旋转以使其默认法线 (0, 1, 0) 与目标朝向轴对齐。
     *
     * @param poseStack       要变换的 PoseStack。
     * @param center          平移到的目标中心点。
     * @param orientationAxis 形状平面最终应朝向的法线向量。
     */
    public static void applyStaticTransform(PoseStack poseStack, Vec3 center, Vector3f orientationAxis) {
        poseStack.translate(center.x, center.y, center.z);
        Vector3f targetNormal = REUSABLE_VECTOR.set(orientationAxis).normalize();
        poseStack.mulPose(REUSABLE_QUATERNION.rotationTo(DEFAULT_NORMAL, targetNormal));
    }

    /**
     * 【通用方法】对 PoseStack 应用动态旋转变换。
     * <p>
     * 这是一个可通用的基本旋转逻辑，它被提取成了一个独立的、可重用的方法。
     * 无论渲染的是填充多面体还是线框N边形，只要需要旋转，就可以调用此方法来提供变换逻辑。
     *
     * @param poseStack    要变换的 PoseStack。
     * @param center       旋转轨道的中心(相对物体的坐标)。
     * @param pivotOffset  相对于中心的旋转轴点。
     * @param rotation     旋转定义，包含旋转轴和角速度。
     * @param time         当前时间（通常是 tick 或 partial tick），用于计算旋转角度。
     */
    public static void applyRotatingTransform(PoseStack poseStack, Vec3 center, Vector3f pivotOffset, Rotation rotation, float time) {
        poseStack.translate(center.x, center.y, center.z);
        poseStack.translate(pivotOffset.x(), pivotOffset.y(), pivotOffset.z());
        float angleRad = rotation.angularVelocityRadPerTick * time;
        poseStack.mulPose(REUSABLE_QUATERNION.fromAxisAngleRad(rotation.normalizedAxis, angleRad));
        poseStack.translate(-pivotOffset.x(), -pivotOffset.y(), -pivotOffset.z());
    }

// --- 公开的渲染API (作为便捷封装) ---

// --- Platonic Solid (填充 vs 线框) ---

    /**
     * 渲染一个静态的、填充的正多面体，并应用真实光照。
     *
     * @param poseStack   PoseStack 实例。
     * @param buffer      顶点消费者。
     * @param level       用于获取光照的世界实例。
     * @param center      多面体的中心位置。
     * @param orientation 多面体的朝向法线。
     * @param visual      要渲染的正多面体定义。
     * @param color       多面体的颜色。
     */
    public static void renderStaticPlatonicSolid(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f orientation, PlatonicSolid visual, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyStaticTransform(ps, center, orientation),
                (buf, pose) -> buildPlatonicSolid(buf, pose, visual, color, packedLight)
        );
    }

    /**
     * 渲染一个旋转的、填充的正多面体，并应用真实光照。
     *
     * @param poseStack PoseStack 实例。
     * @param buffer    顶点消费者。
     * @param level     用于获取光照的世界实例。
     * @param center    旋转轨道的中心。
     * @param pivot     相对于中心的旋转轴点。
     * @param rot       旋转定义。
     * @param time      当前时间。
     * @param visual    要渲染的正多面体定义。
     * @param color     多面体的颜色。
     */
    public static void renderRotatingPlatonicSolid(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f pivot, Rotation rot, float time, PlatonicSolid visual, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyRotatingTransform(ps, center, pivot, rot, time),
                (buf, pose) -> buildPlatonicSolid(buf, pose, visual, color, packedLight)
        );
    }

    /**
     * 渲染一个静态的、由粗线构成的正多面体线框，并应用真实光照。
     *
     * @param poseStack   PoseStack 实例。
     * @param buffer      顶点消费者。
     * @param level       用于获取光照的世界实例。
     * @param center      多面体的中心位置。
     * @param orientation 多面体的朝向法线。
     * @param visual      要渲染的正多面体定义。
     * @param thickness   线框的粗细。
     * @param color       线框的颜色。
     * @param camera      用于计算线框朝向的摄像机实例。
     */
    public static void renderStaticThickPlatonicSolid(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f orientation, PlatonicSolid visual, float thickness, int color, Camera camera) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyStaticTransform(ps, center, orientation),
                (buf, pose) -> buildPlatonicSolidAsThickLines(buf, pose, visual, thickness, color, camera, packedLight)
        );
    }

    /**
     * 渲染一个旋转的、由粗线构成的正多面体线框，并应用真实光照。
     *
     * @param poseStack PoseStack 实例。
     * @param buffer    顶点消费者。
     * @param level     用于获取光照的世界实例。
     * @param center    旋转轨道的中心。
     * @param pivot     相对于中心的旋转轴点。
     * @param rot       旋转定义。
     * @param time      当前时间。
     * @param visual    要渲染的正多面体定义。
     * @param thickness 线框的粗细。
     * @param color     线框的颜色。
     * @param camera    用于计算线框朝向的摄像机实例。
     */
    public static void renderRotatingThickPlatonicSolid(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f pivot, Rotation rot, float time, PlatonicSolid visual, float thickness, int color, Camera camera) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyRotatingTransform(ps, center, pivot, rot, time),
                (buf, pose) -> buildPlatonicSolidAsThickLines(buf, pose, visual, thickness, color, camera, packedLight)
        );
    }

// --- Ngon (填充 vs 线框) ---

    /**
     * 渲染一个静态的、被填充的N边形，并应用真实光照。
     *
     * @param poseStack   PoseStack 实例。
     * @param buffer      顶点消费者。
     * @param level       用于获取光照的世界实例。
     * @param center      N边形的中心位置。
     * @param orientation N边形的朝向法线。
     * @param visual      要渲染的N边形定义。
     * @param color       N边形的颜色。
     */
    public static void renderStaticNgon(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f orientation, Ngon visual, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyStaticTransform(ps, center, orientation),
                (buf, pose) -> buildNgonAsTriangles(buf, pose, visual, color, packedLight)
        );
    }

    /**
     * 渲染一个旋转的、被填充的N边形，并应用真实光照。
     *
     * @param poseStack PoseStack 实例。
     * @param buffer    顶点消费者。
     * @param level     用于获取光照的世界实例。
     * @param center    旋转轨道的中心。
     * @param pivot     相对于中心的旋转轴点。
     * @param rot       旋转定义。
     * @param time      当前时间。
     * @param visual    要渲染的N边形定义。
     * @param color     N边形的颜色。
     */
    public static void renderRotatingNgon(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f pivot, Rotation rot, float time, Ngon visual, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyRotatingTransform(ps, center, pivot, rot, time),
                (buf, pose) -> buildNgonAsTriangles(buf, pose, visual, color, packedLight)
        );
    }

    /**
     * 渲染一个静态的、由粗线构成的N边形轮廓，并应用真实光照。
     *
     * @param poseStack   PoseStack 实例。
     * @param buffer      顶点消费者。
     * @param level       用于获取光照的世界实例。
     * @param center      N边形的中心位置。
     * @param orientation N边形的朝向法线。
     * @param visual      要渲染的N边形定义。
     * @param thickness   轮廓线的粗细。
     * @param color       轮廓线的颜色。
     */
    public static void renderStaticThickNgon(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f orientation, Ngon visual, float thickness, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyStaticTransform(ps, center, orientation),
                (buf, pose) -> buildNgonAsThickLine(buf, pose, visual, thickness, color, packedLight)
        );
    }

    /**
     * 渲染一个旋转的、由粗线构成的N边形轮廓，并应用真实光照。
     *
     * @param poseStack PoseStack 实例。
     * @param buffer    顶点消费者。
     * @param level     用于获取光照的世界实例。
     * @param center    旋转轨道的中心。
     * @param pivot     相对于中心的旋转轴点。
     * @param rot       旋转定义。
     * @param time      当前时间。
     * @param visual    要渲染的N边形定义。
     * @param thickness 轮廓线的粗细。
     * @param color     轮廓线的颜色。
     */
    public static void renderRotatingThickNgon(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f pivot, Rotation rot, float time, Ngon visual, float thickness, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyRotatingTransform(ps, center, pivot, rot, time),
                (buf, pose) -> buildNgonAsThickLine(buf, pose, visual, thickness, color, packedLight)
        );
    }

// --- 其他渲染方法继续遵循此模式 ---

    /**
     * 渲染一个静态的、填充的星形，并应用真实光照。
     *
     * @param poseStack   PoseStack 实例。
     * @param buffer      顶点消费者。
     * @param level       用于获取光照的世界实例。
     * @param center      星形的中心位置。
     * @param orientation 星形的朝向法线。
     * @param visual      要渲染的星形定义。
     * @param color       星形的颜色。
     */
    public static void renderStaticStar(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f orientation, Star visual, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyStaticTransform(ps, center, orientation),
                (buf, pose) -> buildStarVertices(buf, pose, visual, color, packedLight)
        );
    }

    /**
     * 渲染一个旋转的、填充的星形，并应用真实光照。
     *
     * @param poseStack PoseStack 实例。
     * @param buffer    顶点消费者。
     * @param level     用于获取光照的世界实例。
     * @param center    旋转轨道的中心。
     * @param pivot     相对于中心的旋转轴点。
     * @param rot       旋转定义。
     * @param time      当前时间。
     * @param visual    要渲染的星形定义。
     * @param color     星形的颜色。
     */
    public static void renderRotatingStar(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Vector3f pivot, Rotation rot, float time, Star visual, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> applyRotatingTransform(ps, center, pivot, rot, time),
                (buf, pose) -> buildStarVertices(buf, pose, visual, color, packedLight)
        );
    }

    /**
     * 渲染一个静态的、填充的球体（通过IcoSphere算法生成），并应用真实光照。
     *
     * @param poseStack PoseStack 实例。
     * @param buffer    顶点消费者。
     * @param level     用于获取光照的世界实例。
     * @param center    球体的中心位置。
     * @param visual    要渲染的球体定义。
     * @param color     球体的颜色。
     */
    public static void renderStaticSphere(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Sphere visual, int color) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> ps.translate(center.x, center.y, center.z),
                (buf, pose) -> buildIcosphere(buf, pose, visual, color, packedLight)
        );
    }

    /**
     * 渲染一个静态的、填充的球体（通过UV Sphere算法生成），并为每个顶点应用自定义颜色和真实光照。
     *
     * @param poseStack     PoseStack 实例。
     * @param buffer        顶点消费者。
     * @param level         用于获取光照的世界实例。
     * @param center        球体的中心位置。
     * @param visual        要渲染的球体定义。
     * @param colorProvider 一个函数，根据顶点在模型空间中的位置返回其颜色。
     */
    public static void renderStaticCustomSphere(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Sphere visual, Function<Vector3f, Integer> colorProvider) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        renderShape(poseStack, buffer,
                ps -> ps.translate(center.x, center.y, center.z),
                (buf, pose) -> buildUVSphere(buf, pose, visual, colorProvider, packedLight)
        );
    }

    /**
     * 渲染一个旋转的超立方体（或其他超对象）的3D投影，并应用真实光照。
     *
     * @param poseStack PoseStack 实例。
     * @param buffer    顶点消费者。
     * @param level     用于获取光照的世界实例。
     * @param center    超对象的中心位置。
     * @param hypercube 要渲染的超对象定义。
     * @param cNear     W坐标较小一端的颜色。
     * @param cFar      W坐标较大一端的颜色。
     * @param time      当前时间，用于驱动旋转。
     * @param thickness 投影后线条的粗细。
     * @param camera    用于计算线框朝向的摄像机实例。
     */
    public static void renderRotatingHyperObject(PoseStack poseStack, VertexConsumer buffer, Level level, Vec3 center, Hypercube hypercube,
                                                 int cNear, int cFar, float time, float thickness, Camera camera) {
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        Consumer<PoseStack> transformApplier = ps -> ps.translate(center.x, center.y, center.z);
        BiConsumer<VertexConsumer, PoseStack.Pose> vertexBuilder = (buf, pose) ->
                buildHyperObjectVertices(buf, pose, hypercube, cNear, cFar, time, thickness, camera, packedLight);
        renderShape(poseStack, buffer, transformApplier, vertexBuilder);
    }

/*

==========================================================================================

以下是其余未更改的定义和实现

(Interfaces, Records, Constants, Calculation Logic, Vertex Builders, Helpers etc.)

==========================================================================================
*/

// --- 接口与数据记录 ---
    /**
     * 表示一个可渲染形状的接口。
     */
    public interface RenderableShape {
        /**
         * 构建该形状的顶点数据。
         * @param buffer 顶点消费者。
         * @param pose   当前的变换姿态。
         * @param color  形状的颜色。
         */
        void buildVertices(VertexConsumer buffer, PoseStack.Pose pose, int color);

        /**
         * 获取该形状的视觉半径，可用于剔除等计算。
         * @return 视觉半径。
         */
        float getVisualRadius();
    }
    private static final float TWO_PI = (float) Math.TAU;
    private static final Vector3f DEFAULT_NORMAL = new Vector3f(0, 1, 0);
    public static final float MILLIS_PER_TICK = 50.0f;
    public static final Vector3f REUSABLE_VECTOR = new Vector3f();
    private static final Quaternionf REUSABLE_QUATERNION = new Quaternionf();
    /**
     * 定义一个星形。
     * @param points      星形的角数量。
     * @param radiusOuter 外顶点半径。
     * @param radiusInner 内顶点半径。
     */
    public record Star(int points, float radiusOuter, float radiusInner) {}
    /**
     * 定义一个正N边形。
     * @param sides  边数。
     * @param radius 半径（从中心到顶点的距离）。
     */
    public record Ngon(int sides, float radius) implements RenderableShape {
        /**
         * {@inheritDoc}
         */
        @Override public void buildVertices(VertexConsumer buffer, PoseStack.Pose pose, int color) { buildNgonAsTriangles(buffer, pose, this, color, 255); }
        /**
         * {@inheritDoc}
         */
        @Override public float getVisualRadius() { return this.radius; }
    }

    /**
     * 定义一个正多面体（柏拉图立体）或其星状变体。
     * @param type        正多面体的类型。
     * @param outerRadius 外接球半径，定义了形状的整体大小或星状尖端的距离。
     * @param innerRadius 内接球半径，用于定义星状多面体的核心顶点距离。对于凸多面体，此值通常被忽略或等于outerRadius。
     */
    public record PlatonicSolid(SolidType type, float outerRadius, float innerRadius) implements RenderableShape {
        /**
         * 正多面体及其星状变体的类型枚举。
         */
        public enum SolidType {
            TETRAHEDRON, CUBE, OCTAHEDRON, DODECAHEDRON, ICOSAHEDRON,
            // 添加了非凸（星状）类型
            STELLATED_DODECAHEDRON, // 星状十二面体
            STELLATED_ICOSAHEDRON   // 星状二十面体
        }

        /**
         * 为凸多面体或简单缩放提供的便捷构造函数。
         * @param type   正多面体的类型。
         * @param radius 半径，同时设为内外半径。
         */
        public PlatonicSolid(SolidType type, float radius) {
            this(type, radius, radius);
        }

        /**
         * {@inheritDoc}
         */
        @Override public void buildVertices(VertexConsumer buffer, PoseStack.Pose pose, int color) { buildPlatonicSolid(buffer, pose, this, color, 255); }
        /**
         * {@inheritDoc}
         */
        @Override public float getVisualRadius() { return this.outerRadius; }
    }

    /**
     * 定义一个球体。
     * @param subdivisions 细分等级。数值越高，球体越平滑。
     * @param radius       球体半径。
     */
    public record Sphere(int subdivisions, float radius) implements RenderableShape {
        /**
         * {@inheritDoc}
         */
        @Override public void buildVertices(VertexConsumer buffer, PoseStack.Pose pose, int color) { buildIcosphere(buffer, pose, this, color, 255); }
        /**
         * {@inheritDoc}
         */
        @Override public float getVisualRadius() { return this.radius; }
    }
    /**
     * 定义一个旋转。
     * @param normalizedAxis          标准化的旋转轴向量。
     * @param angularVelocityRadPerTick 每 tick 旋转的角速度（弧度）。
     */
    public record Rotation(Vector3f normalizedAxis, float angularVelocityRadPerTick) {
        /**
         * 创建一个 Rotation 实例。
         * @param axis                    旋转轴（将被标准化）。
         * @param angularVelocityRadPerTick 每 tick 旋转的角速度（弧度）。
         * @return 新的 Rotation 实例。
         */
        public static Rotation of(Vector3f axis, float angularVelocityRadPerTick) { return new Rotation(new Vector3f(axis).normalize(), angularVelocityRadPerTick); }
        /**
         * 根据每秒度数创建一个 Rotation 实例。
         * @param axis           旋转轴（将被标准化）。
         * @param degreesPerSecond 每秒旋转的角度。
         * @return 新的 Rotation 实例。
         */
        public static Rotation ofDegreesPerSecond(Vector3f axis, float degreesPerSecond) {
            float radPerTick = (float) Math.toRadians(degreesPerSecond) * (MILLIS_PER_TICK / 1000.0f);
            return new Rotation(new Vector3f(axis).normalize(), radPerTick);
        }
    }

// --- 计算逻辑 (非渲染) ---
    /**
     * 计算一个点围绕另一个中心点旋转后的位置。
     *
     * @param center      轨道中心点。
     * @param orbit       轨道的旋转定义。
     * @param orbitRadius 轨道半径。
     * @param time        当前时间。
     * @return 旋转后的新位置 {@link Vec3}。
     */
    public static Vec3 calculateRevolvingPosition(Vec3 center, Rotation orbit, float orbitRadius, float time) {
        float angleRad = orbit.angularVelocityRadPerTick() * time;
        REUSABLE_QUATERNION.fromAxisAngleRad(orbit.normalizedAxis(), angleRad);
        Vector3f initialOffset;
        if (Math.abs(orbit.normalizedAxis().dot(DEFAULT_NORMAL)) < 0.99f) {
            initialOffset = orbit.normalizedAxis().cross(DEFAULT_NORMAL, REUSABLE_VECTOR);
        } else {
            initialOffset = orbit.normalizedAxis().cross(1, 0, 0, REUSABLE_VECTOR);
        }
        initialOffset.normalize().mul(orbitRadius);
        REUSABLE_QUATERNION.transform(initialOffset);
        return new Vec3(center.x + initialOffset.x(), center.y + initialOffset.y(), center.z + initialOffset.z());
    }

// --- 顶点构建方法 ---
    /**
     * 构建一个填充的正多面体的顶点数据。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param visual 要构建的正多面体定义。
     * @param color  颜色。
     * @param packedLight 光照值。
     */
    private static void buildPlatonicSolid(VertexConsumer buffer, PoseStack.Pose pose, PlatonicSolid visual, int color, int packedLight) {
        // 常量定义：凸多面体的顶点和面
        final Vector3f[] TETRAHEDRON_VERTICES = { new Vector3f(1, 1, 1), new Vector3f(1, -1, -1), new Vector3f(-1, 1, -1), new Vector3f(-1, -1, 1) };
        final int[][] TETRAHEDRON_FACES = { {0, 2, 1}, {0, 1, 3}, {0, 3, 2}, {1, 2, 3} };
        final Vector3f[] CUBE_VERTICES = { new Vector3f(1, 1, 1), new Vector3f(1, -1, 1), new Vector3f(-1, -1, 1), new Vector3f(-1, 1, 1), new Vector3f(1, 1, -1), new Vector3f(1, -1, -1), new Vector3f(-1, -1, -1), new Vector3f(-1, 1, -1) };
        final int[][] CUBE_FACES = { {0, 1, 2, 3}, {4, 7, 6, 5}, {0, 3, 7, 4}, {1, 5, 6, 2}, {0, 4, 5, 1}, {3, 2, 6, 7} };
        final Vector3f[] OCTAHEDRON_VERTICES = { new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, -1, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1) };
        final int[][] OCTAHEDRON_FACES = { {0, 4, 2}, {0, 2, 5}, {0, 5, 3}, {0, 3, 4}, {1, 5, 2}, {1, 2, 4}, {1, 4, 3}, {1, 3, 5} };
        final float PHI = (1.0f + (float) Math.sqrt(5.0)) / 2.0f;
        final Vector3f[] ICOSAHEDRON_VERTICES = { new Vector3f(-1, PHI, 0), new Vector3f(1, PHI, 0), new Vector3f(-1, -PHI, 0), new Vector3f(1, -PHI, 0), new Vector3f(0, -1, PHI), new Vector3f(0, 1, PHI), new Vector3f(0, -1, -PHI), new Vector3f(0, 1, -PHI), new Vector3f(PHI, 0, -1), new Vector3f(PHI, 0, 1), new Vector3f(-PHI, 0, -1), new Vector3f(-PHI, 0, 1) };
        final int[][] ICOSAHEDRON_FACES = { {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1} };
        final float P = (1.0f + (float) Math.sqrt(5.0f)) / 2.0f; // PHI
        final float IP = 1.0f / P; // 1/PHI
        final Vector3f[] DODECAHEDRON_VERTICES = { new Vector3f( 1,  1,  1), new Vector3f( 1,  1, -1), new Vector3f( 1, -1,  1), new Vector3f( 1, -1, -1), new Vector3f(-1,  1,  1), new Vector3f(-1,  1, -1), new Vector3f(-1, -1,  1), new Vector3f(-1, -1, -1), new Vector3f(0,  P,  IP), new Vector3f(0,  P, -IP), new Vector3f(0, -P,  IP), new Vector3f(0, -P, -IP), new Vector3f( IP, 0,  P), new Vector3f(-IP, 0,  P), new Vector3f( IP, 0, -P), new Vector3f(-IP, 0, -P), new Vector3f( P,  IP, 0), new Vector3f(-P,  IP, 0), new Vector3f( P, -IP, 0), new Vector3f(-P, -IP, 0) };
        final int[][] DODECAHEDRON_FACES = { {0, 8, 4, 13, 12}, {0, 12, 2, 18, 16}, {0, 16, 1, 9, 8}, {1, 14, 3, 18, 16}, {1, 9, 5, 15, 14}, {2, 10, 6, 13, 12}, {2, 10, 11, 3, 18}, {3, 11, 7, 15, 14}, {4, 8, 9, 5, 17}, {4, 17, 19, 6, 13}, {5, 17, 19, 7, 15}, {6, 19, 7, 11, 10} };

        switch (visual.type()) {
            case STELLATED_DODECAHEDRON -> {
                buildStellatedSolid(buffer, pose, DODECAHEDRON_VERTICES, DODECAHEDRON_FACES, visual, color, packedLight);
                return;
            }
            case STELLATED_ICOSAHEDRON -> {
                buildStellatedSolid(buffer, pose, ICOSAHEDRON_VERTICES, ICOSAHEDRON_FACES, visual, color, packedLight);
                return;
            }
            // 默认处理所有凸多面体
            default -> {
                Vector3f[] vertices; int[][] faces;
                switch (visual.type()) {
                    case TETRAHEDRON    -> { vertices = TETRAHEDRON_VERTICES; faces = TETRAHEDRON_FACES; }
                    case CUBE           -> { vertices = CUBE_VERTICES; faces = CUBE_FACES; }
                    case OCTAHEDRON     -> { vertices = OCTAHEDRON_VERTICES; faces = OCTAHEDRON_FACES; }
                    case DODECAHEDRON   -> { vertices = DODECAHEDRON_VERTICES; faces = DODECAHEDRON_FACES; }
                    case ICOSAHEDRON    -> { vertices = ICOSAHEDRON_VERTICES; faces = ICOSAHEDRON_FACES; }
                    default             -> { return; } // 不应发生
                }
                // 为凸多面体构建顶点
                for (int[] faceIndices : faces) {
                    Vector3f v0_norm = vertices[faceIndices[0]].normalize(new Vector3f());
                    for (int i = 1; i < faceIndices.length - 1; i++) {
                        Vector3f v1_norm = vertices[faceIndices[i]].normalize(new Vector3f());
                        Vector3f v2_norm = vertices[faceIndices[i+1]].normalize(new Vector3f());
                        // 使用 outerRadius 进行缩放
                        addVertex(buffer, pose, new Vector3f(v0_norm).mul(visual.outerRadius()), color, v0_norm, packedLight);
                        addVertex(buffer, pose, new Vector3f(v1_norm).mul(visual.outerRadius()), color, v1_norm, packedLight);
                        addVertex(buffer, pose, new Vector3f(v2_norm).mul(visual.outerRadius()), color, v2_norm, packedLight);
                    }
                }
            }
        }
    }

    /**
     * 【新增】构建一个星状多面体的顶点数据。
     * @param buffer 顶点消费者。
     * @param pose 当前的变换姿态。
     * @param baseVertices 核心多面体的顶点定义。
     * @param baseFaces 核心多面体的面定义。
     * @param visual 形状定义，包含内外半径。
     * @param color 颜色。
     * @param packedLight 光照值。
     */
    public static void buildStellatedSolid(VertexConsumer buffer, PoseStack.Pose pose, Vector3f[] baseVertices, int[][] baseFaces, PlatonicSolid visual, int color, int packedLight) {
        // 1. 根据 innerRadius 创建核心多面体的顶点
        List<Vector3f> innerVertices = new ArrayList<>();
        for (Vector3f v : baseVertices) {
            innerVertices.add(new Vector3f(v).normalize().mul(visual.innerRadius()));
        }

        // 2. 遍历每个核心面，计算其尖端顶点，并构建金字塔面
        for (int[] faceIndices : baseFaces) {
            // 通过平均面顶点位置来计算面的法线（即尖端的方向）
            Vector3f faceNormal = new Vector3f();
            for (int index : faceIndices) {
                // 使用未标准化的基础顶点以获得更准确的中心
                faceNormal.add(baseVertices[index]);
            }
            faceNormal.normalize();

            // 尖端顶点位于 outerRadius 上
            Vector3f tipVertex = new Vector3f(faceNormal).mul(visual.outerRadius());

            // 3. 构建连接核心面边缘和尖端顶点的三角形，形成金字塔的侧面
            for (int i = 0; i < faceIndices.length; i++) {
                Vector3f p1 = innerVertices.get(faceIndices[i]);
                Vector3f p2 = innerVertices.get(faceIndices[(i + 1) % faceIndices.length]); // 回绕以闭合最后一个边缘

                // 计算金字塔侧面的法线以实现正确的光照
                Vector3f pyramidFaceNormal = new Vector3f(p2).sub(p1).cross(new Vector3f(tipVertex).sub(p1)).normalize();

                addVertex(buffer, pose, p1, color, pyramidFaceNormal, packedLight);
                addVertex(buffer, pose, p2, color, pyramidFaceNormal, packedLight);
                addVertex(buffer, pose, tipVertex, color, pyramidFaceNormal, packedLight);
            }
        }
    }


    /**
     * 构建一个由粗线构成的正多面体线框的顶点数据。
     * @param buffer    顶点消费者。
     * @param pose      当前的变换姿态。
     * @param visual    要构建的正多面体定义。
     * @param thickness 线的粗细。
     * @param color     颜色。
     * @param camera    摄像机实例，用于确定线的朝向。
     * @param packedLight 光照值。
     */
    private static void buildPlatonicSolidAsThickLines(VertexConsumer buffer, PoseStack.Pose pose, PlatonicSolid visual, float thickness, int color, Camera camera, int packedLight) {
        // 常量定义
        final float PHI = (1.0f + (float) Math.sqrt(5.0)) / 2.0f;
        final Vector3f[] TETRAHEDRON_VERTICES = { new Vector3f(1, 1, 1), new Vector3f(1, -1, -1), new Vector3f(-1, 1, -1), new Vector3f(-1, -1, 1) };
        final Vector3f[] CUBE_VERTICES = { new Vector3f(1, 1, 1), new Vector3f(1, -1, 1), new Vector3f(-1, -1, 1), new Vector3f(-1, 1, 1), new Vector3f(1, 1, -1), new Vector3f(1, -1, -1), new Vector3f(-1, -1, -1), new Vector3f(-1, 1, -1) };
        final Vector3f[] OCTAHEDRON_VERTICES = { new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, -1, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1) };
        final Vector3f[] ICOSAHEDRON_VERTICES = { new Vector3f(-1, PHI, 0), new Vector3f(1, PHI, 0), new Vector3f(-1, -PHI, 0), new Vector3f(1, -PHI, 0), new Vector3f(0, -1, PHI), new Vector3f(0, 1, PHI), new Vector3f(0, -1, -PHI), new Vector3f(0, 1, -PHI), new Vector3f(PHI, 0, -1), new Vector3f(PHI, 0, 1), new Vector3f(-PHI, 0, -1), new Vector3f(-PHI, 0, 1) };
        final float P = (1.0f + (float) Math.sqrt(5.0f)) / 2.0f; final float IP = 1.0f / P;
        final Vector3f[] DODECAHEDRON_VERTICES = { new Vector3f( 1,  1,  1), new Vector3f( 1,  1, -1), new Vector3f( 1, -1,  1), new Vector3f( 1, -1, -1), new Vector3f(-1,  1,  1), new Vector3f(-1,  1, -1), new Vector3f(-1, -1,  1), new Vector3f(-1, -1, -1), new Vector3f(0,  P,  IP), new Vector3f(0,  P, -IP), new Vector3f(0, -P,  IP), new Vector3f(0, -P, -IP), new Vector3f( IP, 0,  P), new Vector3f(-IP, 0,  P), new Vector3f( IP, 0, -P), new Vector3f(-IP, 0, -P), new Vector3f( P,  IP, 0), new Vector3f(-P,  IP, 0), new Vector3f( P, -IP, 0), new Vector3f(-P, -IP, 0) };
        final int[][] DODECAHEDRON_FACES = { {0, 8, 4, 13, 12}, {0, 12, 2, 18, 16}, {0, 16, 1, 9, 8}, {1, 14, 3, 18, 16}, {1, 9, 5, 15, 14}, {2, 10, 6, 13, 12}, {2, 10, 11, 3, 18}, {3, 11, 7, 15, 14}, {4, 8, 9, 5, 17}, {4, 17, 19, 6, 13}, {5, 17, 19, 7, 15}, {6, 19, 7, 11, 10} };
        final int[][] ICOSAHEDRON_FACES = { {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1} };

        // 优先处理新增的星状类型
        switch (visual.type()) {
            case STELLATED_DODECAHEDRON -> {
                buildStellatedSolidAsThickLines(buffer, pose, DODECAHEDRON_VERTICES, DODECAHEDRON_FACES, visual, thickness, color, camera, packedLight);
                return;
            }
            case STELLATED_ICOSAHEDRON -> {
                buildStellatedSolidAsThickLines(buffer, pose, ICOSAHEDRON_VERTICES, ICOSAHEDRON_FACES, visual, thickness, color, camera, packedLight);
                return;
            }
            default -> {} // 对于凸多面体，继续执行后续逻辑
        }

        // 凸多面体的边和顶点数据
        final int[][] TETRAHEDRON_EDGES = { {0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3} };
        final int[][] CUBE_EDGES = { {0,1},{1,2},{2,3},{3,0}, {4,5},{5,6},{6,7},{7,4}, {0,4},{1,5},{2,6},{3,7} };
        final int[][] OCTAHEDRON_EDGES = { {0,2},{0,3},{0,4},{0,5}, {1,2},{1,3},{1,4},{1,5}, {2,4},{2,5},{3,4},{3,5} };
        final int[][] ICOSAHEDRON_EDGES = { {0,1},{0,5},{0,7},{0,10},{0,11}, {1,5},{1,7},{1,8},{1,9}, {2,3},{2,4},{2,6},{2,10},{2,11}, {3,4},{3,6},{3,8},{3,9}, {4,9},{4,11},{4,5}, {5,9},{5,11}, {6,7},{6,8},{6,10}, {7,8},{7,10}, {8,9}, {10,11} };
        final int[][] DODECAHEDRON_EDGES = { {0,8},{0,12},{0,16}, {1,9},{1,14},{1,16}, {2,10},{2,12},{2,18}, {3,11},{3,14},{3,18}, {4,8},{4,13},{4,17}, {5,9},{5,15},{5,17}, {6,10},{6,13},{6,19}, {7,11},{7,15},{7,19}, {8,9}, {10,11}, {12,13}, {14,15}, {16,18}, {17,19} };

        Vector3f[] baseVertices; int[][] edges;
        switch (visual.type()) {
            case TETRAHEDRON -> { baseVertices = TETRAHEDRON_VERTICES; edges = TETRAHEDRON_EDGES; }
            case CUBE -> { baseVertices = CUBE_VERTICES; edges = CUBE_EDGES; }
            case OCTAHEDRON -> { baseVertices = OCTAHEDRON_VERTICES; edges = OCTAHEDRON_EDGES; }
            case DODECAHEDRON -> { baseVertices = DODECAHEDRON_VERTICES; edges = DODECAHEDRON_EDGES; }
            case ICOSAHEDRON -> { baseVertices = ICOSAHEDRON_VERTICES; edges = ICOSAHEDRON_EDGES; }
            default -> { return; }
        }

        List<Vector3f> vertices = new ArrayList<>(baseVertices.length);
        for (Vector3f v : baseVertices) {
            // 使用 outerRadius
            vertices.add(v.normalize(new Vector3f()).mul(visual.outerRadius()));
        }

        for (int[] edge : edges) {
            Vector3f p1 = vertices.get(edge[0]); Vector3f p2 = vertices.get(edge[1]);
            buildThickLine(buffer, pose, p1, p2, color, color, thickness, camera, packedLight);
        }
    }

    /**
     * 【新增】构建一个星状多面体的线框顶点数据。
     * @param buffer 顶点消费者。
     * @param pose 当前的变换姿态。
     * @param baseVertices 核心多面体的顶点定义。
     * @param baseFaces 核心多面体的面定义。
     * @param visual 形状定义，包含内外半径。
     * @param thickness 线的粗细。
     * @param color 颜色。
     * @param camera 摄像机实例。
     * @param packedLight 光照值。
     */
    private static void buildStellatedSolidAsThickLines(VertexConsumer buffer, PoseStack.Pose pose, Vector3f[] baseVertices, int[][] baseFaces, PlatonicSolid visual, float thickness, int color, Camera camera, int packedLight) {
        // 1. 根据 innerRadius 创建核心顶点
        List<Vector3f> innerVertices = new ArrayList<>();
        for (Vector3f v : baseVertices) {
            innerVertices.add(new Vector3f(v).normalize().mul(visual.innerRadius()));
        }

        // 2. 遍历每个面，计算尖端顶点并渲染连接线
        for (int[] faceIndices : baseFaces) {
            Vector3f faceNormal = new Vector3f();
            for (int index : faceIndices) {
                faceNormal.add(baseVertices[index]);
            }
            faceNormal.normalize();
            Vector3f tipVertex = new Vector3f(faceNormal).mul(visual.outerRadius());

            // 3. 渲染从核心顶点到尖端顶点的连线
            for (int index : faceIndices) {
                Vector3f innerVertex = innerVertices.get(index);
                buildThickLine(buffer, pose, innerVertex, tipVertex, color, color, thickness, camera, packedLight);
            }
        }
    }


    /**
     * 构建一个由三角形扇面组成的填充N边形的顶点数据。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param visual 要构建的N边形定义。
     * @param color  颜色。
     * @param packedLight 光照值。
     */
    public static void buildNgonAsTriangles(VertexConsumer buffer, PoseStack.Pose pose, Ngon visual, int color, int packedLight)  {
        if (visual.sides < 3) return;
        Vector3f transformedNormal = REUSABLE_VECTOR.set(DEFAULT_NORMAL).mul(pose.normal());
        float angleStep = TWO_PI / visual.sides; float x1 = visual.radius; float z1 = 0;
        for (int i = 1; i <= visual.sides; i++) {
            float nextAngle = i * angleStep;
            float x2 = (float) (visual.radius * Math.cos(nextAngle)); float z2 = (float) (visual.radius * Math.sin(nextAngle));
            addVertex(buffer, pose, 0, 0, 0, color, transformedNormal, packedLight);
            addVertex(buffer, pose, x1, 0, z1, color, transformedNormal, packedLight);
            addVertex(buffer, pose, x2, 0, z2, color, transformedNormal, packedLight);
            x1 = x2; z1 = z2;
        }
    }
    /**
     * 构建一个由粗线构成的N边形轮廓的顶点数据。
     * @param buffer    顶点消费者。
     * @param pose      当前的变换姿态。
     * @param visual    要构建的N边形定义。
     * @param thickness 线的粗细。
     * @param color     颜色。
     * @param packedLight 光照值。
     */
    public static void buildNgonAsThickLine(VertexConsumer buffer, PoseStack.Pose pose, Ngon visual, float thickness, int color, int packedLight){
        if (visual.sides < 3) return;
        List<Vector3f> baseVertices = new ArrayList<>(visual.sides);
        float angleStep = (float) (2.0 * Math.PI / visual.sides);
        for (int i = 0; i < visual.sides; i++) {
            float angle = i * angleStep;
            baseVertices.add(new Vector3f((float) (visual.radius * Math.cos(angle)), 0, (float) (visual.radius * Math.sin(angle))));
        }
        List<Vector3f> outerVertices = new ArrayList<>(visual.sides); List<Vector3f> innerVertices = new ArrayList<>(visual.sides);
        float halfThickness = thickness / 2.0f; Vector3f planeNormal = new Vector3f(0, 1, 0);
        for (int i = 0; i < visual.sides; i++) {
            Vector3f p_prev = baseVertices.get((i + visual.sides - 1) % visual.sides);
            Vector3f p_curr = baseVertices.get(i);
            Vector3f p_next = baseVertices.get((i + 1) % visual.sides);
            Vector3f dir1 = new Vector3f(p_curr).sub(p_prev).normalize(); Vector3f dir2 = new Vector3f(p_next).sub(p_curr).normalize();
            Vector3f normal1 = dir1.cross(planeNormal, new Vector3f()).normalize(); Vector3f normal2 = dir2.cross(planeNormal, new Vector3f()).normalize();
            Vector3f miterDir = new Vector3f(normal1).add(normal2).normalize();
            float miterLength = halfThickness / miterDir.dot(normal1);
            Vector3f offset = new Vector3f(miterDir).mul(miterLength);
            outerVertices.add(new Vector3f(p_curr).add(offset)); innerVertices.add(new Vector3f(p_curr).sub(offset));
        }
        Vector3f transformedNormal = new Vector3f(planeNormal);
        for (int i = 0; i < visual.sides; i++) {
            int next_i = (i + 1) % visual.sides;
            Vector3f v1 = outerVertices.get(i); Vector3f v2 = innerVertices.get(i);
            Vector3f v3 = innerVertices.get(next_i); Vector3f v4 = outerVertices.get(next_i);
            addVertex(buffer, pose, v1, color, transformedNormal, packedLight); addVertex(buffer, pose, v2, color, transformedNormal, packedLight); addVertex(buffer, pose, v3, color, transformedNormal, packedLight);
            addVertex(buffer, pose, v1, color, transformedNormal, packedLight); addVertex(buffer, pose, v3, color, transformedNormal, packedLight); addVertex(buffer, pose, v4, color, transformedNormal, packedLight);
        }
    }
    /**
     * 构建一个填充的星形的顶点数据。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param visual 要构建的星形定义。
     * @param color  颜色。
     * @param packedLight 光照值。
     */
    private static void buildStarVertices(VertexConsumer buffer, PoseStack.Pose pose, Star visual, int color, int packedLight) {
        if (visual.points < 2) return;
        int totalVertices = visual.points * 2; float angleStep = TWO_PI / totalVertices;
        Vector3f transformedNormal = REUSABLE_VECTOR.set(DEFAULT_NORMAL).mul(pose.normal());
        float x1 = visual.radiusOuter; float z1 = 0;
        for (int i = 1; i <= totalVertices; i++) {
            float radius2 = (i % 2 == 0) ? visual.radiusOuter : visual.radiusInner; float angle2 = i * angleStep;
            float x2 = (float) (radius2 * Math.cos(angle2)); float z2 = (float) (radius2 * Math.sin(angle2));
            addVertex(buffer, pose, 0, 0, 0, color, transformedNormal, packedLight);
            addVertex(buffer, pose, x1, 0, z1, color, transformedNormal, packedLight);
            addVertex(buffer, pose, x2, 0, z2, color, transformedNormal, packedLight);
            x1 = x2; z1 = z2;
        }
    }
    /**
     * 构建一个UV球体的顶点数据，每个顶点的颜色由一个函数提供。
     * @param buffer        顶点消费者。
     * @param pose          当前的变换姿态。
     * @param visual        要构建的球体定义。
     * @param colorProvider 根据顶点位置提供颜色的函数。
     * @param packedLight 光照值。
     */
    private static void buildUVSphere(VertexConsumer buffer, PoseStack.Pose pose, Sphere visual, Function<Vector3f, Integer> colorProvider, int packedLight) {
        int longitudeSegments = Math.max(4, visual.subdivisions() * 2); int latitudeSegments = Math.max(3, visual.subdivisions());
        for (int i = 0; i < latitudeSegments; i++) {
            float phi1 = (float) i / latitudeSegments * (float) Math.PI; float phi2 = (float) (i + 1) / latitudeSegments * (float) Math.PI;
            for (int j = 0; j < longitudeSegments; j++) {
                float theta1 = (float) j / longitudeSegments * TWO_PI; float theta2 = (float) (j + 1) / longitudeSegments * TWO_PI;
                Vector3f v1 = sphericalToCartesian(visual.radius, theta1, phi1); Vector3f v2 = sphericalToCartesian(visual.radius, theta2, phi1);
                Vector3f v3 = sphericalToCartesian(visual.radius, theta2, phi2); Vector3f v4 = sphericalToCartesian(visual.radius, theta1, phi2);
                addVertex(buffer, pose, v1, colorProvider.apply(v1), new Vector3f(v1).normalize(), packedLight); addVertex(buffer, pose, v2, colorProvider.apply(v2), new Vector3f(v2).normalize(), packedLight); addVertex(buffer, pose, v4, colorProvider.apply(v4), new Vector3f(v4).normalize(), packedLight);
                addVertex(buffer, pose, v2, colorProvider.apply(v2), new Vector3f(v2).normalize(), packedLight); addVertex(buffer, pose, v3, colorProvider.apply(v3), new Vector3f(v3).normalize(), packedLight); addVertex(buffer, pose, v4, colorProvider.apply(v4), new Vector3f(v4).normalize(), packedLight);
            }
        }
    }
    /**
     * 构建一个IcoSphere（通过细分正二十面体生成的球体）的顶点数据。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param visual 要构建的球体定义。
     * @param color  颜色。
     * @param packedLight 光照值。
     */
    public static void buildIcosphere(VertexConsumer buffer, PoseStack.Pose pose, Sphere visual, int color, int packedLight){
        Map<Long, Integer> middlePointIndexCache = new HashMap<>(); List<Vector3f> vertices = new ArrayList<>();
        float t = (1.0f + (float) Math.sqrt(5.0)) / 2.0f;
        vertices.add(new Vector3f(-1, t, 0).normalize()); vertices.add(new Vector3f(1, t, 0).normalize()); vertices.add(new Vector3f(-1, -t, 0).normalize()); vertices.add(new Vector3f(1, -t, 0).normalize()); vertices.add(new Vector3f(0, -1, t).normalize()); vertices.add(new Vector3f(0, 1, t).normalize()); vertices.add(new Vector3f(0, -1, -t).normalize()); vertices.add(new Vector3f(0, 1, -t).normalize()); vertices.add(new Vector3f(t, 0, -1).normalize()); vertices.add(new Vector3f(t, 0, 1).normalize()); vertices.add(new Vector3f(-t, 0, -1).normalize()); vertices.add(new Vector3f(-t, 0, 1).normalize());
        List<Vector3i> faces = new ArrayList<>(List.of(new Vector3i(0, 11, 5), new Vector3i(0, 5, 1), new Vector3i(0, 1, 7), new Vector3i(0, 7, 10), new Vector3i(0, 10, 11), new Vector3i(1, 5, 9), new Vector3i(5, 11, 4), new Vector3i(11, 10, 2), new Vector3i(10, 7, 6), new Vector3i(7, 1, 8), new Vector3i(3, 9, 4), new Vector3i(3, 4, 2), new Vector3i(3, 2, 6), new Vector3i(3, 6, 8), new Vector3i(3, 8, 9), new Vector3i(4, 9, 5), new Vector3i(2, 4, 11), new Vector3i(6, 2, 10), new Vector3i(8, 6, 7), new Vector3i(9, 8, 1)));
        for (int i = 0; i < visual.subdivisions; i++) {
            List<Vector3i> faces2 = new ArrayList<>();
            for (Vector3i tri : faces) {
                int a = getMiddlePoint(tri.x, tri.y, vertices, middlePointIndexCache); int b = getMiddlePoint(tri.y, tri.z, vertices, middlePointIndexCache); int c = getMiddlePoint(tri.z, tri.x, vertices, middlePointIndexCache);
                faces2.add(new Vector3i(tri.x, a, c)); faces2.add(new Vector3i(tri.y, b, a)); faces2.add(new Vector3i(tri.z, c, b)); faces2.add(new Vector3i(a, b, c));
            }
            faces = faces2;
        }
        for (Vector3i tri : faces) {
            Vector3f v1 = new Vector3f(vertices.get(tri.x)).mul(visual.radius); Vector3f v2 = new Vector3f(vertices.get(tri.y)).mul(visual.radius); Vector3f v3 = new Vector3f(vertices.get(tri.z)).mul(visual.radius);
            addVertex(buffer, pose, v1, color, vertices.get(tri.x), packedLight); addVertex(buffer, pose, v2, color, vertices.get(tri.y), packedLight); addVertex(buffer, pose, v3, color, vertices.get(tri.z), packedLight);
        }
    }
    /**
     * 构建一个三角形的顶点数据。
     * @param buffer   顶点消费者。
     * @param pose     当前的变换姿态。
     * @param triangle 要构建的三角形定义。
     * @param color    颜色。
     * @param packedLight 光照值。
     */
    public static void buildTriangle(VertexConsumer buffer, PoseStack.Pose pose, FractalGenerator.Triangle3D triangle, int color, int packedLight) {
        Vector3f normal = new Vector3f(triangle.v2()).sub(triangle.v1()).cross(new Vector3f(triangle.v3()).sub(triangle.v1())).normalize();
        addVertex(buffer, pose, triangle.v1(), color, normal, packedLight); addVertex(buffer, pose, triangle.v2(), color, normal, packedLight); addVertex(buffer, pose, triangle.v3(), color, normal, packedLight);
    }
    /**
     * 构建一个立方体的顶点数据。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param cube   要构建的立方体定义。
     * @param color  颜色。
     * @param packedLight 光照值。
     */
    public static void buildCube(VertexConsumer buffer, PoseStack.Pose pose, FractalGenerator.Cube cube, int color, int packedLight) {
        float halfSize = cube.size() / 2.0f; Vector3f center = cube.center();
        Vector3f v000 = new Vector3f(center.x - halfSize, center.y - halfSize, center.z - halfSize); Vector3f v001 = new Vector3f(center.x - halfSize, center.y - halfSize, center.z + halfSize); Vector3f v010 = new Vector3f(center.x - halfSize, center.y + halfSize, center.z - halfSize); Vector3f v011 = new Vector3f(center.x - halfSize, center.y + halfSize, center.z + halfSize); Vector3f v100 = new Vector3f(center.x + halfSize, center.y - halfSize, center.z - halfSize); Vector3f v101 = new Vector3f(center.x + halfSize, center.y - halfSize, center.z + halfSize); Vector3f v110 = new Vector3f(center.x + halfSize, center.y + halfSize, center.z - halfSize); Vector3f v111 = new Vector3f(center.x + halfSize, center.y + halfSize, center.z + halfSize);
        buildFace(buffer, pose, color, new Vector3f(0, 1, 0), v011, v010, v110, v111, packedLight); buildFace(buffer, pose, color, new Vector3f(0, -1, 0), v000, v001, v101, v100, packedLight);
        buildFace(buffer, pose, color, new Vector3f(1, 0, 0), v101, v111, v110, v100, packedLight); buildFace(buffer, pose, color, new Vector3f(-1, 0, 0), v000, v010, v011, v001, packedLight);
        buildFace(buffer, pose, color, new Vector3f(0, 0, 1), v001, v011, v111, v101, packedLight); buildFace(buffer, pose, color, new Vector3f(0, 0, -1), v100, v110, v010, v000, packedLight);
    }
    /**
     * 构建一个超对象（如超立方体）的3D投影线框的顶点数据。
     * @param buffer     顶点消费者。
     * @param pose       当前的变换姿态。
     * @param hypercube  要构建的超对象定义。
     * @param colorNear  W坐标较小一端的颜色。
     * @param colorFar   W坐标较大一端的颜色。
     * @param time       当前时间，用于驱动旋转。
     * @param thickness  线的粗细。
     * @param camera     摄像机实例。
     * @param packedLight 光照值。
     */
    private static void buildHyperObjectVertices(VertexConsumer buffer, PoseStack.Pose pose, Hypercube hypercube, int colorNear, int colorFar, float time, float thickness, Camera camera, int packedLight) {
        final int D = hypercube.dimension();
        MatrixN rotationMatrix = MatrixN.createRotationMatrix(D, 3, 4, time * 0.02f).multiply(MatrixN.createRotationMatrix(D, 1, 2, time * 0.035f)).multiply(MatrixN.createRotationMatrix(D, 0, 3, time * 0.015f));
        Vector3f[] projectedPoints3D = new Vector3f[hypercube.vertices().length]; int[] vertexColors = new int[hypercube.vertices().length];
        final float PROJECTION_DISTANCE = 3.0f;
        for (int i = 0; i < hypercube.vertices().length; i++) {
            VectorN rotatedPoint = hypercube.vertices()[i].transform(rotationMatrix);
            VectorN currentPoint = rotatedPoint; VectorN point4D = null;
            for (int d = D - 1; d >= 3; d--) {
                currentPoint = currentPoint.project(d, PROJECTION_DISTANCE);
                if (d == 4) point4D = currentPoint;
            }
            VectorN projectedPoint3D_N = currentPoint;
            projectedPoints3D[i] = new Vector3f(projectedPoint3D_N.get(0), projectedPoint3D_N.get(1), projectedPoint3D_N.get(2));
            float wCoord = (point4D != null) ? point4D.get(3) : 0;
//            vertexColors[i] = mapWToColor(wCoord, hypercube.size(), colorNear, colorFar);
        }
        for (int[] edge : hypercube.edges()) {
            buildThickLine(buffer, pose, projectedPoints3D[edge[0]], projectedPoints3D[edge[1]], vertexColors[edge[0]], vertexColors[edge[1]], thickness, camera, packedLight);
        }
    }

// --- 辅助方法 ---
    /**
     * 获取两点之间的中点，用于IcoSphere细分。如果中点已存在，则从缓存中获取。
     * @param p1       第一个点的索引。
     * @param p2       第二个点的索引。
     * @param vertices 顶点列表。
     * @param cache    中点索引缓存。
     * @return 中点的索引。
     */
    private static int getMiddlePoint(int p1, int p2, List<Vector3f> vertices, Map<Long, Integer> cache) {
        long smallerIndex = Math.min(p1, p2); long greaterIndex = Math.max(p1, p2);
        long key = (smallerIndex << 32) + greaterIndex;
        return cache.computeIfAbsent(key, k -> {
            Vector3f point1 = vertices.get(p1); Vector3f point2 = vertices.get(p2);
            Vector3f middle = new Vector3f((point1.x + point2.x) / 2.0f, (point1.y + point2.y) / 2.0f, (point1.z + point2.z) / 2.0f).normalize();
            vertices.add(middle); return vertices.size() - 1;
        });
    }
    /**
     * 将球坐标转换为笛卡尔坐标。
     * @param r     半径。
     * @param theta 水平角（经度）。
     * @param phi   垂直角（纬度）。
     * @return 对应的笛卡尔坐标 {@link Vector3f}。
     */
    private static Vector3f sphericalToCartesian(float r, float theta, float phi) {
        float sinPhi = (float) Math.sin(phi);
        return new Vector3f(r * sinPhi * (float) Math.cos(theta), r * (float) Math.cos(phi), r * sinPhi * (float) Math.sin(theta));
    }
    /**
     * 向顶点消费者添加一个顶点。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param pos    顶点位置。
     * @param color  顶点颜色。
     * @param normal 顶点法线。
     * @param packedLight 光照值。
     */
    private static void addVertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pos, int color, Vector3f normal, int packedLight) {
        addVertex(buffer, pose, pos.x, pos.y, pos.z, color, normal, packedLight);
    }
    /**
     * 向顶点消费者添加一个顶点。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param x      顶点x坐标。
     * @param y      顶点y坐标。
     * @param z      顶点z坐标。
     * @param color  顶点颜色。
     * @param normal 顶点法线。
     * @param packedLight 光照值。
     */
    private static void addVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int color, Vector3f normal, int packedLight) {
        buffer.addVertex(pose.pose(), x, y, z)
                .setColor(color)
                .setLight(packedLight)
                .setNormal(pose, normal.x(), normal.y(), normal.z()); }
    /**
     * 构建一条简单的线段（两个顶点）。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param p1     起点。
     * @param p2     终点。
     * @param color  颜色。
     * @param packedLight 光照值。
     */
    private static void buildLine(VertexConsumer buffer, PoseStack.Pose pose, Vector3f p1, Vector3f p2, int color, int packedLight) {
        Vector3f dummyNormal = REUSABLE_VECTOR.set(0, 1, 0).mul(pose.normal());
        addVertex(buffer, pose, p1, color, dummyNormal, packedLight); addVertex(buffer, pose, p2, color, dummyNormal, packedLight);
    }
    /**
     * 构建一个由两个三角形组成的四边形面。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param color  颜色。
     * @param normal 面的法线。
     * @param v1     顶点1。
     * @param v2     顶点2。
     * @param v3     顶点3。
     * @param v4     顶点4。
     * @param packedLight 光照值。
     */
    static void buildFace(VertexConsumer buffer, PoseStack.Pose pose, int color, Vector3f normal, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, int packedLight) {
        addVertex(buffer, pose, v1, color, normal, packedLight); addVertex(buffer, pose, v2, color, normal, packedLight); addVertex(buffer, pose, v3, color, normal, packedLight);
        addVertex(buffer, pose, v1, color, normal, packedLight); addVertex(buffer, pose, v3, color, normal, packedLight); addVertex(buffer, pose, v4, color, normal, packedLight);
    }
    /**
     * 构建一条面向摄像机的粗线（一个矩形）。
     * @param buffer    顶点消费者。
     * @param pose      当前的变换姿态。
     * @param p1        起点。
     * @param p2        终点。
     * @param color1    起点颜色。
     * @param color2    终点颜色。
     * @param thickness 线的粗细。
     * @param camera    摄像机实例。
     * @param packedLight 光照值。
     */
    public static void buildThickLine(VertexConsumer buffer, PoseStack.Pose pose, Vector3f p1, Vector3f p2, int color1, int color2, float thickness, Camera camera, int packedLight) {
        Vector3f lineDirection = new Vector3f(p2).sub(p1);
        // 如果线段长度过短，则不渲染，避免计算错误
        if (lineDirection.lengthSquared() < 1.0E-6) return;
        lineDirection.normalize();

        // --- 修正后的稳健广告牌逻辑 ---
        // 直接从Camera获取稳定的“上”和“右”向量 (它们已经是JOML的Vector3f)
        Vector3f camUp = camera.getUpVector();
        Vector3f camRight = camera.getLeftVector().mul(-1.0f); // getLeftVector是左方向，取反得到右方向

        // 通过叉乘计算出垂直于线段和镜头“上”方向的向量，作为光束的“宽度”方向
        Vector3f offsetDirection = new Vector3f(lineDirection).cross(camUp, new Vector3f()).normalize();

        // 极端情况：如果线段方向恰好与镜头“上”方向平行（例如垂直向上/下射箭），叉乘结果会是零向量
        // 此时，改用镜头“右”方向作为备用计算基准
        if (offsetDirection.lengthSquared() < 0.001) {
            offsetDirection = new Vector3f(lineDirection).cross(camRight, new Vector3f()).normalize();
        }
        // --- 修正结束 ---

        Vector3f offset = offsetDirection.mul(thickness / 2.0f);
        Vector3f v1 = new Vector3f(p1).add(offset);
        Vector3f v2 = new Vector3f(p1).sub(offset);
        Vector3f v3 = new Vector3f(p2).sub(offset);
        Vector3f v4 = new Vector3f(p2).add(offset);

        // 法线用于光照，使用offsetDirection是合理的
        Vector3f normal = offsetDirection;
        buildFace(buffer, pose, color1, color2, normal, v1, v2, v3, v4, packedLight);
    }
    /**
     * 构建一个由两个三角形组成的、支持颜色渐变的四边形面。
     * @param buffer 顶点消费者。
     * @param pose   当前的变换姿态。
     * @param c1     顶点v1和v2的颜色。
     * @param c2     顶点v3和v4的颜色。
     * @param normal 面的法线。
     * @param v1     顶点1。
     * @param v2     顶点2。
     * @param v3     顶点3。
     * @param v4     顶点4。
     * @param packedLight 光照值。
     */
    private static void buildFace(VertexConsumer buffer, PoseStack.Pose pose, int c1, int c2, Vector3f normal, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, int packedLight) {
        addVertex(buffer, pose, v1, c1, normal, packedLight); addVertex(buffer, pose, v2, c1, normal, packedLight); addVertex(buffer, pose, v3, c2, normal, packedLight);
        addVertex(buffer, pose, v1, c1, normal, packedLight); addVertex(buffer, pose, v3, c2, normal, packedLight); addVertex(buffer, pose, v4, c2, normal, packedLight);
    }

    // 假设 Hypercube, FractalGenerator, MatrixN, VectorN 等类已在别处定义
    public static final class FractalGenerator {
        public record Triangle3D(Vector3f v1, Vector3f v2, Vector3f v3) {}
        public record Cube(Vector3f center, float size) {}
    }
    public static final class Hypercube {
        public int dimension() { return 0; }
        public VectorN[] vertices() { return new VectorN[0]; }
        public int[][] edges() { return new int[0][]; }
        public float size() { return 0f; }
    }
    public static class MatrixN {
        public static MatrixN createRotationMatrix(int d, int a, int b, float t) { return new MatrixN(); }
        public MatrixN multiply(MatrixN other) { return this; }
    }
    public static class VectorN {
        public VectorN transform(MatrixN matrix) { return this; }
        public VectorN project(int d, float dist) { return this; }
        public float get(int i) { return 0f; }
    }
}