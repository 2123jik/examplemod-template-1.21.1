package com.example.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client {
    public static List<BlockPos> findBlocksAroundPlayer(Player player, Level level, int radius, Block block) {
        List<BlockPos> chestPositions = new CopyOnWriteArrayList<>();
        AABB searchArea = new AABB(player.blockPosition()).inflate(radius);
        BlockPos.betweenClosedStream(searchArea).forEach(currentPos -> {
            if (level.getBlockState(currentPos).is(block)) {
                chestPositions.add(currentPos.immutable());
            }
        });
        return chestPositions;
    }
}
