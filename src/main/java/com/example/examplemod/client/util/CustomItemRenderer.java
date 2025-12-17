package com.example.examplemod.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class CustomItemRenderer {

    public static final CustomItemRenderer INSTANCE = new CustomItemRenderer();

    // 演示用的物品
    private static final ItemStack DISPLAY_ITEM = new ItemStack(Items.DIAMOND_SWORD);

    private CustomItemRenderer() {}

    public void render(RenderLevelStageEvent event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        Vec3 targetPos = new Vec3(80,120,80);

        poseStack.pushPose();

        // 2. 坐标修正：PoseStack 是相对相机的，所以必须移动到 (目标世界坐标 - 相机坐标)
        // 原代码直接使用了 -cameraPos，这会导致物品渲染在世界原点(0,0,0)附近，而不是你想要的位置
        poseStack.translate(targetPos.x - cameraPos.x, targetPos.y - cameraPos.y + 1.0, targetPos.z - cameraPos.z);

        poseStack.scale(32.0f, 32.0f, 32.0f);

        // 4. 获取 BufferSource
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        BlockPos targetBlockPos = BlockPos.containing(targetPos);
        int packedLight = LevelRenderer.getLightColor(mc.level, targetBlockPos);
        // 5. 调用渲染
        mc.getItemRenderer().renderStatic(
                mc.player.getMainHandItem(),
                ItemDisplayContext.GROUND, // 使用 GROUND 或 FIXED 模式
                packedLight,  // 光照：0xF000F0 (满亮)，或者使用 LevelRenderer.getLightColor 获取实际光照
                OverlayTexture.NO_OVERLAY, // 覆盖层：通常使用 NO_OVERLAY
                poseStack,
                bufferSource,
                mc.level,
                0
        );

        bufferSource.endBatch();

        poseStack.popPose();

    }
}