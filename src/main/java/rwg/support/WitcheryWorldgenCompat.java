package rwg.support;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import rwg.biomes.realistic.land.RealisticBiomeMountainChain;
import rwg.util.ContinentalNoise;
import rwg.world.ChunkManagerRealistic;

/** Placement checks shared by the optional Witchery world-generation mixin. */
public final class WitcheryWorldgenCompat {

    private static final int OPENING_MARGIN = 8;
    private static final int LANDMARK_SAMPLE_SPACING = 4;
    private static final float LAVA_CAVE_SURFACE_RADIUS = 70f;
    private static final float SKYLIGHT_THRESHOLD = 0.70f;
    private static final int MAX_FOUNDATION_HEIGHT_VARIATION = 12;
    private static final int FOUNDATION_DEPTH = 12;
    private static final int MIN_SOLID_FOUNDATION_BLOCKS = 6;

    private WitcheryWorldgenCompat() {}

    public static boolean intersectsProtectedTerrain(World world, StructureBoundingBox bounds) {
        if (!(world.getWorldChunkManager() instanceof ChunkManagerRealistic)) return false;
        if (hasUnsuitableFoundation(world, bounds)) return true;

        ChunkManagerRealistic manager = (ChunkManagerRealistic) world.getWorldChunkManager();
        if (intersectsLandmark(manager, bounds)) return true;

        for (int sampleZ = bounds.minZ - OPENING_MARGIN; sampleZ <= bounds.maxZ + OPENING_MARGIN; sampleZ++) {
            for (int sampleX = bounds.minX - OPENING_MARGIN; sampleX <= bounds.maxX + OPENING_MARGIN; sampleX++) {
                if (manager.getRiverJunctionStrength(sampleX, sampleZ) >= SKYLIGHT_THRESHOLD
                        && hasMountainChainNearby(manager, sampleX, sampleZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasUnsuitableFoundation(World world, StructureBoundingBox bounds) {
        int minimumHeight = Integer.MAX_VALUE;
        int maximumHeight = Integer.MIN_VALUE;
        int sampledColumns = 0;
        int unsupportedColumns = 0;
        for (int z = bounds.minZ; z <= bounds.maxZ; z += 2) {
            for (int x = bounds.minX; x <= bounds.maxX; x += 2) {
                int top = world.getTopSolidOrLiquidBlock(x, z);
                minimumHeight = Math.min(minimumHeight, top);
                maximumHeight = Math.max(maximumHeight, top);
                sampledColumns++;

                int solidBlocks = 0;
                for (int y = top - 1; y >= Math.max(1, top - FOUNDATION_DEPTH); y--) {
                    if (world.getBlock(x, y, z).getMaterial().isSolid()) solidBlocks++;
                }
                if (solidBlocks < MIN_SOLID_FOUNDATION_BLOCKS) unsupportedColumns++;
            }
        }
        return maximumHeight - minimumHeight > MAX_FOUNDATION_HEIGHT_VARIATION
                || unsupportedColumns * 3 > sampledColumns;
    }

    private static boolean intersectsLandmark(ChunkManagerRealistic manager, StructureBoundingBox bounds) {
        int minX = bounds.minX - OPENING_MARGIN;
        int maxX = bounds.maxX + OPENING_MARGIN;
        int minZ = bounds.minZ - OPENING_MARGIN;
        int maxZ = bounds.maxZ + OPENING_MARGIN;
        for (int z = minZ; z <= maxZ; z += LANDMARK_SAMPLE_SPACING) {
            for (int x = minX; x <= maxX; x += LANDMARK_SAMPLE_SPACING) {
                if (isProtectedLandmark(manager, x, z)) return true;
            }
        }
        return isProtectedLandmark(manager, maxX, maxZ);
    }

    private static boolean isProtectedLandmark(ChunkManagerRealistic manager, int x, int z) {
        if (manager.getVolcanoVicinityCoordinates(x, z) != Long.MIN_VALUE) return true;
        long cave = manager.getLavaCaveCoordinates(x, z);
        if (cave == Long.MIN_VALUE) return false;
        float caveX = ContinentalNoise.unpackVolcanoX(cave);
        float caveZ = ContinentalNoise.unpackVolcanoY(cave);
        return caveX * caveX + caveZ * caveZ <= LAVA_CAVE_SURFACE_RADIUS * LAVA_CAVE_SURFACE_RADIUS;
    }

    private static boolean hasMountainChainNearby(ChunkManagerRealistic manager, int x, int z) {
        for (int offsetZ = -32; offsetZ <= 32; offsetZ += 16) {
            for (int offsetX = -32; offsetX <= 32; offsetX += 16) {
                if (manager.getBiomeDataAt(x + offsetX, z + offsetZ) instanceof RealisticBiomeMountainChain) {
                    return true;
                }
            }
        }
        return false;
    }
}
