package com.example.examplemod.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// T: 生物类型, M: 生物模型
public class BlockCapeLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    // 用于画一个标准 1x1x1 方块的简单模型部分 (这里用代码手撸顶点，比ModelPart更灵活)
    public BlockCapeLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;

        // --- 1. 获取“随机”方块 ---
        // 使用 Entity ID 做种子，保证同一个生物每次渲染都是同一个方块
        long seed = entity.getId() * 192301L;
        Block randomBlock = BuiltInRegistries.BLOCK.byId((int) (Math.abs(seed) % BuiltInRegistries.BLOCK.size()));
        if (randomBlock == Blocks.AIR || randomBlock == null) randomBlock = Blocks.TNT; // 保底逻辑

        // 获取方块的贴图精灵 (Sprite)
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(randomBlock.defaultBlockState());

        poseStack.pushPose();

        // --- 2. 定位到背部 ---
        // 不同生物的背部位置不一样，这里取一个大概的通用值
        // 如果是 Humanoid (僵尸/骷髅)，这个位置比较准；如果是猪/牛，可能需要根据 entity.getType() 微调
        poseStack.translate(0.0F, 0.0F, 0.15F); // 向后一点
        if (entity.isCrouching()) {
            poseStack.translate(0.0F, 0.2F, 0.0F); // 潜行修正
        }

        // --- 3. 物理逻辑 (简化版复用) ---
        // 因为普通生物没有 cloakX/Y/Z，我们用 (当前位置 - 上一帧位置) 来模拟拖尾
        double d0 = Mth.lerp(partialTicks, entity.xo, entity.getX()) - Mth.lerp(partialTicks, entity.xo, entity.getX()); // X差值 (近似)
        // 实际上上面的计算对于非玩家实体很难平滑，我们直接用 BodyRot 配合移动速度模拟

        float bodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        double velX = Mth.lerp(partialTicks, entity.xo, entity.getX()) - entity.xo;
        double velZ = Mth.lerp(partialTicks, entity.zo, entity.getZ()) - entity.zo;
        float speed = (float) Math.sqrt(velX * velX + velZ * velZ) * 100f; // 速度放大

        // 简单的摆动逻辑：速度越快，抬起越高
        float lift = speed;
        lift = Mth.clamp(lift, 0, 80); // 限制抬起角度

        // 走路时的自然摆动
        float walkSwing = Mth.sin(limbSwing * 0.6F) * limbSwingAmount * 10.0F;

        poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + lift / 2.0F + walkSwing)); // 上下摆动
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F)); // 旋转180度，让方块正面朝外

        // --- 4. 关键：压扁方块 ---
        // X轴宽一点(0.6)，Y轴长一点(0.9)，Z轴极扁(0.05) -> 这就变成了披风
        poseStack.scale(0.6F, 0.9F, 0.05F);

        // 下移一点，让旋转中心在肩膀
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        // --- 5. 绘制方块 ---
        // 使用 CUTOUT 渲染类型 (支持透明方块如玻璃)
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        renderFlattenedCube(poseStack, vertexConsumer, packedLight, sprite);

        poseStack.popPose();
    }

    // 手动画一个立方体，贴上 Block 的纹理
    private void renderFlattenedCube(PoseStack poseStack, VertexConsumer consumer, int light, TextureAtlasSprite sprite) {
        Matrix4f m = poseStack.last().pose();
        Matrix3f n = poseStack.last().normal();

        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        // 这里只简单画背面和正面，为了性能可以少画几个面
        // 正面 (Face)
        addVertex(consumer, m, n, light, 0, 1, 0, minU, maxV);
        addVertex(consumer, m, n, light, 1, 1, 0, maxU, maxV);
        addVertex(consumer, m, n, light, 1, 0, 0, maxU, minV);
        addVertex(consumer, m, n, light, 0, 0, 0, minU, minV);

        // 背面 (Back)
        addVertex(consumer, m, n, light, 0, 0, 1, minU, minV);
        addVertex(consumer, m, n, light, 1, 0, 1, maxU, minV);
        addVertex(consumer, m, n, light, 1, 1, 1, maxU, maxV);
        addVertex(consumer, m, n, light, 0, 1, 1, minU, maxV);

        // 侧面稍微画一下以免露馅（代码省略，原理同上）
    }

    private void addVertex(VertexConsumer consumer, Matrix4f m, Matrix3f n, int light, float x, float y, float z, float u, float v) {
        consumer.addVertex(m, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal( 0, 1, 0);
    }
}