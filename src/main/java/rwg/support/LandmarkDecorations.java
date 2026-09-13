package rwg.support;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import cpw.mods.fml.common.Loader;
import eu.usrv.legacylootgames.blocks.DungeonBrick;
import ganymedes01.etfuturum.ModBlocks;
import mods.natura.worldgen.GlowshroomGenBlueGreen;
import mods.natura.worldgen.GlowshroomGenPurple;
import ru.timeconqueror.lootgames.registry.LGBlocks;
import rwg.biomes.realistic.land.RealisticBiomeMountainChain;
import rwg.map.LavaCaveLandmark;
import rwg.util.ContinentalNoise;
import rwg.util.NoiseGenerator;
import rwg.world.ChunkManagerRealistic;
import twilightforest.block.TFBlocks;

/** Optional mod-backed centerpieces and flora for RWG's underground landmarks. */
public final class LandmarkDecorations {

    private static final int LAVA_LEVEL = 25;
    private static final int LAVA_PILLAR_TOP = 37;
    private static final int RIVER_PILLAR_TOP = 64;

    private final Block deepslate;
    private final Block puzzleMaster;
    private final Block twilightPortal;
    private final WorldGenerator[] glowshrooms;

    private LandmarkDecorations(Block deepslate, Block puzzleMaster, Block twilightPortal,
            WorldGenerator[] glowshrooms) {
        this.deepslate = deepslate;
        this.puzzleMaster = puzzleMaster;
        this.twilightPortal = twilightPortal;
        this.glowshrooms = glowshrooms;
    }

    public static LandmarkDecorations create() {
        if (!Loader.isModLoaded("etfuturum")) return null;
        Block lootGame = Loader.isModLoaded("lootgames") ? LGBlocks.PUZZLE_MASTER : null;
        Block twilight = Loader.isModLoaded("TwilightForest") ? TFBlocks.portal : null;
        WorldGenerator[] naturaGlowshrooms = Loader.isModLoaded("Natura")
                ? new WorldGenerator[] { new GlowshroomGenBlueGreen(false), new GlowshroomGenPurple(false) }
                : null;
        Block deep = ModBlocks.DEEPSLATE.get();
        return new LandmarkDecorations(deep == null ? Blocks.stone : deep, lootGame, twilight, naturaGlowshrooms);
    }

    public void decorate(World world, ChunkManagerRealistic manager, NoiseGenerator perlin, int chunkX, int chunkZ) {
        activateCeilingLavaSources(world, manager, chunkX, chunkZ);
        decorateLavaShore(world, manager, perlin, chunkX, chunkZ);
        decorateLavaCenter(world, manager, chunkX, chunkZ);
        decorateRiverJunction(world, manager, chunkX, chunkZ);
    }

    private static void activateCeilingLavaSources(World world, ChunkManagerRealistic manager, int chunkX, int chunkZ) {
        for (int offsetX = 8; offsetX < 24; offsetX++) {
            int x = chunkX + offsetX;
            for (int offsetZ = 8; offsetZ < 24; offsetZ++) {
                int z = chunkZ + offsetZ;
                if (manager.getLavaCaveCoordinates(x, z) == Long.MIN_VALUE) continue;
                for (int y = 26; y < 96; y++) {
                    if (world.getBlock(x, y, z) != Blocks.lava || !world.isAirBlock(x, y - 1, z)) continue;
                    world.setBlock(x, y, z, Blocks.flowing_lava, 0, 2);
                    world.scheduleBlockUpdate(x, y, z, Blocks.flowing_lava, Blocks.flowing_lava.tickRate(world));
                }
            }
        }
    }

    private void decorateLavaCenter(World world, ChunkManagerRealistic manager, int chunkX, int chunkZ) {
        long center = manager.getLavaCaveCenterCoordinates(chunkX + 8, chunkZ + 8);
        if (center == Long.MIN_VALUE) return;
        int centerX = (int) (center >> 32);
        int centerZ = (int) center;
        if (Math.floorDiv(centerX, 16) * 16 != chunkX || Math.floorDiv(centerZ, 16) * 16 != chunkZ
                || manager.getVolcanoVicinityCoordinates(centerX, centerZ) != Long.MIN_VALUE)
            return;

        long choice = mix(world.getSeed() ^ center ^ 0xD1B54A32D192ED03L);
        if ((choice & 3L) == 0L) {
            placeRoundedPillar(world, centerX, centerZ, 9, LAVA_PILLAR_TOP, 6, deepslate, false);
            placeNetherPortal(world, centerX, LAVA_PILLAR_TOP + 1, centerZ, (choice & 4L) != 0L);
        } else if (puzzleMaster != null) {
            placeRoundedPillar(world, centerX, centerZ, 9, LAVA_PILLAR_TOP, 6, deepslate, true);
            world.setBlock(centerX, LAVA_PILLAR_TOP + 2, centerZ, puzzleMaster, 0, 2);
        } else {
            placeRoundedPillar(world, centerX, centerZ, 9, LAVA_PILLAR_TOP, 6, deepslate, false);
        }
    }

    private void decorateRiverJunction(World world, ChunkManagerRealistic manager, int chunkX, int chunkZ) {
        int[] center = findJunctionCenter(manager, chunkX, chunkZ);
        if (center == null || Math.floorDiv(center[0], 16) * 16 != chunkX
                || Math.floorDiv(center[1], 16) * 16 != chunkZ)
            return;
        if (world.getBlock(center[0], 62, center[1]).getMaterial() != net.minecraft.block.material.Material.water
                || !world.isAirBlock(center[0], 65, center[1]))
            return;
        long key = (long) center[0] << 32 ^ center[1] & 0xffffffffL;
        long choice = mix(world.getSeed() ^ key ^ 0x94D049BB133111EBL);
        if ((choice & 1L) != 0L) return;

        if ((choice >>> 1 & 3L) == 0L && twilightPortal != null) {
            placeSquarePillar(world, center[0], center[1], 40, RIVER_PILLAR_TOP, 8, deepslate);
            placeTwilightPortal(world, center[0], RIVER_PILLAR_TOP, center[1]);
        } else if (puzzleMaster != null) {
            placeRoundedPillar(world, center[0], center[1], 40, RIVER_PILLAR_TOP, 2, deepslate, true);
            world.setBlock(center[0], RIVER_PILLAR_TOP + 2, center[1], puzzleMaster, 0, 2);
        }
    }

    private void decorateLavaShore(World world, ChunkManagerRealistic manager, NoiseGenerator perlin, int chunkX,
            int chunkZ) {
        if (glowshrooms == null) return;
        for (int offsetX = 8; offsetX < 24; offsetX++) {
            int x = chunkX + offsetX;
            for (int offsetZ = 8; offsetZ < 24; offsetZ++) {
                int z = chunkZ + offsetZ;
                long cave = manager.getLavaCaveCoordinates(x, z);
                if (cave == Long.MIN_VALUE || !LavaCaveLandmark.isMainChamber(
                        perlin,
                        ContinentalNoise.unpackVolcanoX(cave),
                        ContinentalNoise.unpackVolcanoY(cave)))
                    continue;
                float patch = perlin.noise2((x + 719f) / 8f, (z - 283f) / 8f) + perlin.noise2(x / 23f, z / 23f) * .3f;
                if (patch < .62f) continue;
                long placement = mix(world.getSeed() ^ (long) x * 0x9E3779B97F4A7C15L ^ (long) z * 0xC2B2AE3D27D4EB4FL);
                if ((placement & 63L) != 0L) continue;
                int floor = findCaveFloor(world, x, z);
                if (floor >= LAVA_LEVEL && floor <= 34 && world.isAirBlock(x, floor + 1, z)) {
                    Block substrate = world.getBlock(x, floor, z);
                    int substrateMetadata = world.getBlockMetadata(x, floor, z);
                    world.setBlock(x, floor, z, Blocks.netherrack, 0, 2);
                    Random random = new Random(placement);
                    WorldGenerator glowshroom = glowshrooms[(int) (placement >>> 6 & 1L)];
                    if (!glowshroom.generate(world, random, x, floor + 1, z)) {
                        world.setBlock(x, floor, z, substrate, substrateMetadata, 2);
                    }
                }
            }
        }
    }

    private static int findCaveFloor(World world, int x, int z) {
        for (int y = 44; y >= LAVA_LEVEL; y--) {
            if (world.getBlock(x, y, z).getMaterial().isSolid() && world.isAirBlock(x, y + 1, z)) return y;
        }
        return -1;
    }

    private static int[] findJunctionCenter(ChunkManagerRealistic manager, int chunkX, int chunkZ) {
        int bestX = chunkX + 8;
        int bestZ = chunkZ + 8;
        float best = -1f;
        for (int x = chunkX; x < chunkX + 16; x += 2) {
            for (int z = chunkZ; z < chunkZ + 16; z += 2) {
                float strength = manager.getRiverJunctionStrength(x, z);
                if (strength > best) {
                    best = strength;
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        if (best < .35f) return null;
        for (int step = 4; step >= 1; step /= 2) {
            boolean moved;
            do {
                moved = false;
                for (int dx = -step; dx <= step; dx += step) {
                    for (int dz = -step; dz <= step; dz += step) {
                        int x = bestX + dx;
                        int z = bestZ + dz;
                        float strength = manager.getRiverJunctionStrength(x, z);
                        if (strength > best) {
                            best = strength;
                            bestX = x;
                            bestZ = z;
                            moved = true;
                        }
                    }
                }
            } while (moved);
        }
        if (best < .82f || !hasMountainChainNearby(manager, bestX, bestZ)
                || manager.getLavaCaveCoordinates(bestX, bestZ) != Long.MIN_VALUE)
            return null;
        return new int[] { bestX, bestZ };
    }

    private static boolean hasMountainChainNearby(ChunkManagerRealistic manager, int x, int z) {
        for (int dx = -24; dx <= 24; dx += 12) {
            for (int dz = -24; dz <= 24; dz += 12) {
                if (manager.getBiomeDataAt(x + dx, z + dz) instanceof RealisticBiomeMountainChain) return true;
            }
        }
        return false;
    }

    private static void placeRoundedPillar(World world, int centerX, int centerZ, int bottom, int top, int radius,
            Block block, boolean shieldTop) {
        int radiusSquared = radius * radius + radius / 2;
        int shieldMetadata = DungeonBrick.Type.FLOOR_SHIELDED.ordinal();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) continue;
                for (int y = bottom; y <= top; y++) {
                    boolean shield = shieldTop && y == top;
                    world.setBlock(
                            centerX + dx,
                            y,
                            centerZ + dz,
                            shield ? LGBlocks.DUNGEON_WALL : block,
                            shield ? shieldMetadata : 0,
                            2);
                }
            }
        }
    }

    private static void placeSquarePillar(World world, int centerX, int centerZ, int bottom, int top, int width,
            Block block) {
        int minimumOffset = -width / 2;
        int maximumOffset = minimumOffset + width - 1;
        for (int dx = minimumOffset; dx <= maximumOffset; dx++) {
            for (int dz = minimumOffset; dz <= maximumOffset; dz++) {
                for (int y = bottom; y <= top; y++) world.setBlock(centerX + dx, y, centerZ + dz, block, 0, 2);
            }
        }
    }

    private static void placeNetherPortal(World world, int centerX, int bottom, int centerZ, boolean alongZ) {
        for (int horizontal = -1; horizontal <= 2; horizontal++) {
            for (int y = 0; y < 5; y++) {
                boolean frame = horizontal == -1 || horizontal == 2 || y == 0 || y == 4;
                int x = centerX + (alongZ ? 0 : horizontal);
                int z = centerZ + (alongZ ? horizontal : 0);
                world.setBlock(x, bottom + y, z, frame ? Blocks.obsidian : Blocks.portal, alongZ ? 2 : 1, 2);
            }
        }
    }

    private void placeTwilightPortal(World world, int centerX, int y, int centerZ) {
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                boolean portal = dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
                world.setBlock(centerX + dx, y, centerZ + dz, portal ? twilightPortal : Blocks.grass, 0, 2);
            }
        }
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
