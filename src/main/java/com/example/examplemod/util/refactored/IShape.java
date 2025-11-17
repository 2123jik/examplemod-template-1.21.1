package com.example.examplemod.util.refactored;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;

/**
 * 表示一个可渲染的3D形状的接口。
 * 封装了特定形状的顶点构建逻辑。
 */
public interface IShape {

    /**
     * 构建该形状的填充顶点。
     * @param context 渲染上下文，包含所有必需的渲染信息。
     */
    void buildFilled(RenderContext context);

    /**
     * 构建该形状的线框顶点。
     * @param context 渲染上下文，包含所有必需的渲染信息。
     * @param thickness 线的粗细。
     */
    void buildWireframe(RenderContext context, float thickness);

    /**
     * 渲染上下文记录，用于将渲染参数捆绑在一起传递。
     */
    record RenderContext(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            Camera camera,
            int packedLight
    ) {}
}