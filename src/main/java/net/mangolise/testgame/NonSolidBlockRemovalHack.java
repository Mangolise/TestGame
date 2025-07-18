package net.mangolise.testgame;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.polar.PolarWorld;
import net.mangolise.gamesdk.instance.InstanceAnalysis;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.List;
import java.util.function.BiConsumer;

public class NonSolidBlockRemovalHack {
    
    public static void apply(PolarWorld world, Instance instance) {
        Int2ObjectMap<List<Point>> analysis = InstanceAnalysis.analyse(world);

        forEachRemovalBlock(analysis, (point, block) -> {
            instance.setBlock(point, Block.AIR);
        });
        
        instance.eventNode().addListener(PlayerChunkLoadEvent.class, event -> {
            int chunkX = event.getChunkX();
            int chunkZ = event.getChunkZ();

            forEachRemovalBlock(analysis, (point, block) -> {
                if (point.chunkX() != chunkX || point.chunkZ() != chunkZ) return;
                
                // set the block for the player
                event.getPlayer().sendPacket(new BlockChangePacket(point, block));
            });
        });
    }
    
    private static void forEachRemovalBlock(Int2ObjectMap<List<Point>> data, BiConsumer<Point, Block> consumer) {
        for (Int2ObjectMap.Entry<List<Point>> entry : data.int2ObjectEntrySet()) {
            Block block = Block.fromStateId(entry.getIntKey());
            if (!block.compare(Block.BARRIER) && block.isSolid()) continue;
            
            List<Point> points = entry.getValue();
            for (Point point : points) {
                consumer.accept(point, block);
            }
        }
    }
}
