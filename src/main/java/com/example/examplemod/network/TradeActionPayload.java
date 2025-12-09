package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.server.menu.TradeMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// TradeActionPayload.java
public record TradeActionPayload(boolean locked) implements CustomPacketPayload {
    public static final Type<TradeActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "trade_action"));
    
    // 1.21.1 使用 StreamCodec
    public static final StreamCodec<ByteBuf, TradeActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, TradeActionPayload::locked,
        TradeActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // 服务端处理逻辑
    public static void handleServer(final TradeActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof TradeMenu menu) {
                menu.setPlayerLocked(context.player(), payload.locked);
            }
        });
    }
}