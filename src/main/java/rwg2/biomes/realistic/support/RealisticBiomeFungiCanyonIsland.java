package rwg2.biomes.realistic.support;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import biomesoplenty.api.biome.BOPBiome;
import biomesoplenty.common.biome.decoration.BOPOverworldBiomeDecorator;
import biomesoplenty.common.biome.decoration.OverworldBiomeFeatures;
import rwg2.api.RWGBiomes;
import rwg2.biomes.realistic.RealisticBiomeBase;
import rwg2.surface.SurfaceBase;
import rwg2.surface.SurfaceMountainStone;
import rwg2.terrain.TerrainBase;
import rwg2.terrain.TerrainCanyon;
import rwg2.util.CellNoise;
import rwg2.util.CliffCalculator;
import rwg2.util.NoiseGenerator;

/** A flooded, terraced jungle mesa which retains BOP Fungi Forest vegetation without its fungal ground cover. */
public class RealisticBiomeFungiCanyonIsland extends RealisticBiomeBase {

    private final BiomeGenBase fungiForest;
    private final BiomeGenBase marsh;
    private final TerrainBase terrain = new TerrainCanyon(true, 35f, 160f, 60f, 40f, 61f);
    private final SurfaceBase surface;

    public RealisticBiomeFungiCanyonIsland(BiomeGenBase fungiForest, BiomeGenBase marsh) {
        super(0, RWGBiomes.baseJungleMesa, RealisticBiomeBase.coastDunes, RWGBiomes.baseRiverWet);
        this.fungiForest = fungiForest;
        this.marsh = marsh;
        surface = new SurfaceMountainStone(fungiForest.topBlock, fungiForest.fillerBlock, false, null, 0.95f);
    }

    @Override
    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river) {
        return terrain.generateNoise(perlin, cell, x, y, ocean, border, river);
    }

    @Override
    public void rReplace(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        surface.paintTerrain(blocks, metadata, i, j, x, y, depth, world, rand, perlin, cell, noise, river, base);
        paintShallowPools(blocks, metadata, i, j, x, y, perlin, noise);
    }

    @Override
    public void rDecorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {
        if (strength <= 0.3f) return;
        if (marsh != null) decorateWithMarshWater(world, rand, chunkX, chunkY);
        decorateFungiForestWithoutMushrooms(world, rand, chunkX, chunkY);
    }

    private void paintShallowPools(Block[] blocks, byte[] metadata, int blockX, int blockZ, int localX, int localZ,
            NoiseGenerator perlin, float[] heightNoise) {
        // Pools belong on the canyon's horizontal shelves, never on stair faces or softer transition slopes.
        if (CliffCalculator.calc(localX, localZ, heightNoise) > 0.45f) return;

        float poolNoise = perlin.noise2(blockX / 32f, blockZ / 32f) + perlin.noise2(blockX / 9f, blockZ / 9f) * 0.25f;
        if (poolNoise < 0.5f) return;

        int column = (localZ * 16 + localX) * 256;
        for (int height = 255; height > 1; height--) {
            int index = column + height;
            if (blocks[index] == Blocks.air && blocks[index - 1] == fungiForest.topBlock) {
                blocks[index - 1] = Blocks.water;
                metadata[index - 1] = 0;
                if (poolNoise > 0.82f && blocks[index - 2] == fungiForest.fillerBlock) {
                    blocks[index - 2] = Blocks.water;
                    metadata[index - 2] = 0;
                }
                return;
            }
        }
    }

    private void decorateFungiForestWithoutMushrooms(World world, Random rand, int chunkX, int chunkY) {
        BOPOverworldBiomeDecorator decorator = decorator(fungiForest);
        OverworldBiomeFeatures features = decorator.bopFeatures;
        synchronized (decorator) {
            int mushrooms = decorator.mushroomsPerChunk;
            int bigMushrooms = decorator.bigMushroomsPerChunk;
            int toadstools = features.toadstoolsPerChunk;
            int glowshrooms = features.glowshroomsPerChunk;
            int bopBigMushrooms = features.bopBigMushroomsPerChunk;
            int portobellos = features.portobellosPerChunk;
            int blueMilks = features.blueMilksPerChunk;
            boolean mycelium = features.generateMycelium;
            try {
                decorator.mushroomsPerChunk = 0;
                decorator.bigMushroomsPerChunk = 0;
                features.toadstoolsPerChunk = 0;
                features.glowshroomsPerChunk = 0;
                features.bopBigMushroomsPerChunk = 0;
                features.portobellosPerChunk = 0;
                features.blueMilksPerChunk = 0;
                features.generateMycelium = false;
                fungiForest.decorate(world, rand, chunkX, chunkY);
            } finally {
                decorator.mushroomsPerChunk = mushrooms;
                decorator.bigMushroomsPerChunk = bigMushrooms;
                features.toadstoolsPerChunk = toadstools;
                features.glowshroomsPerChunk = glowshrooms;
                features.bopBigMushroomsPerChunk = bopBigMushrooms;
                features.portobellosPerChunk = portobellos;
                features.blueMilksPerChunk = blueMilks;
                features.generateMycelium = mycelium;
            }
        }
    }

    private void decorateWithMarshWater(World world, Random rand, int chunkX, int chunkY) {
        BOPOverworldBiomeDecorator decorator = decorator(marsh);
        OverworldBiomeFeatures features = decorator.bopFeatures;
        synchronized (decorator) {
            int grass = decorator.grassPerChunk;
            int bopGrass = features.bopGrassPerChunk;
            int waterLakes = features.waterLakesPerChunk;
            try {
                // Retain the Marsh's mud and aquatic plants without its dense grass or underground lake excavation.
                decorator.grassPerChunk = -999;
                features.bopGrassPerChunk = 0;
                features.waterLakesPerChunk = 0;
                marsh.decorate(world, rand, chunkX, chunkY);
            } finally {
                decorator.grassPerChunk = grass;
                features.bopGrassPerChunk = bopGrass;
                features.waterLakesPerChunk = waterLakes;
            }
        }
    }

    private static BOPOverworldBiomeDecorator decorator(BiomeGenBase biome) {
        if (!(biome instanceof BOPBiome)) {
            throw new IllegalStateException(
                    "Expected a BOP biome for " + biome.biomeName + ", got " + biome.getClass().getName());
        }

        // BOPBiome hides BiomeGenBase.theBiomeDecorator with its own typed field. Accessing the field through a
        // BiomeGenBase reference returns the unused vanilla decorator instead of the one BOP uses in decorate().
        Object decorator = ((BOPBiome<?>) biome).theBiomeDecorator;
        if (!(decorator instanceof BOPOverworldBiomeDecorator)) {
            throw new IllegalStateException(
                    "Expected a BOP overworld decorator for " + biome.biomeName
                            + ", got "
                            + (decorator == null ? "null" : decorator.getClass().getName()));
        }
        return (BOPOverworldBiomeDecorator) decorator;
    }
}
