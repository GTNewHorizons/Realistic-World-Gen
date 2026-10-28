package rwg2.support;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import cpw.mods.fml.common.FMLLog;

/** Temporary, passive diagnostics for cross-chunk trees and villages. Never loads a missing chunk. */
public final class WorldgenDiagnostics {

    private static final String PREFIX = "[RWG-WORLDGEN-DIAG] ";
    private static final ThreadLocal<TreeTrace> ACTIVE_TREE = new ThreadLocal<TreeTrace>();

    private WorldgenDiagnostics() {}

    public static void beginTree(String type, World world, int x, int y, int z) {
        if (world.isRemote) return;
        TreeTrace previous = ACTIVE_TREE.get();
        if (previous != null) {
            FMLLog.warning(
                    PREFIX + "nested tree trace; replacing unfinished %s at %d,%d,%d",
                    previous.type,
                    previous.originX,
                    previous.originY,
                    previous.originZ);
        }
        ACTIVE_TREE.set(new TreeTrace(type, world, x, y, z));
    }

    public static void recordTreeWriteAttempt(World world, int x, int y, int z) {
        TreeTrace trace = ACTIVE_TREE.get();
        if (trace == null || trace.world != world) return;
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long key = chunkKey(chunkX, chunkZ);
        if (trace.chunks.add(key) && !world.getChunkProvider().chunkExists(chunkX, chunkZ)) {
            trace.initiallyMissingChunks.add(key);
        }
        trace.minX = Math.min(trace.minX, x);
        trace.minY = Math.min(trace.minY, y);
        trace.minZ = Math.min(trace.minZ, z);
        trace.maxX = Math.max(trace.maxX, x);
        trace.maxY = Math.max(trace.maxY, y);
        trace.maxZ = Math.max(trace.maxZ, z);
        trace.attemptedWrites++;
    }

    public static void recordTreeWrite(int x, int y, int z, boolean succeeded) {
        TreeTrace trace = ACTIVE_TREE.get();
        if (trace != null && !succeeded) trace.failedWrites++;
    }

    public static void endTree(boolean generated) {
        TreeTrace trace = ACTIVE_TREE.get();
        if (trace == null) return;
        ACTIVE_TREE.remove();
        FMLLog.info(
                PREFIX + "tree type=%s origin=%d,%d,%d originChunk=%d,%d generated=%s writes=%d failedWrites=%d "
                        + "bounds=%s touchedChunks=%s initiallyMissingChunks=%s",
                trace.type,
                trace.originX,
                trace.originY,
                trace.originZ,
                trace.originX >> 4,
                trace.originZ >> 4,
                generated,
                trace.attemptedWrites,
                trace.failedWrites,
                trace.bounds(),
                formatChunks(trace.chunks),
                formatChunks(trace.initiallyMissingChunks));
    }

    public static void recordVillageStart(World world, int originChunkX, int originChunkZ, StructureStart start) {
        if (world == null || world.isRemote || start == null || start.getBoundingBox() == null) return;
        StructureBoundingBox box = start.getBoundingBox();
        Set<Long> previouslyPopulated = new HashSet<Long>();
        IChunkProvider provider = world.getChunkProvider();
        for (int chunkX = box.minX >> 4; chunkX <= box.maxX >> 4; chunkX++) {
            for (int chunkZ = box.minZ >> 4; chunkZ <= box.maxZ >> 4; chunkZ++) {
                if (provider.chunkExists(chunkX, chunkZ)) {
                    Chunk chunk = provider.provideChunk(chunkX, chunkZ);
                    if (chunk.isTerrainPopulated) previouslyPopulated.add(chunkKey(chunkX, chunkZ));
                }
            }
        }
        FMLLog.info(
                PREFIX + "village discovered originChunk=%d,%d bounds=%s components=%d alreadyPopulated=%s",
                originChunkX,
                originChunkZ,
                formatBox(box),
                start.getComponents().size(),
                formatChunks(previouslyPopulated));
        if (!previouslyPopulated.isEmpty()) {
            FMLLog.warning(
                    PREFIX + "village %d,%d was discovered after intersecting chunks had populated: %s",
                    originChunkX,
                    originChunkZ,
                    formatChunks(previouslyPopulated));
        }
        for (Object value : start.getComponents()) {
            StructureComponent component = (StructureComponent) value;
            FMLLog.info(
                    PREFIX + "village component originChunk=%d,%d type=%s bounds=%s",
                    originChunkX,
                    originChunkZ,
                    component.getClass().getName(),
                    formatBox(component.getBoundingBox()));
        }
    }

    public static void recordVillageEmission(World world, int chunkX, int chunkZ) {
        if (world.isRemote) return;
        FMLLog.info(PREFIX + "village emitted chunk=%d,%d dimension=%d", chunkX, chunkZ, world.provider.dimensionId);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX & 0xffffffffL | ((long) chunkZ & 0xffffffffL) << 32;
    }

    private static String formatChunks(Set<Long> chunks) {
        StringBuilder result = new StringBuilder("[");
        boolean first = true;
        for (Long key : chunks) {
            if (!first) result.append(' ');
            result.append((int) (key.longValue() & 0xffffffffL));
            result.append(',');
            result.append((int) (key.longValue() >>> 32));
            first = false;
        }
        return result.append(']').toString();
    }

    private static String formatBox(StructureBoundingBox box) {
        if (box == null) return "null";
        return box.minX + "," + box.minY + "," + box.minZ + ".." + box.maxX + "," + box.maxY + "," + box.maxZ;
    }

    private static final class TreeTrace {

        private final String type;
        private final World world;
        private final int originX;
        private final int originY;
        private final int originZ;
        private final Set<Long> chunks = new HashSet<Long>();
        private final Set<Long> initiallyMissingChunks = new HashSet<Long>();
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int maxZ = Integer.MIN_VALUE;
        private int attemptedWrites;
        private int failedWrites;

        private TreeTrace(String type, World world, int x, int y, int z) {
            this.type = type;
            this.world = world;
            this.originX = x;
            this.originY = y;
            this.originZ = z;
        }

        private String bounds() {
            return attemptedWrites == 0 ? "none"
                    : minX + "," + minY + "," + minZ + ".." + maxX + "," + maxY + "," + maxZ;
        }
    }
}
