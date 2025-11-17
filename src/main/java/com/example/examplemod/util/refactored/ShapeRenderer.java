package com.example.examplemod.util.refactored;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 提供一个流畅的API来渲染3D形状。
 * 这是与外部代码交互的主要入口点。
 */
public class ShapeRenderer {

    // --- API参数字段 ---
    private final IShape shape;
    private final PoseStack poseStack;
    private final VertexConsumer buffer;
    private final Level level;
    private final Camera camera;

    private Vec3 center = Vec3.ZERO;
    private Vector3f orientation = new Vector3f(0, 1, 0); // 默认朝上
    private Rotation rotation = null;
    private float time = 0;
    private Color4f color = new Color4f(1, 1, 1, 1);

    // --- 构造函数 ---
    private ShapeRenderer(IShape shape, PoseStack poseStack, VertexConsumer buffer, Level level, Camera camera) {
        this.shape = shape;
        this.poseStack = poseStack;
        this.buffer = buffer;
        this.level = level;
        this.camera = camera;
    }

    /**
     * 开始一个形状渲染流程。
     *
     * @return 一个新的 ShapeRenderer 实例用于链式调用。
     */
    public static ShapeRenderer begin(IShape shape, PoseStack poseStack, VertexConsumer buffer, Level level, Camera camera) {
        return new ShapeRenderer(shape, poseStack, buffer, level, camera);
    }

    // --- 链式配置方法 ---
    public ShapeRenderer at(Vec3 center) {
        this.center = center;
        return this;
    }

    public ShapeRenderer oriented(Vector3f orientation) {
        this.orientation = orientation;
        return this;
    }

    public ShapeRenderer rotating(Rotation rotation, float time) {
        this.rotation = rotation;
        this.time = time;
        return this;
    }

    public ShapeRenderer colored(Color4f color) {
        this.color = color;
        // 注意：实际应用中，颜色应传递给 RenderHelper
        return this;
    }

    // --- 最终执行方法 ---

    /**
     * 最终执行渲染，将形状渲染为填充实体。
     */
    public void drawFilled() {
        draw((context) -> shape.buildFilled(context));
    }

    /**
     * 最终执行渲染，将形状渲染为线框。
     * @param thickness 线的粗细。
     */
    public void drawWireframe(float thickness) {
        draw((context) -> shape.buildWireframe(context, thickness));
    }

    // --- 私有核心渲染逻辑 ---
    private void draw(java.util.function.Consumer<IShape.RenderContext> buildAction) {
        poseStack.pushPose();

        // 1. 应用变换
        applyTransformations();

        // 2. 准备渲染上下文
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(center));
        IShape.RenderContext context = new IShape.RenderContext(buffer, poseStack.last(), camera, packedLight);

        // 3. 调用具体形状的构建逻辑
        buildAction.accept(context);

        poseStack.popPose();
    }
    
    private void applyTransformations() {
        // 平移到中心点
        poseStack.translate(center.x, center.y, center.z);

        // 应用旋转（如果定义了）
        if (rotation != null) {
            float angleRad = rotation.angularVelocityRadPerTick() * time;
            poseStack.mulPose(new Quaternionf().fromAxisAngleRad(rotation.normalizedAxis(), angleRad));
        }
        
        // 应用静态朝向
        Vector3f defaultNormal = new Vector3f(0, 1, 0);
        Vector3f targetNormal = new Vector3f(orientation).normalize();
        poseStack.mulPose(new Quaternionf().rotationTo(defaultNormal, targetNormal));
    }

    // --- 辅助数据记录 ---
    public record Rotation(Vector3f normalizedAxis, float angularVelocityRadPerTick) {}
    public record Color4f(float r, float g, float b, float a) {}
}