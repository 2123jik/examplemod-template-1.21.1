package com.example.examplemod.util.refactored;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import org.joml.Vector3f;

/**
 * 一个静态工具类，提供用于构建顶点和几何图元的底层辅助方法。
 * 这些方法被各种 IShape 实现所共享。
 */
public final class RenderHelper {

    // 私有构造函数，防止实例化
    private RenderHelper() {}

    /**
     * 向顶点消费者添加一个顶点。这是最核心的顶点构建方法。
     * 
     * @param context 包含 buffer 和 pose 的渲染上下文。
     * @param pos     顶点位置。
     * @param color   顶点颜色。
     * @param normal  顶点法线。
     */
    public static void addVertex(IShape.RenderContext context, Vector3f pos, ShapeRenderer.Color4f color, Vector3f normal) {
        VertexConsumer buffer = context.buffer();
        PoseStack.Pose pose = context.pose();
        int packedLight = context.packedLight();

        buffer.addVertex(pose.pose(), pos.x, pos.y, pos.z)
                .setColor(color.r(), color.g(), color.b(), color.a())
                .setLight(packedLight)
                .setNormal(pose, normal.x(), normal.y(), normal.z());
    }

    /**
     * addVertex 的重载版本，方便直接传递颜色。
     * 注意：在实际应用中，颜色最好也通过 context 传递，以支持渐变等效果。
     * 这里为了简化，我们假设整个形状是单色的。
     */
    public static void addVertex(IShape.RenderContext context, Vector3f pos, Vector3f normal) {
        // 暂时硬编码一个白色，实际项目中应从context或参数中获取
        addVertex(context, pos, new ShapeRenderer.Color4f(1, 1, 1, 1), normal);
    }


    /**
     * 构建一个由两个三角形组成的、面向摄像机的粗线（一个矩形）。
     *
     * @param context   渲染上下文。
     * @param p1        起点。
     * @param p2        终点。
     * @param thickness 线的粗细。
     */
    public static void buildThickLine(IShape.RenderContext context, Vector3f p1, Vector3f p2, float thickness) {
        Camera camera = context.camera();
        Vector3f lineDirection = new Vector3f(p2).sub(p1);
        if (lineDirection.lengthSquared() < 1.0E-6) return;
        lineDirection.normalize();
        
        // 使用Camera的向量来确保线段始终面向玩家
        Vector3f camUp = new Vector3f(camera.getUpVector());
        Vector3f offsetDirection = new Vector3f(lineDirection).cross(camUp).normalize();

        // 处理线段与相机“up”向量平行或接近平行的极端情况
        if (offsetDirection.lengthSquared() < 0.001) {
            Vector3f camRight = new Vector3f(camera.getLeftVector()).mul(-1.0f);
            offsetDirection = new Vector3f(lineDirection).cross(camRight).normalize();
        }

        Vector3f offset = offsetDirection.mul(thickness / 2.0f);
        Vector3f v1 = new Vector3f(p1).add(offset);
        Vector3f v2 = new Vector3f(p1).sub(offset);
        Vector3f v3 = new Vector3f(p2).sub(offset);
        Vector3f v4 = new Vector3f(p2).add(offset);

        // 法线用于光照，对于广告牌式的线段，直接使用偏移方向是合理的
        Vector3f normal = offsetDirection;

        // 构建两个三角形组成的面
        // 注意：为了简单，这里用了单色。如果需要渐变，需要修改 buildFace 方法
        ShapeRenderer.Color4f color = new ShapeRenderer.Color4f(1, 1, 1, 1);
        buildFace(context, color, color, normal, v1, v2, v3, v4);
    }

    /**
     * 构建一个由两个三角形组成的四边形面，支持颜色渐变。
     *
     * @param context 渲染上下文。
     * @param c1      顶点v1和v2的颜色。
     * @param c2      顶点v3和v4的颜色。
     * @param normal  面的法线。
     * @param v1      顶点1。
     * @param v2      顶点2。
     * @param v3      顶点3。
     * @param v4      顶点4。
     */
    public static void buildFace(IShape.RenderContext context, ShapeRenderer.Color4f c1, ShapeRenderer.Color4f c2, Vector3f normal, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4) {
        addVertex(context, v1, c1, normal);
        addVertex(context, v2, c1, normal);
        addVertex(context, v3, c2, normal);

        addVertex(context, v1, c1, normal);
        addVertex(context, v3, c2, normal);
        addVertex(context, v4, c2, normal);
    }
}