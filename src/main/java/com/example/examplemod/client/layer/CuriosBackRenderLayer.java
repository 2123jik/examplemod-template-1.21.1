package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class CuriosBackRenderLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private static final Map<Item, Float> SIZE_CACHE = new HashMap<>();
    private static final Map<Item, Float> SCALE_CACHE = new HashMap<>();

    public CuriosBackRenderLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        event.getSkins().forEach(skinName -> {
            PlayerRenderer renderer = event.getSkin(skinName);
            if (renderer != null) {
                renderer.addLayer(new CuriosBackRenderLayer<>(renderer));
            }
        });
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        List<ItemStack> stacks = getCuriosBackStacks(entity);
        if (stacks.isEmpty()) return;

        matrixStack.pushPose();
        if (this.getParentModel() instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.body.translateAndRotate(matrixStack);
        }
        float a = 30;
        float dz = 0.06f;
        float z = 0.3f;

        matrixStack.translate(0.0D, 0.0D, z);

        matrixStack.pushPose();
        matrixStack.mulPose(Axis.ZP.rotationDegrees(a));
        ri(stacks.get(0), matrixStack, buffer, packedLight, entity);

        if (stacks.size() > 1) {
            matrixStack.pushPose();
            matrixStack.mulPose(Axis.ZP.rotationDegrees(-a));
            matrixStack.mulPose(Axis.YP.rotationDegrees(180F));
            matrixStack.translate(0.0D, 0.0D, -dz);
            ri(stacks.get(1), matrixStack, buffer, packedLight, entity);
        }
        matrixStack.popPose();
    }

    private List<ItemStack> getCuriosBackStacks(LivingEntity entity) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(entity);
        return optional.map(iCuriosItemHandler -> iCuriosItemHandler.findCurios(item -> true)
                .stream()
                .map(SlotResult::stack)
                .filter(stack -> !stack.isEmpty() && !stack.is(Items.ELYTRA))
                .limit(3)
                .toList()).orElse(Collections.emptyList());
    }

    private void ri(ItemStack stack, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, T entity) {
        matrixStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        float scale = getHybridScale(stack);

        if (scale != 1.0f) {
            matrixStack.scale(scale, scale, scale);
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                matrixStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        matrixStack.popPose();
    }

    private float getHybridScale(ItemStack stack) {
        float geoScale = getSmartScale(stack);

        if (geoScale < 0.99f) {
            return geoScale;
        }

        float resScale = getScaleByResolution(stack);

        return geoScale * resScale;
    }

    private float getSmartScale(ItemStack stack) {
        Item item = stack.getItem();
        if (SIZE_CACHE.containsKey(item)) {
            return SIZE_CACHE.get(item);
        }

        float scale = 1.0f;

        try {
            Minecraft mc = Minecraft.getInstance();
            BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, null, 0);

            if (model != null) {
                float maxExtent = calculateModelMaxExtent(model);
                if (maxExtent > 2.5f) {
                    scale = 0.5f;
                } else if (maxExtent > 1.5f) {
                    scale = 0.65f;
                } else if (maxExtent > 1.1f) {
                    scale = 0.8f;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SIZE_CACHE.put(item, scale);
        return scale;
    }

    private float calculateModelMaxExtent(BakedModel model) {
        float maxDistanceSq = 0f;
        RandomSource random = RandomSource.create(0);
        List<BakedQuad> allQuads = new java.util.ArrayList<>(model.getQuads(null, null, random));
        for (Direction dir : Direction.values()) {
            allQuads.addAll(model.getQuads(null, dir, random));
        }

        for (BakedQuad quad : allQuads) {
            int[] vertices = quad.getVertices();
            int vertexSize = 8;

            for (int i = 0; i < 4; i++) {
                int offset = i * vertexSize;
                if (offset + 2 >= vertices.length) break;
                float x = Float.intBitsToFloat(vertices[offset + 0]);
                float y = Float.intBitsToFloat(vertices[offset + 1]);
                float z = Float.intBitsToFloat(vertices[offset + 2]);
                float distSq = x * x + y * y + z * z;
                if (distSq > maxDistanceSq) {
                    maxDistanceSq = distSq;
                }
            }
        }
        return (float) Math.sqrt(maxDistanceSq);
    }

    private float getScaleByResolution(ItemStack stack) {
        Item item = stack.getItem();
        if (SCALE_CACHE.containsKey(item)) {
            return SCALE_CACHE.get(item);
        }

        float scale = 1.0f;
        int maxResolution = 16;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(),
                "textures/item/" + itemId.getPath() + ".png"
        );

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource = resourceManager.getResource(textureLocation);

        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open()) {
                NativeImage image = NativeImage.read(stream);
                int width = image.getWidth();
                int height = image.getHeight();
                maxResolution = Math.max(width, height);
                image.close();
            } catch (IOException e) {
            }
        }

        if (maxResolution >= 64) {
            scale = 0.5f;
        } else if (maxResolution >= 32) {
            scale = 0.75f;
        }

        SCALE_CACHE.put(item, scale);
        return scale;
    }
}