package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.resources.ResourceLocation;

public record UpdateItemAttributesPayload(ItemAttributeModifiers modifiers) implements CustomPacketPayload {

    // 定义包的唯一ID
    public static final Type<UpdateItemAttributesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "update_item_attributes"));

    // 定义如何序列化/反序列化 (直接使用 MC 原生的 StreamCodec)
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateItemAttributesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemAttributeModifiers.STREAM_CODEC,
                    UpdateItemAttributesPayload::modifiers,
                    UpdateItemAttributesPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}