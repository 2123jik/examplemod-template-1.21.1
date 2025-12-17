package com.example.examplemod.client.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CustomScaleConfig {
    public static final CustomScaleConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        Pair<CustomScaleConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(CustomScaleConfig::new);
        SPEC = pair.getRight();
        INSTANCE = pair.getLeft();
    }

    public final ModConfigSpec.DoubleValue scaleValue;

    // 构造函数，这里定义配置项
    public CustomScaleConfig(ModConfigSpec.Builder builder) {
        builder.push("general");

        scaleValue = builder
                .comment("GUI Scale override. Set to -1 to use vanilla logic. Set to 2.5, 3.0 etc for custom scale.")
                // 注意：min 必须小于等于默认值 -1，这里设为 -1.0 到 10.0
                .defineInRange("scale", -1.0, -1.0, 10.0);

        builder.pop();
    }
}