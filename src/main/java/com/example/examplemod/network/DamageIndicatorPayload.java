package com.example.examplemod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DamageIndicatorPayload(float amount, int critType, int layers) implements CustomPacketPayload {

    public static final Type<DamageIndicatorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "dmg_indicator"));

    public static final StreamCodec<ByteBuf, DamageIndicatorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DamageIndicatorPayload::amount,
            ByteBufCodecs.INT, DamageIndicatorPayload::critType,
            ByteBufCodecs.INT, DamageIndicatorPayload::layers,
            DamageIndicatorPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}