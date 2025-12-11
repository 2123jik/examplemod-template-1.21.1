package com.example.examplemod.client.structure;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(value = Dist.CLIENT)
public class WorldRegionRenderer {

    // --- 配置与状态 ---
    private static BlockPos selectionMin = null;
    private static BlockPos selectionMax = null;
    private static BlockPos renderPosition = null; // 缩小模型显示的中心位置

    public static float GLOBAL_SCALE = 0.0125f; // 缩放比例，默认缩小10倍

    // --- 缓存 ---
    private static VertexBuffer cachedBuffer = null;
    private static boolean isBaking = false;
    private static boolean needsRebake = false;
    
    // 存储捕获的数据快照（相对坐标 -> 状态）
    private static final Map<BlockPos, BlockState> capturedData = new HashMap<>();
    private static BlockPos captureSize = BlockPos.ZERO; // 捕获区域的长宽高

    // --- 交互逻辑 (测试用) ---
    // 使用 木棍(Stick) 点击方块：
    // 1. 左键：设置起点 (Min)
    // 2. 右键：设置终点 (Max) 并触发抓取
    // 使用 钻石(Diamond) 右键点击：
    // 3. 设置渲染显示的中心点
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide) return;

        // 设置终点并抓取
        if (event.getItemStack().is(Items.STICK)) {
            selectionMax = event.getPos();
            if (selectionMin != null) {
                event.getEntity().displayClientMessage(Component.literal("区域已设定: 正在抓取并烘焙..."), true);
                captureWorldData(event.getLevel(), selectionMin, selectionMax);
            } else {
                event.getEntity().displayClientMessage(Component.literal("请先左键设置起点 (Min)"), true);
            }
            event.setCanceled(true);
        }

        // 设置显示位置
        if (event.getItemStack().is(Items.DIAMOND)) {
            renderPosition = event.getPos().above(); // 显示在点击方块的上方
            event.getEntity().displayClientMessage(Component.literal("显示位置已更新"), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide) return;
        if (event.getItemStack().is(Items.STICK)) {
            selectionMin = event.getPos();
            event.getEntity().displayClientMessage(Component.literal("起点已设定: " + selectionMin.toShortString()), true);
            event.setCanceled(true);
        }
    }

    // --- 核心逻辑 1：抓取世界数据 (主线程) ---
    public static void captureWorldData(BlockGetter level, BlockPos p1, BlockPos p2) {
        if (isBaking) return;
        
        // 计算包围盒
        BlockPos min = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        BlockPos max = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
        
        capturedData.clear();
        captureSize = max.subtract(min).offset(1, 1, 1);

        // 限制最大体积防止卡顿 (例如限制 64x64x64)
        if (captureSize.getX() * captureSize.getY() * captureSize.getZ() > 200000) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("区域过大，无法抓取!"), false);
            return;
        }

        // 遍历并存入 Map (存储相对坐标)
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getRenderShape() != RenderShape.INVISIBLE) {
                // 存储相对坐标： Pos - Min
                capturedData.put(pos.subtract(min).immutable(), state);
            }
        }

        needsRebake = true;
    }

    // --- 渲染循环 ---
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        
        // 触发烘焙
        if (needsRebake && !isBaking) {
            bakeCapturedData();
            needsRebake = false;
        }

        if (cachedBuffer == null || renderPosition == null) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = event.getCamera().getPosition();
        
        // 准备渲染状态
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.setShader(GameRenderer::getRendertypeCutoutShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
        var shader = GameRenderer.getRendertypeCutoutShader();
        Matrix4f viewMatrix = event.getModelViewMatrix();
        Matrix4f drawMatrix = new Matrix4f(viewMatrix);

        // 计算渲染位置： 
        // 目标位置 - 相机位置 = 相对相机的世界坐标
        float dx = (float) (renderPosition.getX() - cameraPos.x) + 0.5f; // +0.5 居中
        float dy = (float) (renderPosition.getY() - cameraPos.y);
        float dz = (float) (renderPosition.getZ() - cameraPos.z) + 0.5f;

        drawMatrix.translate(dx, dy, dz);
        drawMatrix.scale(GLOBAL_SCALE*10, GLOBAL_SCALE*10, GLOBAL_SCALE*10);
        
        // 居中对齐模型：将模型的中心点移到 (0,0,0)
        drawMatrix.translate(
            -captureSize.getX() / 2.0f, 
            0, 
            -captureSize.getZ() / 2.0f
        );

        cachedBuffer.bind();
        cachedBuffer.drawWithShader(drawMatrix, projectionMatrix, shader);
        VertexBuffer.unbind();

        mc.gameRenderer.lightTexture().turnOffLightLayer();
    }

    // --- 核心逻辑 2：异步烘焙 ---
    private static void bakeCapturedData() {
        isBaking = true;
        // 克隆数据以防止主线程修改
        Map<BlockPos, BlockState> dataToBake = new HashMap<>(capturedData);

        CompletableFuture.runAsync(() -> {
            // 虚拟世界用于面剔除
            BlockGetter fakeLevel = new MapBlockGetter(dataToBake);
            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
            RandomSource random = RandomSource.create();
            ModelData modelData = ModelData.EMPTY;
            PoseStack matrix = new PoseStack();

            // 分配缓冲区
            ByteBufferBuilder allocator = new ByteBufferBuilder(1024 * 64);
            BufferBuilder builder = new BufferBuilder(allocator, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

            for (Map.Entry<BlockPos, BlockState> entry : dataToBake.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();

                matrix.pushPose();
                matrix.translate(pos.getX(), pos.getY(), pos.getZ());
                
                BakedModel model = blockRenderer.getBlockModel(state);
                random.setSeed(state.getSeed(pos)); // 使用相对坐标作为种子，保证纹理一致

                // 渲染每个面
                for (Direction dir : Direction.values()) {
                    // 只有当相邻位置没有方块时才渲染该面 (简单的面剔除)
                    // 注意：这里的 pos 是相对坐标，fakeLevel 也是基于相对坐标的
                    if (Block.shouldRenderFace(state, fakeLevel, pos, dir, pos.relative(dir))) {
                        List<BakedQuad> quads = model.getQuads(state, dir, random, modelData, null);
                        renderQuads(builder, matrix, quads);
                    }
                }
                
                // 渲染无方向的面 (如花草)
                List<BakedQuad> nullQuads = model.getQuads(state, null, random, modelData, null);
                renderQuads(builder, matrix, nullQuads);

                matrix.popPose();
            }

            MeshData meshData = builder.build();

            // 回到主线程上传
            RenderSystem.recordRenderCall(() -> {
                if (cachedBuffer != null) cachedBuffer.close();
                if (meshData != null) {
                    cachedBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    cachedBuffer.bind();
                    cachedBuffer.upload(meshData);
                    VertexBuffer.unbind();
                    meshData.close();
                }
                isBaking = false;
            });
        });
    }

    private static void renderQuads(VertexConsumer buffer, PoseStack poseStack, List<BakedQuad> quads) {
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            // 这里使用满亮度和无覆盖层，使其看起来像全息图或模型
            buffer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, 1.0f, 15728880, OverlayTexture.NO_OVERLAY);
        }
    }

    // --- 辅助类：虚拟世界读取器 ---
    private static class MapBlockGetter implements BlockGetter {
        private final Map<BlockPos, BlockState> map;

        public MapBlockGetter(Map<BlockPos, BlockState> map) {
            this.map = map;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            // 这里的 pos 是相对坐标
            BlockState state = map.get(pos);
            return state != null ? state : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) { return Fluids.EMPTY.defaultFluidState(); }
        @Override
        public int getHeight() { return 256; }
        @Override
        public int getMinBuildHeight() { return 0; }
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) { return null; }
    }

    // --- 清理 ---
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RenderSystem.recordRenderCall(() -> {
                if (cachedBuffer != null) {
                    cachedBuffer.close();
                    cachedBuffer = null;
                }
            });
            capturedData.clear();
            selectionMin = null;
            selectionMax = null;
            renderPosition = null;
        }
    }
}