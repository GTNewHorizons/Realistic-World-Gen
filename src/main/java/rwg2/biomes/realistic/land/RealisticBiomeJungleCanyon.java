package rwg2.biomes.realistic.land;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg2.api.RWGBiomes;
import rwg2.biomes.realistic.RealisticBiomeBase;
import rwg2.deco.DecoBlob;
import rwg2.deco.trees.DecoJungleSmall;
import rwg2.surface.SurfaceBase;
import rwg2.surface.SurfaceGrassCanyon;
import rwg2.terrain.TerrainBase;
import rwg2.terrain.TerrainCanyon;
import rwg2.util.CellNoise;
import rwg2.util.NoiseGenerator;

public class RealisticBiomeJungleCanyon extends RealisticBiomeBase {

    private TerrainBase terrain;
    private SurfaceBase surface;

    public RealisticBiomeJungleCanyon() {
        super(0, RWGBiomes.baseJungleCanyon);

        terrain = new TerrainCanyon(true, 35f, 160f, 60f, 40f, 69f);
        surface = new SurfaceGrassCanyon(Blocks.grass, Blocks.dirt, (byte) 0);
    }

    @Override
    public void rDecorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {
        for (int l = 0; l < 1f * strength; ++l) {
            int i1 = chunkX + rand.nextInt(16) + 8;
            int j1 = chunkY + rand.nextInt(16) + 8;
            int k1 = world.getHeightValue(i1, j1);
            if (k1 < 70) {
                (new DecoBlob(Blocks.mossy_cobblestone, 0)).generate(world, rand, i1, k1, j1);
            }
        }

        for (int a = 0; a < 5f * strength; a++) {
            int j6 = chunkX + rand.nextInt(16) + 8;
            int k10 = chunkY + rand.nextInt(16) + 8;
            int z52 = world.getHeightValue(j6, k10);

            WorldGenerator worldgenerator = new DecoJungleSmall(
                    Blocks.log,
                    3,
                    Blocks.leaves,
                    3,
                    1 + rand.nextInt(4),
                    0,
                    5f,
                    2,
                    0.32f,
                    0.14f);
            worldgenerator.setScale(1.0D, 1.0D, 1.0D);
            worldgenerator.generate(world, rand, j6, z52, k10);
        }
    }

    @Override
    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river) {
        return terrain.generateNoise(perlin, cell, x, y, ocean, border, river);
    }

    @Override
    public void rReplace(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        surface.paintTerrain(blocks, metadata, i, j, x, y, depth, world, rand, perlin, cell, noise, river, base);
    }
}
