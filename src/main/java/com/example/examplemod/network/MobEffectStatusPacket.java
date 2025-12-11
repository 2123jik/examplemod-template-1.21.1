package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.FriendlyByteBuf; // 引用您提供的类
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

import static com.example.examplemod.ExampleMod.loc;

public record MobEffectStatusPacket(
        UUID entityUuid,
        ResourceLocation effectId,
        MobEffectStatusPacket.Action action
) implements CustomPacketPayload {

    // Type 和 Codec 定义不变
    public static final CustomPacketPayload.Type<MobEffectStatusPacket> TYPE =new Type<>(loc( "effect_status_sync"));
    public static final StreamCodec<FriendlyByteBuf, MobEffectStatusPacket> STREAM_CODEC = StreamCodec.of(
            MobEffectStatusPacket::encode, // 更改为静态编码方法
            MobEffectStatusPacket::new     // 引用解码构造器 (B -> T)
    );

    public static enum Action {
        ADD,
        REMOVE;
        // 使用 FriendlyByteBuf 的 readEnum/writeEnum 方法
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.of(
                // Encoder: (FriendlyByteBuf buffer, Action action) -> buffer.writeEnum(action)
                FriendlyByteBuf::writeEnum,
                // Decoder: (FriendlyByteBuf buffer) -> buffer.readEnum(Action.class)
                (buf) -> buf.readEnum(Action.class)
        );
    }

    public MobEffectStatusPacket(final FriendlyByteBuf buffer) {
        this(
                buffer.readUUID(),
                buffer.readResourceLocation(),
                Action.STREAM_CODEC.decode(buffer)
        );
    }

    public static void encode(FriendlyByteBuf buffer, MobEffectStatusPacket payload) {
        buffer.writeUUID(payload.entityUuid());
        buffer.writeResourceLocation(payload.effectId());
        Action.STREAM_CODEC.encode(buffer, payload.action());

    }
    @Override
    public CustomPacketPayload.Type<MobEffectStatusPacket> type() {
        return TYPE;
    }
}