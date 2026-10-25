package rwg.biomes.realistic.land;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.api.RWGBiomes;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.deco.DecoBlob;
import rwg.deco.DecoFlowers;
import rwg.deco.DecoGrass;
import rwg.deco.trees.DecoJungleFat;
import rwg.surface.SurfaceBase;
import rwg.surface.SurfaceGrassland;
import rwg.terrain.TerrainBase;
import rwg.terrain.TerrainHighland;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.util.PoissonPointNoise;

public class RealisticBiomeHighRainforest extends RealisticBiomeBase {

    private static final double LARGE_TREE_SPACING = 24D;
    private static final int LARGE_TREE_PLACEMENT_ROUNDS = 2;
    private static final long LARGE_TREE_SEED_SALT = 0x3C6EF372FE94F82BL;

    private TerrainBase terrain;
    private SurfaceBase surface;
    private long largeTreeSeed = Long.MIN_VALUE;
    private PoissonPointNoise largeTreePoints;

    public RealisticBiomeHighRainforest() {
        super(0, RWGBiomes.baseJungle);

        terrain = new TerrainHighland(0f, 140f, 68f, 200f);
        surface = new SurfaceGrassland(Blocks.grass, Blocks.dirt, Blocks.stone, Blocks.cobblestone);
    }

    public void rDecorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {
        if (strength <= 0f) return;
        for (int attempt = 0; attempt < 2f * strength; attempt++) {
            int x = chunkX + rand.nextInt(16) + 8;
            int z = chunkY + rand.nextInt(16) + 8;
            int y = world.getHeightValue(x, z);
            new DecoBlob(Blocks.mossy_cobblestone, 0).generate(world, rand, x, y, z);
        }

        PoissonPointNoise points = largeTreePoints(world.getSeed());
        double cellSize = points.getCellSize();
        int minimumX = chunkX + 8;
        int minimumZ = chunkY + 8;
        int maximumX = minimumX + 16;
        int maximumZ = minimumZ + 16;
        int minimumCellX = floor(minimumX / cellSize);
        int maximumCellX = floor((maximumX - 1) / cellSize);
        int minimumCellZ = floor(minimumZ / cellSize);
        int maximumCellZ = floor((maximumZ - 1) / cellSize);
        double[] candidate = new double[2];
        for (int cellZ = minimumCellZ; cellZ <= maximumCellZ; cellZ++) {
            for (int cellX = minimumCellX; cellX <= maximumCellX; cellX++) {
                for (int round = 0; round < points.getRounds(); round++) {
                    if (!points.isAccepted(cellX, cellZ, round)) continue;
                    points.getCandidate(cellX, cellZ, round, candidate);
                    int treeX = floor(candidate[0]);
                    int treeZ = floor(candidate[1]);
                    if (treeX < minimumX || treeX >= maximumX
                            || treeZ < minimumZ
                            || treeZ >= maximumZ
                            || rand.nextFloat() >= Math.min(strength, 1f))
                        continue;
                    generateLargeTree(world, rand, treeX, treeZ);
                }
            }
        }

        for (int attempt = 0; attempt < 13; attempt++) {
            int x = chunkX + rand.nextInt(16) + 8;
            int y = rand.nextInt(128);
            int z = chunkY + rand.nextInt(16) + 8;
            if (rand.nextInt(6) == 0) {
                new DecoGrass(Blocks.double_plant, rand.nextBoolean() ? 2 : 3).generate(world, rand, x, y, z);
            } else {
                new DecoGrass(Blocks.tallgrass, rand.nextBoolean() ? 1 : 2).generate(world, rand, x, y, z);
            }
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            if (rand.nextFloat() >= Math.min(strength, 1f)) continue;
            int x = chunkX + rand.nextInt(16) + 8;
            int y = rand.nextInt(128);
            int z = chunkY + rand.nextInt(16) + 8;
            new DecoFlowers(new int[] { 1, 1, 1, 8 }).generate(world, rand, x, y, z);
        }
    }

    private synchronized PoissonPointNoise largeTreePoints(long worldSeed) {
        long seed = worldSeed ^ LARGE_TREE_SEED_SALT;
        if (largeTreePoints == null || largeTreeSeed != seed) {
            largeTreeSeed = seed;
            largeTreePoints = new PoissonPointNoise(seed, LARGE_TREE_SPACING, LARGE_TREE_PLACEMENT_ROUNDS);
        }
        return largeTreePoints;
    }

    private static void generateLargeTree(World world, Random rand, int x, int z) {
        int y = world.getHeightValue(x, z);
        WorldGenerator generator = new DecoJungleFat(
                Blocks.log,
                3,
                Blocks.leaves,
                3,
                15 + rand.nextInt(15),
                4 + rand.nextInt(2),
                14f,
                3,
                0.2f,
                0.25f);
        generator.setScale(1.0D, 1.0D, 1.0D);
        generator.generate(world, rand, x, y, z);
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river) {
        return terrain.generateNoise(perlin, cell, x, y, ocean, border, river);
    }

    public void rReplace(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        surface.paintTerrain(blocks, metadata, i, j, x, y, depth, world, rand, perlin, cell, noise, river, base);
    }
}
