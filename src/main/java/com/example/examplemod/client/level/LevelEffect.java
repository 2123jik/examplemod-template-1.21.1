package com.example.examplemod.client.level;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = ExampleMod.MODID,value = Dist.CLIENT)
public class LevelEffect {
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        final Minecraft mc = Minecraft.getInstance();
        final Tesselator tesselator = Tesselator.getInstance();
        final Level level = mc.level;
        final Camera camera=event.getCamera();
        final double time= Blaze3D.getTime();
        final Matrix4f mvm=event.getModelViewMatrix();
        final Matrix4f pm=event.getProjectionMatrix();
        final Vector3f pos=new Vector3f(0,100,0);

    }
}
