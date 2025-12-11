package com.example.examplemod.client.time;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(value = Dist.CLIENT)
public class TimeEchoRenderer {

    private static Method setSharedFlagMethod;
    private static Field compositeStateField, textureStateField, textureLocationField;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if(player.hasEffect(MobEffects.DAMAGE_BOOST))
        {


            {
            if (TimeTravelManager.isRenderingEcho) return;
            TimeTravelManager.isRenderingEcho = true;

            try {
                PoseStack poseStack = event.getPoseStack();
                Vec3 camPos = event.getCamera().getPosition();
                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

                // 设置为 0.8 秒
                float delaySeconds = 0.8f;
                float alpha = 0.3f;

                renderSwapGhost(mc, player, delaySeconds, camPos, poseStack, bufferSource, alpha);

                bufferSource.endBatch();
            } finally {
                TimeTravelManager.isRenderingEcho = false;
            }
            }
        }
    }

    private static void renderSwapGhost(Minecraft mc, AbstractClientPlayer player, float secondsAgo,
                                        Vec3 camPos, PoseStack poseStack, MultiBufferSource bufferSource, float alpha) {

        PlayerStateSnapshot snapshot = TimeTravelManager.getSnapshot(player.getUUID(), secondsAgo);
        if (snapshot == null) return;

        // === 1. 备份所有状态 (包含 Iron's Spells 魔法状态) ===
        double rX = player.getX(); double rY = player.getY(); double rZ = player.getZ();
        double rXo = player.xo; double rYo = player.yo; double rZo = player.zo;
        float rYRot = player.getYRot(); float rXRot = player.getXRot();
        float rYHead = player.yHeadRot; float rYBody = player.yBodyRot;
        float rYHeadO = player.yHeadRotO; float rYBodyO = player.yBodyRotO;
        Pose rPose = player.getPose();
        boolean rSneak = player.isShiftKeyDown();
        boolean rSprint = player.isSprinting();
        boolean rSwim = player.isSwimming();
        boolean rElytra = player.isFallFlying();
        float rLimbP = player.walkAnimation.position();
        float rLimbS = player.walkAnimation.speed();
        float rAttack = player.attackAnim;
        Map<EquipmentSlot, ItemStack> rEquip = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) rEquip.put(slot, player.getItemBySlot(slot));

        // 备份 ISS 数据
        var magicData = ClientMagicData.getSyncedSpellData(player);
        boolean rIsCasting = magicData.isCasting();
        String rSpellId = magicData.getCastingSpellId();
        int rLevel = magicData.getCastingSpellLevel();

        // === 2. 篡改状态 ===
        try {
            player.setPos(snapshot.x, snapshot.y, snapshot.z);
            player.xo = snapshot.x; player.yo = snapshot.y; player.zo = snapshot.z;
            player.setYRot(snapshot.yRot); player.yRotO = snapshot.yRot;
            player.setXRot(snapshot.xRot); player.xRotO = snapshot.xRot;
            player.yHeadRot = snapshot.yHeadRot; player.yHeadRotO = snapshot.yHeadRot;
            player.yBodyRot = snapshot.yBodyRot; player.yBodyRotO = snapshot.yBodyRot;
            player.setPose(snapshot.pose);
            player.setShiftKeyDown(snapshot.isSneaking);
            player.setSprinting(snapshot.isSprinting);
            player.setSwimming(snapshot.isSwimming);
            player.walkAnimation.setSpeed(snapshot.limbSwingAmount);
            player.attackAnim = snapshot.attackAnim;
            setProtectedFlag(player, 7, snapshot.isElytraFlying);
            for (var entry : snapshot.equipment.entrySet()) player.setItemSlot(entry.getKey(), entry.getValue());

            // 篡改 ISS 数据 (反射)
            injectMagicState(magicData, snapshot.isCasting, snapshot.castSpellId, snapshot.castLevel);

            // === 3. 渲染 ===
            poseStack.pushPose();
            poseStack.translate(snapshot.x - camPos.x, snapshot.y - camPos.y, snapshot.z - camPos.z);

            MultiBufferSource ghostBuffer = new SmartGhostBuffer(bufferSource, alpha);
            mc.getEntityRenderDispatcher().getRenderer(player).render(
                    player, 0, 1.0f, poseStack, ghostBuffer, 0xF000F0
            );

            poseStack.popPose();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // === 4. 还原所有状态 ===
            player.setPos(rX, rY, rZ);
            player.xo = rXo; player.yo = rYo; player.zo = rZo;
            player.setYRot(rYRot); player.yRotO = rYRot;
            player.setXRot(rXRot); player.xRotO = rXRot;
            player.yHeadRot = rYHead; player.yHeadRotO = rYHeadO;
            player.yBodyRot = rYBody; player.yBodyRotO = rYBodyO;
            player.setPose(rPose);
            player.setShiftKeyDown(rSneak);
            player.setSprinting(rSprint);
            player.setSwimming(rSwim);
            player.walkAnimation.setSpeed(rLimbS);
            player.attackAnim = rAttack;
            setProtectedFlag(player, 7, rElytra);
            for (var entry : rEquip.entrySet()) player.setItemSlot(entry.getKey(), entry.getValue());

            // 还原 ISS 数据
            injectMagicState(magicData, rIsCasting, rSpellId, rLevel);
        }
    }

    private static void injectMagicState(Object data, boolean isCasting, String spellId, int level) {
        try {
            Class<?> clazz = data.getClass();
            // 需要根据实际环境调整字段名
            Field f1 = getField(clazz, "isCasting"); f1.setBoolean(data, isCasting);
            Field f2 = getField(clazz, "castingSpellId"); f2.set(data, spellId);
            Field f3 = getField(clazz, "castingSpellLevel"); f3.setInt(data, level);
        } catch (Exception ignored) {}
    }

    private static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        Field f = clazz.getDeclaredField(name); f.setAccessible(true); return f;
    }

    private static void setProtectedFlag(Entity entity, int flag, boolean value) {
        try {
            if (setSharedFlagMethod == null) {
                setSharedFlagMethod = Entity.class.getDeclaredMethod("setSharedFlag", int.class, boolean.class);
                setSharedFlagMethod.setAccessible(true);
            }
            setSharedFlagMethod.invoke(entity, flag, value);
        } catch (Exception ignored) {}
    }

    // SmartGhostBuffer 类保持不变 (省略)
    private static class SmartGhostBuffer implements MultiBufferSource {
        private final MultiBufferSource delegate; private final float alpha;
        public SmartGhostBuffer(MultiBufferSource delegate, float alpha) { this.delegate = delegate; this.alpha = alpha; }
        @Override public VertexConsumer getBuffer(RenderType type) {
            RenderType newType = type;
            Optional<ResourceLocation> texture = getTextureFromRenderType(type);
            if (type.toString().contains("glint")) return new AlphaTintVertexConsumer(delegate.getBuffer(type), alpha * 0.8f);
            if (type.toString().contains("eyes")) return delegate.getBuffer(type);
            if (texture.isPresent()) newType = RenderType.entityTranslucent(texture.get());
            return new AlphaTintVertexConsumer(delegate.getBuffer(newType), alpha);
        }
    }
    private static class AlphaTintVertexConsumer implements VertexConsumer {
        private final VertexConsumer wrapped; private final float alpha;
        public AlphaTintVertexConsumer(VertexConsumer wrapped, float alpha) { this.wrapped = wrapped; this.alpha = alpha; }
        @Override public VertexConsumer addVertex(float x, float y, float z) { wrapped.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { wrapped.setColor(r, g, b, (int)(a * alpha)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { wrapped.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { wrapped.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { wrapped.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { wrapped.setNormal(x, y, z); return this; }
    }

    // ============================================================
    //  反射工具：从 RenderType 中提取 Texture
    // ============================================================
    private static Optional<ResourceLocation> getTextureFromRenderType(RenderType type) {
        try {
            // RenderType -> CompositeState
            if (compositeStateField == null) {
                // 同样注意混淆名，开发环境通常是 state
                Field[] fields = RenderType.class.getDeclaredFields();
                for (Field f : fields) {
                    // 这是一个简单的启发式查找，RenderType 只有一个 RenderType.CompositeState 类型的字段
                    if (f.getType().getName().contains("CompositeState")) {
                        compositeStateField = f;
                        compositeStateField.setAccessible(true);
                        break;
                    }
                }
            }
            if (compositeStateField == null) return Optional.empty();
            
            Object state = compositeStateField.get(type); // 获取 CompositeState 对象

            // CompositeState -> TextureStateShard
            if (textureStateField == null) {
                Field[] fields = state.getClass().getDeclaredFields();
                for (Field f : fields) {
                    if (RenderStateShard.TextureStateShard.class.isAssignableFrom(f.getType())) {
                        textureStateField = f;
                        textureStateField.setAccessible(true);
                        break;
                    }
                }
            }
            if (textureStateField == null) return Optional.empty();

            Object textureState = textureStateField.get(state); // 获取 TextureStateShard

            // TextureStateShard -> Optional<ResourceLocation> texture
            if (textureLocationField == null) {
                // TextureStateShard 父类是 RenderStateShard，texture 字段在 TextureStateShard 中
                // 或者是 protected final Optional<ResourceLocation> texture;
                Field[] fields = RenderStateShard.TextureStateShard.class.getDeclaredFields();
                for (Field f : fields) {
                    if (f.getType() == Optional.class) {
                        textureLocationField = f;
                        textureLocationField.setAccessible(true);
                        break;
                    }
                }
            }
            
            if (textureLocationField != null) {
                return (Optional<ResourceLocation>) textureLocationField.get(textureState);
            }

        } catch (Exception e) {
            // 假如反射失败，静默处理，返回空
        }
        return Optional.empty();
    }

}