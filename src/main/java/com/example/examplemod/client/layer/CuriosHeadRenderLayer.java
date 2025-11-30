package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
// 引入刚才创建的配置类
import com.example.examplemod.client.config.HeadLayerConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class CuriosHeadRenderLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public CuriosHeadRenderLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        event.getSkins().forEach(skinName -> {
            PlayerRenderer renderer = event.getSkin(skinName);
            if (renderer != null) {
                renderer.addLayer(new CuriosHeadRenderLayer<>(renderer));
            }
        });
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource bufferSource, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        List<ItemStack> stacks = getCuriosHeadStacks(livingEntity);
        if (stacks.isEmpty()) return;

        // 这里只渲染列表中的第一个物品作为示例，如果需要渲染多个，可以使用 for 循环并适当偏移
        ItemStack stack = stacks.get(0);

        matrixStack.pushPose();

        if (this.getParentModel() instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.head.translateAndRotate(matrixStack);
        }

        matrixStack.translate(HeadLayerConfig.transX, HeadLayerConfig.transY, HeadLayerConfig.transZ);

        matrixStack.scale((float) HeadLayerConfig.scaleX, (float) HeadLayerConfig.scaleY, (float) HeadLayerConfig.scaleZ);

        matrixStack.mulPose(Axis.ZP.rotationDegrees((float) HeadLayerConfig.rotZ));
        matrixStack.mulPose(Axis.YP.rotationDegrees((float) HeadLayerConfig.rotY));
        matrixStack.mulPose(Axis.XP.rotationDegrees((float) HeadLayerConfig.rotX));

        matrixStack.mulPose(Axis.XP.rotationDegrees(180.0F));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                livingEntity,
                stack,
                ItemDisplayContext.HEAD,
                false,
                matrixStack,
                bufferSource,
                livingEntity.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                livingEntity.getId()
        );

        matrixStack.popPose();
    }

    private List<ItemStack> getCuriosHeadStacks(LivingEntity entity) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(entity);
        return optional.map(iCuriosItemHandler -> iCuriosItemHandler.findCurios("head")
                .stream()
                .map(SlotResult::stack)
                .filter(stack -> !stack.isEmpty() && !stack.is(Items.ELYTRA))
                .limit(3)
                .toList()).orElse(Collections.emptyList());
    }
}