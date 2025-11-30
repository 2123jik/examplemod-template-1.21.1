package com.example.examplemod.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class InventoryModelRenderer {

    // 按键绑定
    public static final KeyMapping OPEN_EDITOR_KEY = new KeyMapping(
            "key.examplemod.open_editor",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // 默认 K 键打开
            "key.categories.examplemod"
    );

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR_KEY);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_EDITOR_KEY.consumeClick()) {
//            Minecraft.getInstance().setScreen(new ProModelEditorScreen());
        }
    }

    // ==========================================
    // 配置类 (保持不变，方便存储)
    // ==========================================
    public static class LayerConfig {
        public static final LayerConfig INSTANCE;
        public static final ModConfigSpec SPEC;

        static {
            Pair<LayerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(LayerConfig::new);
            SPEC = pair.getRight();
            INSTANCE = pair.getLeft();
        }

        public static class LayerParams {
            public final ModConfigSpec.DoubleValue transX, transY, transZ;
            public final ModConfigSpec.DoubleValue rotX, rotY, rotZ;
            public final ModConfigSpec.DoubleValue scaleX, scaleY, scaleZ;
            public final ModConfigSpec.ConfigValue<String> bone;

            public LayerParams(ModConfigSpec.Builder builder, int id) {
                builder.push("Layer_" + id);
                transX = builder.defineInRange("transX", 0.0, -50.0, 50.0); // 扩大范围方便调试
                transY = builder.defineInRange("transY", 0.0, -50.0, 50.0);
                transZ = builder.defineInRange("transZ", 0.0, -50.0, 50.0);
                rotX = builder.defineInRange("rotX", 0.0, -360.0, 360.0);
                rotY = builder.defineInRange("rotY", 0.0, -360.0, 360.0);
                rotZ = builder.defineInRange("rotZ", 0.0, -360.0, 360.0);
                scaleX = builder.defineInRange("scaleX", 1.0, 0.01, 10.0);
                scaleY = builder.defineInRange("scaleY", 1.0, 0.01, 10.0);
                scaleZ = builder.defineInRange("scaleZ", 1.0, 0.01, 10.0);
                bone = builder.define("bone", "none");
                builder.pop();
            }
        }

        public final List<LayerParams> layers = new ArrayList<>();

        LayerConfig(ModConfigSpec.Builder builder) {
            builder.comment("Render Layer Settings").push("Layers");
            // 定义 6 层，足够用了
            for(int i=1; i<=6; i++) layers.add(new LayerParams(builder, i));
            builder.pop();
        }
    }

    public static class ClientConfig {
        public static final ClientConfig INSTANCE;
        public static final ModConfigSpec SPEC;

        static {
            Pair<ClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ClientConfig::new);
            SPEC = pair.getRight();
            INSTANCE = pair.getLeft();
        }

        public final ModConfigSpec.IntValue xOffset;
        public final ModConfigSpec.IntValue yOffset;
        public final ModConfigSpec.IntValue width;
        public final ModConfigSpec.IntValue height;
        public final ModConfigSpec.DoubleValue scale;
        public final ModConfigSpec.IntValue selectorWidth;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("InventoryCustomModel");
            xOffset = builder.comment("Window X Offset").defineInRange("xOffset", -90, -2000, 2000);
            yOffset = builder.comment("Window Y Offset").defineInRange("yOffset", 10, -2000, 2000);
            width = builder.comment("Window Width").defineInRange("width", 80, 50, 1000);
            height = builder.comment("Window Height").defineInRange("height", 120, 60, 1000);
            scale = builder.comment("Model Scale").defineInRange("scale", 30.0, 5.0, 300.0);
            selectorWidth = builder.comment("Selector List Width").defineInRange("selectorWidth", 100, 60, 400);
            builder.pop();
        }
    }
}