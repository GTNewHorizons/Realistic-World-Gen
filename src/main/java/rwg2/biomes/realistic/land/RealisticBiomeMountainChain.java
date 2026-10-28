package rwg2.biomes.realistic.land;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg2.biomes.realistic.RealisticBiomeBase;
import rwg2.surface.SurfaceBase;
import rwg2.surface.SurfaceMountainStoneMix1;
import rwg2.terrain.TerrainBase;
import rwg2.terrain.TerrainHilly;
import rwg2.util.CellNoise;
import rwg2.util.NoiseGenerator;
import rwg2.world.ChunkManagerRealistic;

/** Shared placeholder terrain for borders between non-adjacent climate bands. */
public class RealisticBiomeMountainChain extends RealisticBiomeBase {

    private static final RealisticBiomeMountainChain[] VARIANTS = new RealisticBiomeMountainChain[256];
    private static final int DECORATION_SAMPLE_STEP = 4;
    private static final int MAX_DECORATED_HEIGHT_CHANGE = 4;
    private final RealisticBiomeBase backingBiome;
    private final TerrainBase terrain = new TerrainHilly(230f, 120f, 0f, 260f, 120f);
    private final SurfaceBase surface;

    private RealisticBiomeMountainChain(RealisticBiomeBase backingBiome) {
        super(0, backingBiome.baseBiome, backingBiome.beachBiome, backingBiome.riverBiome);
        this.backingBiome = backingBiome;
        surface = createSurface(backingBiome.baseBiome.topBlock, backingBiome.baseBiome.fillerBlock);
        setDisplayName(backingBiome.baseBiome.biomeName);
    }

    public static RealisticBiomeMountainChain forBiome(RealisticBiomeBase backingBiome) {
        RealisticBiomeMountainChain variant = VARIANTS[backingBiome.biomeID];
        if (variant == null) {
            variant = new RealisticBiomeMountainChain(backingBiome);
            VARIANTS[backingBiome.biomeID] = variant;
        }
        return variant;
    }

    private static SurfaceBase createSurface(Block top, Block fill) {
        return new SurfaceMountainStoneMix1(top, fill, false, null, 0f, 1.5f, 60f, 65f, 1.5f, Blocks.stone, 0.20f);
    }

    @Override
    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river) {
        return terrain.generateNoise(perlin, cell, x, y, ocean, border, river);
    }

    @Override
    public void rDecorate(World world, Random rand, int chunkX, int chunkZ, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {
        if (!(world.getWorldChunkManager() instanceof ChunkManagerRealistic)) return;
        ChunkManagerRealistic manager = (ChunkManagerRealistic) world.getWorldChunkManager();
        int sampleCount = 0;
        int gentleSampleCount = 0;
        for (int offsetX = 8; offsetX < 24; offsetX += DECORATION_SAMPLE_STEP) {
            for (int offsetZ = 8; offsetZ < 24; offsetZ += DECORATION_SAMPLE_STEP) {
                int x = chunkX + offsetX;
                int z = chunkZ + offsetZ;
                float height = manager.getNoiseAt(x, z);
                float heightChange = Math.max(
                        Math.max(
                                Math.abs(height - manager.getNoiseAt(x - DECORATION_SAMPLE_STEP, z)),
                                Math.abs(height - manager.getNoiseAt(x + DECORATION_SAMPLE_STEP, z))),
                        Math.max(
                                Math.abs(height - manager.getNoiseAt(x, z - DECORATION_SAMPLE_STEP)),
                                Math.abs(height - manager.getNoiseAt(x, z + DECORATION_SAMPLE_STEP))));
                if (heightChange <= MAX_DECORATED_HEIGHT_CHANGE) {
                    gentleSampleCount++;
                }
                sampleCount++;
            }
        }
        backingBiome.rDecorate(
                world,
                rand,
                chunkX,
                chunkZ,
                perlin,
                cell,
                strength * gentleSampleCount / sampleCount,
                river);
    }

    @Override
    public void rReplace(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        base[x * 16 + y] = backingBiome.baseBiome;
        surface.paintTerrain(blocks, metadata, i, j, x, y, depth, world, rand, perlin, cell, noise, river, base);
    }
}
