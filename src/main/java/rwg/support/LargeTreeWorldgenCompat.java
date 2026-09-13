package rwg.support;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

/** Ensures exceptionally wide modded trees cannot write into terrain that has not been generated yet. */
public final class LargeTreeWorldgenCompat {

    private static final int MAXIMUM_TREE_RADIUS = 32;
    private static final ThreadLocal<Integer> DECORATION_DEPTH = new ThreadLocal<Integer>() {

        @Override
        protected Integer initialValue() {
            return 0;
        }
    };
    private static final ThreadLocal<Boolean> GENERATING_FOOTPRINT = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private LargeTreeWorldgenCompat() {}

    public static void beginBiomeDecoration() {
        DECORATION_DEPTH.set(DECORATION_DEPTH.get() + 1);
    }

    public static void endBiomeDecoration() {
        int depth = DECORATION_DEPTH.get() - 1;
        if (depth <= 0) {
            DECORATION_DEPTH.remove();
        } else {
            DECORATION_DEPTH.set(depth);
        }
    }

    public static void prepareFootprint(World world, int x, int z) {
        if (DECORATION_DEPTH.get() == 0 || GENERATING_FOOTPRINT.get()) return;

        GENERATING_FOOTPRINT.set(true);
        try {
            IChunkProvider provider = world.getChunkProvider();
            int minimumChunkX = x - MAXIMUM_TREE_RADIUS >> 4;
            int maximumChunkX = x + MAXIMUM_TREE_RADIUS >> 4;
            int minimumChunkZ = z - MAXIMUM_TREE_RADIUS >> 4;
            int maximumChunkZ = z + MAXIMUM_TREE_RADIUS >> 4;
            for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
                for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                    provider.provideChunk(chunkX, chunkZ);
                }
            }
        } finally {
            GENERATING_FOOTPRINT.remove();
        }
    }
}
