package rwg.map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import rwg.util.CellNoise;
import rwg.util.ContinentalNoise;
import rwg.util.NoiseGenerator;
import rwg.world.ChunkManagerRealistic;

/** Chunk-local renderer for a deterministic lava chamber, its Voronoi tunnel network, and its surface vent. */
public final class LavaCaveLandmark {

    private static final float MAIN_RADIUS = 88f;
    private static final float NETWORK_RADIUS = 320f;
    private static final int CHAMBER_Y = 29;
    private static final int LAVA_LEVEL = 25;
    private static final float SURFACE_RADIUS = 65f;
    private static final float OPENING_RADIUS = 14f;
    private static final float MARKER_BIOME_RADIUS = 25f;

    private LavaCaveLandmark() {}

    public static float surfaceHeight(NoiseGenerator perlin, float localX, float localZ, float underlyingHeight) {
        float x = warpedX(perlin, localX, localZ);
        float z = warpedZ(perlin, localX, localZ);
        float distance = (float) Math.sqrt(x * x + z * z);
        if (distance >= SURFACE_RADIUS || underlyingHeight < 63f) return underlyingHeight;
        float cone = smoothstep(1f - distance / SURFACE_RADIUS) * 20f;
        float vent = smoothstep(1f - distance / (OPENING_RADIUS + 4f));
        float subtleRim = Math.max(0f, 1f - Math.abs(distance - (OPENING_RADIUS + 2f)) / 5f) * 2.5f;
        return underlyingHeight + cone * (1f - vent) + subtleRim;
    }

    public static boolean isSurfaceOpening(NoiseGenerator perlin, float localX, float localZ) {
        return warpedDistanceSquared(perlin, localX, localZ) < OPENING_RADIUS * OPENING_RADIUS;
    }

    public static boolean isMarkerBiome(NoiseGenerator perlin, float localX, float localZ) {
        return warpedDistanceSquared(perlin, localX, localZ) < MARKER_BIOME_RADIUS * MARKER_BIOME_RADIUS;
    }

    public static boolean isMainChamber(NoiseGenerator perlin, float localX, float localZ) {
        return warpedDistanceSquared(perlin, localX, localZ) < MAIN_RADIUS * MAIN_RADIUS;
    }

    private static float warpedDistanceSquared(NoiseGenerator perlin, float localX, float localZ) {
        float x = warpedX(perlin, localX, localZ);
        float z = warpedZ(perlin, localX, localZ);
        return x * x + z * z;
    }

    public static void decorateSurface(World world, ChunkManagerRealistic manager, NoiseGenerator perlin, int chunkX,
            int chunkZ, Block smolderingGrass) {
        if (smolderingGrass == null) return;
        for (int offsetX = 8; offsetX < 24; offsetX++) {
            int worldX = chunkX + offsetX;
            for (int offsetZ = 8; offsetZ < 24; offsetZ++) {
                int worldZ = chunkZ + offsetZ;
                long cave = manager.getLavaCaveCoordinates(worldX, worldZ);
                if (cave == Long.MIN_VALUE || !isMarkerBiome(
                        perlin,
                        ContinentalNoise.unpackVolcanoX(cave),
                        ContinentalNoise.unpackVolcanoY(cave)))
                    continue;
                float patch = perlin.noise2((worldX + 317f) / 7f, (worldZ - 491f) / 7f)
                        + perlin.noise2(worldX / 19f, worldZ / 19f) * .4f;
                if (patch <= .35f) continue;
                int surfaceY = world.getHeightValue(worldX, worldZ) - 1;
                if (surfaceY > 0 && world.getBlock(worldX, surfaceY, worldZ) == Blocks.grass) {
                    world.setBlock(worldX, surfaceY, worldZ, smolderingGrass, 1, 2);
                }
            }
        }
    }

    public static void generate(Block[] blocks, byte[] metadata, int chunkX, int chunkZ, ChunkManagerRealistic manager,
            NoiseGenerator perlin, CellNoise cell) {
        double[] cellSamples = new double[4];
        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkX * 16 + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkZ * 16 + localZ;
                if (manager.getVolcanoVicinityCoordinates(worldX, worldZ) != Long.MIN_VALUE) continue;
                long packed = manager.getLavaCaveCoordinates(worldX, worldZ);
                if (packed == Long.MIN_VALUE) continue;
                float landmarkX = ContinentalNoise.unpackVolcanoX(packed);
                float landmarkZ = ContinentalNoise.unpackVolcanoY(packed);
                carveColumn(
                        blocks,
                        metadata,
                        localX,
                        localZ,
                        worldX,
                        worldZ,
                        landmarkX,
                        landmarkZ,
                        perlin,
                        cell,
                        cellSamples);
            }
        }
    }

    private static void carveColumn(Block[] blocks, byte[] metadata, int localX, int localZ, int worldX, int worldZ,
            float landmarkX, float landmarkZ, NoiseGenerator perlin, CellNoise cell, double[] cellSamples) {
        float x = warpedX(perlin, landmarkX, landmarkZ);
        float z = warpedZ(perlin, landmarkX, landmarkZ);
        float distance = (float) Math.sqrt(x * x + z * z);
        boolean main = distance < MAIN_RADIUS;

        int column = (localX * 16 + localZ) * 256;
        int surface = findSurface(blocks, column);

        float networkScale = 108f;
        float networkWarpX = x + perlin.noise2((worldX + 1700f) / 130f, (worldZ - 900f) / 130f) * 24f;
        float networkWarpZ = z + perlin.noise2((worldX - 2300f) / 130f, (worldZ + 1100f) / 130f) * 24f;
        cell.sampleTwo2D(networkWarpX / networkScale, networkWarpZ / networkScale, 1D, cellSamples);
        float edgeDistance = (float) (cellSamples[2] - cellSamples[0]) * networkScale;
        float distanceFade = smoothstep(1f - distance / NETWORK_RADIUS);
        float tunnelRadius = 4f + distanceFade * 19f;
        boolean tunnel = distance < NETWORK_RADIUS && edgeDistance < tunnelRadius;
        if (!main && !tunnel) return;

        float tunnelCenter = CHAMBER_Y + perlin.noise2(worldX / 105f, worldZ / 105f) * 2.2f
                + perlin.noise2(worldX / 41f, worldZ / 41f) * 1.1f;
        float horizontal = edgeDistance / tunnelRadius;
        float tunnelHalfHeight = tunnelRadius * (float) Math.sqrt(Math.max(0f, 1f - horizontal * horizontal));
        float tunnelFloor = Math.min(tunnelCenter - tunnelHalfHeight, LAVA_LEVEL - 2f);
        float tunnelCeiling = Math.max(tunnelCenter + tunnelHalfHeight, LAVA_LEVEL + 4f);

        float roughness = perlin.noise2(worldX / 13f, worldZ / 13f) * 2.2f
                + perlin.noise2(worldX / 37f, worldZ / 37f) * 2.8f;
        float floor;
        float ceiling;
        int localLavaLevel;
        if (main) {
            float radial = Math.min(1f, distance / MAIN_RADIUS);
            float dome = 1f - smootherstep(radial);
            floor = 11f + smootherstep(radial) * 21f + roughness * .3f;
            float stalactiteNoise = perlin.noise2((worldX + 947f) / 8f, (worldZ - 613f) / 8f)
                    + perlin.noise2(worldX / 21f, worldZ / 21f) * .35f;
            float stalactite = (float) Math.pow(Math.max(0f, stalactiteNoise - .48f), 2D) * 16f;
            ceiling = Math.min(45f + dome * 29f + roughness, surface - 8f) - stalactite;
            localLavaLevel = LAVA_LEVEL;
            if (tunnel) {
                // Ease the Voronoi trough into the lake shore instead of cutting a vertical, lava-deep slot at the
                // final tunnel column.
                float channelProfile = (float) Math.sqrt(Math.max(0f, 1f - horizontal * horizontal));
                float channelBlend = smootherstep(channelProfile);
                floor += (Math.min(floor, tunnelFloor) - floor) * channelBlend;
                float tunnelRoof = Math.min(tunnelCeiling + roughness * .25f, surface - 8f);
                ceiling += (Math.max(ceiling, tunnelRoof) - ceiling) * channelBlend;
            }
        } else {
            floor = tunnelFloor;
            ceiling = tunnelCeiling + roughness * .25f;
            localLavaLevel = LAVA_LEVEL;
        }

        int bottom = Math.max(5, (int) Math.ceil(floor));
        int top = Math.min(Math.min(250, (int) Math.floor(ceiling)), surface - 2);
        if (top < bottom) return;
        for (int y = bottom; y <= top; y++) {
            int index = column + y;
            if (main && isBoulder(x, z, y)) {
                blocks[index] = Blocks.obsidian;
            } else {
                blocks[index] = y <= localLavaLevel ? Blocks.lava : Blocks.air;
            }
            metadata[index] = 0;
        }

        if (blocks[column + top + 1] instanceof BlockFalling) {
            blocks[column + top] = Blocks.stone;
            metadata[column + top] = 0;
        }

        float ceilingGlow = perlin.noise2((worldX - 823f) / 9f, (worldZ + 521f) / 9f)
                + perlin.noise2((worldX + 191f) / 24f, (worldZ - 337f) / 24f) * .45f;
        boolean glowstonePatch = main && distance < MAIN_RADIUS - 8f && ceilingGlow > .68f;
        if (glowstonePatch && top + 1 < surface) {
            blocks[column + top + 1] = Blocks.glowstone;
            metadata[column + top + 1] = 0;
        }

        // A spaced high-frequency sample creates plentiful isolated sources rather than broad lava patches.
        float drip = perlin.noise2((worldX + 411f) / 4f, (worldZ - 733f) / 4f);
        boolean isolatedLavaSource = (worldX & 1) == 0 && (worldZ & 1) == 0
                && Math.floorMod(coordinateHash(worldX, worldZ), 10) == 0
                && drip > .18f;
        if (!glowstonePatch && isolatedLavaSource && top + 2 < surface) {
            blocks[column + top + 1] = Blocks.lava;
            metadata[column + top + 1] = 0;
        }

        if (distance < 28f && surface >= 63 && surface > top) {
            int height = Math.max(1, surface - top);
            for (int y = top + 1; y <= surface; y++) {
                float vertical = (y - top) / (float) height;
                float funnelRadius;
                if (vertical < .58f) {
                    funnelRadius = 28f + (8f - 28f) * smootherstep(vertical / .58f);
                } else {
                    funnelRadius = 8f + (OPENING_RADIUS - 8f) * smootherstep((vertical - .58f) / .42f);
                }
                if (distance < funnelRadius) {
                    blocks[column + y] = Blocks.air;
                    metadata[column + y] = 0;
                }
            }
        }
    }

    private static int coordinateHash(int x, int z) {
        int hash = x * 0x1f1f1f1f ^ z * 0x45d9f3b;
        hash ^= hash >>> 16;
        hash *= 0x45d9f3b;
        return hash ^ hash >>> 16;
    }

    private static int findSurface(Block[] blocks, int column) {
        int surface = 255;
        while (surface > 1 && (blocks[column + surface] == Blocks.air || blocks[column + surface] == null)) surface--;
        return surface;
    }

    private static boolean isBoulder(float x, float z, int y) {
        return ellipsoid(x + 19f, z - 7f, y - 26f, 7f, 6f) || ellipsoid(x - 14f, z + 18f, y - 25f, 6f, 5f)
                || ellipsoid(x - 27f, z - 11f, y - 24f, 5f, 6f)
                || ellipsoid(x + 5f, z + 25f, y - 25f, 4.5f, 5f);
    }

    private static boolean ellipsoid(float x, float z, float y, float radius, float height) {
        return (x * x + z * z) / (radius * radius) + y * y / (height * height) <= 1f;
    }

    private static float warpedX(NoiseGenerator perlin, float x, float z) {
        float anchor = smoothstep((float) Math.sqrt(x * x + z * z) / 48f);
        return x + (perlin.noise2((x + 1300f) / 175f, (z - 700f) / 175f) * 34f
                + perlin.noise2((x - 170f) / 58f, (z + 290f) / 58f) * 14f) * anchor;
    }

    private static float warpedZ(NoiseGenerator perlin, float x, float z) {
        float anchor = smoothstep((float) Math.sqrt(x * x + z * z) / 48f);
        return z + (perlin.noise2((x - 2100f) / 175f, (z + 1900f) / 175f) * 34f
                + perlin.noise2((x + 370f) / 58f, (z - 510f) / 58f) * 14f) * anchor;
    }

    private static float smoothstep(float value) {
        value = Math.max(0f, Math.min(1f, value));
        return value * value * (3f - 2f * value);
    }

    private static float smootherstep(float value) {
        value = Math.max(0f, Math.min(1f, value));
        return value * value * value * (value * (value * 6f - 15f) + 10f);
    }
}
