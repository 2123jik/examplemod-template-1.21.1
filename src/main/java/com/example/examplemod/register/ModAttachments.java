package com.example.examplemod.register;

import com.example.examplemod.ExampleMod;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ExampleMod.MODID);

    // 存储施法者的 UUID。使用 Optional 处理空值情况。
    public static final Supplier<AttachmentType<Optional<UUID>>> LAST_ATTACKER_UUID = ATTACHMENT_TYPES.register(
            "last_attacker_uuid",
            () -> AttachmentType.builder(() -> Optional.<UUID>empty())
                    .serialize(Codec.STRING.xmap(
                            s -> s.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(s)),
                            uuid -> uuid.map(UUID::toString).orElse("")
                    ))
                    .copyOnDeath() // 推荐开启，防止玩家死亡重置后丢失归属信息
                    .build()
    );
}