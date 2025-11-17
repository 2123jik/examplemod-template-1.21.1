//package com.example.examplemod.client;
//
//import com.example.examplemod.ExampleMod;
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.model.WardenModel;
//import net.minecraft.client.model.geom.ModelLayers;
//import net.minecraft.client.model.geom.ModelPart;
//import net.minecraft.client.player.AbstractClientPlayer;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.client.renderer.texture.OverlayTexture;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.HumanoidArm;
//import net.minecraft.world.entity.monster.warden.Warden;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RenderArmEvent;
//
//@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
//public class PlayerArmRenderer {
//
//    // 使用延迟初始化来缓存模型和虚拟实体
//    private static WardenModel<Warden> wardenModel;
//    private static Warden virtualWarden;
//
//    // 监守者的材质位置
//    private static final ResourceLocation WARDEN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/warden/warden.png");
//
//    private static void initVirtualMembers() {
//        if (virtualWarden == null && Minecraft.getInstance().level != null) {
//            wardenModel = new WardenModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.WARDEN));
//            virtualWarden = new Warden(EntityType.WARDEN, Minecraft.getInstance().level);
//        }
//    }
//
//    @SubscribeEvent
//    public static void onRenderPlayerArm(RenderArmEvent event) {
//        // 确保我们的虚拟对象已经初始化
//        initVirtualMembers();
//        if (wardenModel == null || virtualWarden == null) {
//            return;
//        }
//
//        // 我们只替换右手
//        if (event.getArm() != HumanoidArm.RIGHT) {
//            return;
//        }
//
//        // 关键步骤：阻止原版手臂的渲染
//        event.setCanceled(true);
//
//        // 从事件中获取必要参数
//        PoseStack poseStack = event.getPoseStack();
//        MultiBufferSource buffer = event.getMultiBufferSource();
//        int packedLight = event.getPackedLight();
//        AbstractClientPlayer player = event.getPlayer();
//
//        poseStack.pushPose();
//        wardenModel.root().getAllParts().forEach(ModelPart::resetPose);
//        // --- 动画同步 ---
//        // 我们需要获取玩家当前的动画状态。RenderArmEvent 没有直接提供 limbSwing 等参数，
//        // 但我们可以从 player 对象中获取到近似值。
//        // 一个更简单的方法是，让监守者的手臂直接模仿玩家手臂的姿态。
//        // 但为了通用性，我们先尝试“嫁接”动画。
//        // 注意：由于事件没有提供所有动画参数，这里的动作可能不会100%完美匹配，但可以作为一个很好的起点。
//        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
//        float ageInTicks = player.tickCount + partialTick;
//        float limbSwing = player.walkDist - player.walkDistO; // 这是一个简化的近似
//        float limbSwingAmount = player.walkDist;
//
//        // 让监守者模型进行动画计算
//        wardenModel.setupAnim(virtualWarden, limbSwing, limbSwingAmount, ageInTicks, 0, 0);
//
//        // --- 渲染监守者手臂 ---
//        // 1. 微调位置和旋转，使其看起来像是从玩家肩膀长出来的
//        //    这些值需要反复试验才能找到最佳效果！
//        poseStack.translate(0.0D, -0.6D, 0.0D); // 向下移动一点
//        poseStack.scale(1.5f, 1.5f, 1.5f); // 放大手臂模型
//
//        // 2. 获取正确的 VertexConsumer
//        var vertexConsumer = buffer.getBuffer(RenderType.entityCutout(WARDEN_TEXTURE));
//
//        // 3. 单独渲染 rightArm 部件
//        wardenModel.root().getChild("bone").getChild("body").getChild("right_arm").render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
//
//        poseStack.popPose();
//    }
//}