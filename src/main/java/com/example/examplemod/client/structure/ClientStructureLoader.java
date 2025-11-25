package com.example.examplemod.client.structure;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.InputStream;
import java.util.Optional;

public class ClientStructureLoader {

    public static StructureTemplate loadStructure(ResourceLocation structureId) {
        Minecraft mc = Minecraft.getInstance();
        Optional<Resource> resource = mc.getResourceManager().getResource(structureId);
        if (resource.isEmpty()) {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                ResourceManager serverManager = server.getResourceManager();
                resource = serverManager.getResource(structureId);
            }
        }

        try (InputStream inputStream = resource.get().open()) {

            CompoundTag nbt = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());

            StructureTemplate template = new StructureTemplate();
            HolderGetter<Block> blockGetter = mc.level.registryAccess()
                    .registryOrThrow(Registries.BLOCK)
                    .asLookup();

            template.load(blockGetter, nbt);
            return template;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}