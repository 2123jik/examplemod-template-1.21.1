package com.example.examplemod.client.structure;

import com.example.examplemod.ExampleMod;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jline.utils.Log;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL;

@EventBusSubscriber(value = Dist.CLIENT)
public class BakedStructureRenderer {

    // --- 配置区域 ---
    private static final ResourceLocation STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "structures/kisegi_sanctuary_main_0.nbt");

    public static float GLOBAL_SCALE = 0.0325f;

    // 块大小: 32x32x32 (位移 5)
    private static final int SECTION_SHIFT = 5;

    // 最大渲染距离平方
    private static final double MAX_RENDER_DIST_SQR = 64.0 * 64.0;

    // --- 缓存 ---
    private static final Set<BlockPos> allInstances = new LinkedHashSet<>();
    private static final List<BlockPos> visibleInstancesCache = new ArrayList<>();
    private static final List<RenderSection> cachedSections = new ArrayList<>();
    private static AABB totalStructureBounds = null;

    private static StructureTemplate cachedTemplate = null;
    private static boolean isBaking = false;
    private static Field paletteField = null;

    // --- 内部类：渲染分块 ---
    private static class RenderSection implements AutoCloseable {
        final VertexBuffer buffer;
        final AABB localAABB;
        final Vec3 center;
        double sortDistSqr;

        RenderSection(VertexBuffer buffer, AABB localAABB) {
            this.buffer = buffer;
            this.localAABB = localAABB;
            this.center = localAABB.getCenter();
        }

        @Override
        public void close() {
            if (buffer != null) buffer.close();
        }
    }

    // --- 内部类：虚拟世界读取器 (用于面剔除) ---
    private static class MapBlockGetter implements BlockGetter {
        private final Map<BlockPos, BlockState> map;

        public MapBlockGetter(Map<BlockPos, BlockState> map) {
            this.map = map;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            BlockState state = map.get(pos);
            return state != null ? state : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }


        @Override
        public int getHeight() { return 256; }
        @Override
        public int getMinBuildHeight() { return 0; }
        @Override
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) { return null; }
    }
    //private static final boolean test=true;
    private static void println(final Object o) {
        System.out.println(o);
    }
    private static void println1(final Object o) {
        System.out.println(o);
    }
    // --- 交互触发 ---
    @SubscribeEvent
    public static void onPlayerPlaceBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide && event.getItemStack().is(Items.BEDROCK)) {
            BlockPos targetPos = event.getPos().relative(event.getFace()).above(2);
            allInstances.add(targetPos);
            if (cachedSections.isEmpty() && !isBaking) triggerBakingPipeline();
        }

    }

    // --- 渲染循环 ---
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (allInstances.isEmpty()) return;
        if (cachedSections.isEmpty()) {
            if (!isBaking) triggerBakingPipeline();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = event.getCamera().getPosition();
        Frustum frustum = event.getFrustum();
        float currentScale = GLOBAL_SCALE;

        // 1. 实例筛选与排序
        visibleInstancesCache.clear();

        if (totalStructureBounds == null) {
            visibleInstancesCache.addAll(allInstances);
        } else {
            // 预计算缩放后的整体包围盒尺寸
            double sMinX = totalStructureBounds.minX * currentScale;
            double sMinY = totalStructureBounds.minY * currentScale;
            double sMinZ = totalStructureBounds.minZ * currentScale;
            double sMaxX = totalStructureBounds.maxX * currentScale;
            double sMaxY = totalStructureBounds.maxY * currentScale;
            double sMaxZ = totalStructureBounds.maxZ * currentScale;

            for (BlockPos pos : allInstances) {
                double distSqr = pos.distToCenterSqr(cameraPos.x, cameraPos.y, cameraPos.z);
                if (distSqr > MAX_RENDER_DIST_SQR) continue;

                // 视锥剔除
                double worldMinX = pos.getX() + sMinX;
                double worldMinY = pos.getY() + sMinY;
                double worldMinZ = pos.getZ() + sMinZ;
                double worldMaxX = pos.getX() + sMaxX;
                double worldMaxY = pos.getY() + sMaxY;
                double worldMaxZ = pos.getZ() + sMaxZ;

                if (frustum.isVisible(new AABB(worldMinX, worldMinY, worldMinZ, worldMaxX, worldMaxY, worldMaxZ))) {
                    visibleInstancesCache.add(pos);
                }
            }
        }

        if (visibleInstancesCache.isEmpty()) return;

        // 由近到远排序实例
        visibleInstancesCache.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(cameraPos.x, cameraPos.y, cameraPos.z)));

        // 2. 分块排序
        BlockPos closestInstance = visibleInstancesCache.get(0);
        for (RenderSection section : cachedSections) {
            double worldCenterX = closestInstance.getX() + section.center.x * currentScale;
            double worldCenterY = closestInstance.getY() + section.center.y * currentScale;
            double worldCenterZ = closestInstance.getZ() + section.center.z * currentScale;
            section.sortDistSqr = cameraPos.distanceToSqr(worldCenterX, worldCenterY, worldCenterZ);
        }
        cachedSections.sort(Comparator.comparingDouble(s -> s.sortDistSqr));

        // 3. 渲染提交
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.setShader(GameRenderer::getRendertypeCutoutShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
        var shader = GameRenderer.getRendertypeCutoutShader();
        Matrix4f baseViewMatrix = event.getModelViewMatrix();
        Matrix4f drawMatrix = new Matrix4f();

        for (RenderSection section : cachedSections) {
            double sScaledMinX = section.localAABB.minX * currentScale;
            double sScaledMinY = section.localAABB.minY * currentScale;
            double sScaledMinZ = section.localAABB.minZ * currentScale;
            double sScaledMaxX = section.localAABB.maxX * currentScale;
            double sScaledMaxY = section.localAABB.maxY * currentScale;
            double sScaledMaxZ = section.localAABB.maxZ * currentScale;

            boolean bound = false;

            for (BlockPos pos : visibleInstancesCache) {
                double worldMinX = pos.getX() + sScaledMinX;
                double worldMinY = pos.getY() + sScaledMinY;
                double worldMinZ = pos.getZ() + sScaledMinZ;
                double worldMaxX = pos.getX() + sScaledMaxX;
                double worldMaxY = pos.getY() + sScaledMaxY;
                double worldMaxZ = pos.getZ() + sScaledMaxZ;

                // 分块级视锥剔除
                if (!frustum.isVisible(new AABB(worldMinX, worldMinY, worldMinZ, worldMaxX, worldMaxY, worldMaxZ))) {
                    continue;
                }

                if (!bound) {
                    section.buffer.bind();
                    bound = true;
                }

                float dx = (float) (pos.getX() - cameraPos.x);
                float dy = (float) (pos.getY() - cameraPos.y);
                float dz = (float) (pos.getZ() - cameraPos.z);

                drawMatrix.set(baseViewMatrix);
                drawMatrix.translate(dx, dy, dz);
                drawMatrix.scale(currentScale, currentScale, currentScale);

                section.buffer.drawWithShader(drawMatrix, projectionMatrix, shader);
            }

            if (bound) {
                VertexBuffer.unbind();
            }
        }
        mc.gameRenderer.lightTexture().turnOffLightLayer();
    }

    private static void triggerBakingPipeline() {
        isBaking = true;
        CompletableFuture.supplyAsync(() -> {
            if (cachedTemplate == null) {
                return ClientStructureLoader.loadStructure(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "structures/kisegi_sanctuary_main_0.nbt"));
            }
            return cachedTemplate;
        }).thenAcceptAsync(template -> {
            if (template == null) {
                isBaking = false;
                return;
            }
            cachedTemplate = template;
            bakeOnMainThread(template);
        }, Minecraft.getInstance());
    }

    private static void bakeOnMainThread(StructureTemplate template) {
        try {
            List<StructureTemplate.Palette> palettes = getPalettesSafe(template);
            if (palettes == null || palettes.isEmpty()) {
                isBaking = false;
                return;
            }

            List<StructureTemplate.StructureBlockInfo> blocks = palettes.get(0).blocks();
            Map<BlockPos, BlockState> blockMap = Maps.newHashMapWithExpectedSize(blocks.size());
            for (StructureTemplate.StructureBlockInfo info : blocks) {
                blockMap.put(info.pos(), info.state());
            }

            // 创建虚拟世界用于面剔除判断
            BlockGetter fakeLevel = new MapBlockGetter(blockMap);

            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
            RandomSource random = RandomSource.create();

            Map<Long, ByteBufferBuilder> allocators = new HashMap<>();
            Map<Long, BufferBuilder> builders = new HashMap<>();
            Map<Long, AABB> sectionBounds = new HashMap<>();

            double totalMinX = Double.MAX_VALUE, totalMinY = Double.MAX_VALUE, totalMinZ = Double.MAX_VALUE;
            double totalMaxX = -Double.MAX_VALUE, totalMaxY = -Double.MAX_VALUE, totalMaxZ = -Double.MAX_VALUE;
            boolean hasBlocks = false;

            PoseStack bakeStack = new PoseStack();
            ModelData modelData = ModelData.EMPTY;

            for (StructureTemplate.StructureBlockInfo info : blocks) {
                BlockState state = info.state();
                BlockPos pos = info.pos();

                if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) continue;

                long sectionKey = BlockPos.asLong(pos.getX() >> SECTION_SHIFT, pos.getY() >> SECTION_SHIFT, pos.getZ() >> SECTION_SHIFT);

                // 延迟初始化 Builder
                BufferBuilder builder = builders.computeIfAbsent(sectionKey, k -> {
                    // 预分配内存可以适当减小，因为面剔除后数据量会减少
                    ByteBufferBuilder alloc = new ByteBufferBuilder(1024 * 64);
                    allocators.put(k, alloc);
                    return new BufferBuilder(alloc, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                });

                BakedModel model = blockRenderer.getBlockModel(state);
                bakeStack.pushPose();
                bakeStack.translate(pos.getX(), pos.getY(), pos.getZ());
                random.setSeed(state.getSeed(pos));

                boolean renderedAnyFace = false;

                // === 关键优化：面剔除循环 ===
                for (Direction dir : Direction.values()) {
                    // 只有当面没有被遮挡时，才进行绘制
                    if (Block.shouldRenderFace(state, fakeLevel, pos, dir, pos.relative(dir))) {
                        List<BakedQuad> quads = model.getQuads(state, dir, random, modelData, null);
                        if (!quads.isEmpty()) {
                            renderQuads(builder, bakeStack, quads, 15728880, OverlayTexture.NO_OVERLAY);
                            renderedAnyFace = true;
                        }
                    }
                }

                // 处理非贴图面（如植物交叉模型，dir 为 null 的情况）
                List<BakedQuad> nullQuads = model.getQuads(state, null, random, modelData, null);
                if (!nullQuads.isEmpty()) {
                    renderQuads(builder, bakeStack, nullQuads, 15728880, OverlayTexture.NO_OVERLAY);
                    renderedAnyFace = true;
                }

                bakeStack.popPose();

                if (renderedAnyFace) {
                    totalMinX = Math.min(totalMinX, pos.getX());
                    totalMinY = Math.min(totalMinY, pos.getY());
                    totalMinZ = Math.min(totalMinZ, pos.getZ());
                    totalMaxX = Math.max(totalMaxX, pos.getX() + 1);
                    totalMaxY = Math.max(totalMaxY, pos.getY() + 1);
                    totalMaxZ = Math.max(totalMaxZ, pos.getZ() + 1);
                    hasBlocks = true;
                    sectionBounds.merge(sectionKey, new AABB(pos), AABB::minmax);
                }
            }

            if (hasBlocks) {
                totalStructureBounds = new AABB(totalMinX, totalMinY, totalMinZ, totalMaxX, totalMaxY, totalMaxZ);
            }

            List<RenderSection> newSections = new ArrayList<>();
            for (Map.Entry<Long, BufferBuilder> entry : builders.entrySet()) {
                MeshData meshData = entry.getValue().build();
                if (meshData != null) {
                    VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    vbo.bind();
                    vbo.upload(meshData);
                    VertexBuffer.unbind();
                    meshData.close(); // 及时释放 MeshData 内存

                    AABB bounds = sectionBounds.get(entry.getKey());
                    newSections.add(new RenderSection(vbo, bounds));
                }
            }

            // 释放分配器内存
            for (ByteBufferBuilder alloc : allocators.values()) {
                alloc.close();
            }

            RenderSystem.recordRenderCall(() -> {
                for (RenderSection s : cachedSections) s.close();
                cachedSections.clear();
                cachedSections.addAll(newSections);
                isBaking = false;
            });

        } catch (Exception e) {
            e.printStackTrace();
            isBaking = false;
        }
    }

    // --- 辅助方法 ---

    private static void renderQuads(VertexConsumer buffer, PoseStack poseStack, List<BakedQuad> quads, int packedLight, int packedOverlay) {
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            buffer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
        }
    }

    private static List<StructureTemplate.Palette> getPalettesSafe(StructureTemplate template) {
        try {
            if (paletteField != null) {
                return (List<StructureTemplate.Palette>) paletteField.get(template);
            }
            String[] fieldNames = {"palettes", "f_74482_", "field_186272_a", "b"};
            for (String name : fieldNames) {
                try {
                    Field field = StructureTemplate.class.getDeclaredField(name);
                    field.setAccessible(true);
                    List<StructureTemplate.Palette> result = (List<StructureTemplate.Palette>) field.get(template);
                    if (result != null) {
                        paletteField = field;
                        return result;
                    }
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    @SubscribeEvent
    public static void onlevelunload(LevelEvent.Unload event) {
        if(event.getLevel().isClientSide()){
            reset();
        }
    }
    public static void reset() {
        RenderSystem.recordRenderCall(() -> {
            for (RenderSection section : cachedSections) section.close();
            cachedSections.clear();
        });
        cachedTemplate = null;
        allInstances.clear();
        isBaking = false;
        totalStructureBounds = null;
        visibleInstancesCache.clear();
    }
}