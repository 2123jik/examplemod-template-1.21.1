package com.example.examplemod.util.refactored.shape;

import com.example.examplemod.util.refactored.IShape;
import com.example.examplemod.util.refactored.RenderHelper;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * 代表一个柏拉图立体形状。
 */
public class PlatonicSolid implements IShape {

    private final PlatonicSolidData data;
    private final float radius;

    public PlatonicSolid(PlatonicSolidData data, float radius) {
        this.data = data;
        this.radius = radius;
    }

    @Override
    public void buildFilled(RenderContext context) {
        // 预处理顶点，将其缩放到指定半径
        List<Vector3f> scaledVertices = new ArrayList<>();
        for (Vector3f v : data.vertices) {
            scaledVertices.add(new Vector3f(v).normalize().mul(radius));
        }

        // 根据面的定义构建三角形
        for (int[] faceIndices : data.faces) {
            // 三角剖分 (Tessellation)
            for (int i = 1; i < faceIndices.length - 1; i++) {
                Vector3f v0 = scaledVertices.get(faceIndices[0]);
                Vector3f v1 = scaledVertices.get(faceIndices[i]);
                Vector3f v2 = scaledVertices.get(faceIndices[i + 1]);

                Vector3f normal = new Vector3f(v1).sub(v0).cross(new Vector3f(v2).sub(v0)).normalize();

                RenderHelper.addVertex(context, v0, normal);
                RenderHelper.addVertex(context, v1, normal);
                RenderHelper.addVertex(context, v2, normal);
            }
        }
    }

    @Override
    public void buildWireframe(RenderContext context, float thickness) {
        List<Vector3f> scaledVertices = new ArrayList<>();
        for (Vector3f v : data.vertices) {
            scaledVertices.add(new Vector3f(v).normalize().mul(radius));
        }
        
        // 根据边的定义构建粗线
        for (int[] edge : data.edges) {
            Vector3f p1 = scaledVertices.get(edge[0]);
            Vector3f p2 = scaledVertices.get(edge[1]);
            RenderHelper.buildThickLine(context, p1, p2, thickness);
        }
    }
}