package com.example.examplemod.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

public class BlackHoleParticle extends TextureSheetParticle {

    // 【核心修复1】将渲染模式改为 QUADS，防止顶点被错误地连成条带
    public static final ParticleRenderType BLACK_HOLE_RENDER = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorLightmapShader);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull(); // 同样双面渲染，保证从圆心内部看也能看到

            // 这里必须是 QUADS，因为我们在下面只提供4个顶点
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP);
        }
        @Override public String toString() { return "BLACK_HOLE_RENDER"; }
    };

    private final float halfWidth;  // 宽度的一半
    private final float halfHeight; // 高度的一半

    protected BlackHoleParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);

        this.gravity = 0.0F;
        this.friction = 0.96F; // 增加一点阻力防止飞太远

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        // 设置粒子大小
        // 这是一个竖着的长方形片
        this.halfWidth = 0.06F;
        this.halfHeight = 0.06F; // 沿Y轴比较高，符合“沿y正半轴向上”的感觉

        this.lifetime = 60 + this.random.nextInt(40);

        // 不需要随机旋转了，因为我们要严格控制法线指向圆心
    }

    @Override
    public ParticleRenderType getRenderType() {
        return BLACK_HOLE_RENDER;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // 1. 获取插值后的粒子世界坐标
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x));
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y));
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z));

        // 2. 变换到相对于相机的坐标
        Vector3f camPos = camera.getPosition().toVector3f();
        float renderX = x - camPos.x();
        float renderY = y - camPos.y();
        float renderZ = z - camPos.z();

        Matrix4f mat = new Matrix4f();
        mat.translate(renderX, renderY, renderZ);

        // 【核心修复2】计算法线指向圆心所需的旋转
        // 黑洞中心是 (0,0,0)。
        // 粒子位置是 (x, y, z)。
        // 向量 P = (x, 0, z) 是从圆心指出来的方向。
        // 我们要让粒子的法线（Local Z+）指向圆心，也就是指向 (-x, 0, -z)。

        // Math.atan2(x, z) 给出的是向量 (x, z) 偏离 Z轴的角度。
        // 如果我们旋转这个角度，粒子的 Z轴（法线）会指向外侧。
        // 所以我们需要 + PI (180度)，让法线指向内侧。
        float angleY = (float) (Math.atan2(x, z) + Math.PI);

        mat.rotateY(angleY);

        int r = 160; int g = 100; int b = 255; int a = 255-2*this.age;
        // 4. 绘制直立四边形 (Initial Pose along Y axis)
        // 此时坐标系已经旋转好了：
        // 局部 X轴 = 切线方向
        // 局部 Y轴 = 竖直方向 (World Up)
        // 局部 Z轴 = 指向圆心 (Normal)

        // 逆时针绘制 4 个顶点 (QUADS 标准顺序)
        // 1. 左上
        vertex(buffer, mat, -halfWidth, halfHeight, 0, r, g, b, a);
        // 2. 左下
        vertex(buffer, mat, -halfWidth, -halfHeight, 0, r, g, b, a);
        // 3. 右下
        vertex(buffer, mat, halfWidth, -halfHeight, 0, r, g, b, a);
        // 4. 右上
        vertex(buffer, mat, halfWidth, halfHeight, 0, r, g, b, a);
    }

    private void vertex(VertexConsumer builder, Matrix4f pose, float x, float y, float z, int r, int g, int b, int a) {
        builder.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setLight(FULL_BRIGHT);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new BlackHoleParticle(level, x, y, z, vx, vy, vz);
        }
    }
}