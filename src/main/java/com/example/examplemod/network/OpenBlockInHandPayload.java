package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenBlockInHandPayload(ResourceLocation blockId) implements CustomPacketPayload {
    public static final Type<OpenBlockInHandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "open_block_in_hand"));

    // 需要 RegistryFriendlyByteBuf 因为 ResourceLocation 可能涉及注册表 (虽然这里是纯ID，但也无妨)
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBlockInHandPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, OpenBlockInHandPayload::blockId,
            OpenBlockInHandPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}