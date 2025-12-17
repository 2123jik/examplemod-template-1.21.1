package com.example.examplemod.bridge;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// 【重点】这里千万不要 extends VertexConsumer，否则就是死循环！
public interface LegacyVertexConsumer {

    // --- 适配旧版 vertex ---
    default LegacyVertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        // 运行时 this 绝对是 VertexConsumer，放心强转
        ((VertexConsumer) this).addVertex(matrix, x, y, z);
        return this;
    }

    // --- 适配旧版 color ---
    default LegacyVertexConsumer color(int r, int g, int b, int a) {
        ((VertexConsumer) this).setColor(r, g, b, a);
        return this;
    }

    // --- 适配旧版 uv ---
    default LegacyVertexConsumer uv(float u, float v) {
        ((VertexConsumer) this).setUv(u, v);
        return this;
    }

    // --- 适配旧版 overlayCoords ---
    default LegacyVertexConsumer overlayCoords(int overlay) {
        ((VertexConsumer) this).setOverlay(overlay);
        return this;
    }

    // --- 适配旧版 uv2 ---
    default LegacyVertexConsumer uv2(int light) {
        ((VertexConsumer) this).setLight(light);
        return this;
    }

    // --- 适配旧版 normal ---
    default LegacyVertexConsumer normal(Matrix3f normalMatrix, float x, float y, float z) {
        float tx = normalMatrix.m00() * x + normalMatrix.m10() * y + normalMatrix.m20() * z;
        float ty = normalMatrix.m01() * x + normalMatrix.m11() * y + normalMatrix.m21() * z;
        float tz = normalMatrix.m02() * x + normalMatrix.m12() * y + normalMatrix.m22() * z;
        ((VertexConsumer) this).setNormal(tx, ty, tz);
        return this;
    }

    // --- 适配旧版 endVertex ---
    default void endVertex() {
        // no-op
    }
}