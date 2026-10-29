package rwg2.biomes.realistic.ocean;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg2.biomes.realistic.RealisticBiomeBase;
import rwg2.util.CellNoise;
import rwg2.util.NoiseGenerator;

public class RealisticBiomeOcean extends RealisticBiomeBase {

    public enum DepthRange {
        SHALLOW,
        DEEP
    }

    private final boolean shallow;
    private final boolean decorateBaseBiome;
    private final String variantName;

    public RealisticBiomeOcean(BiomeGenBase biome, boolean shallow, boolean decorateBaseBiome) {
        this(biome, shallow, decorateBaseBiome, shallow ? "RealisticBiomeOceanShallow" : "RealisticBiomeOceanDeep");
    }

    public RealisticBiomeOcean(BiomeGenBase biome, boolean shallow, boolean decorateBaseBiome, String variantName) {
        super(0, biome);
        this.shallow = shallow;
        this.decorateBaseBiome = decorateBaseBiome;
        this.variantName = variantName;
    }

    public String getVariantName() {
        return variantName;
    }

    public DepthRange getDepthRange() {
        return shallow ? DepthRange.SHALLOW : DepthRange.DEEP;
    }

    @Override
    public void rDecorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {}

    public void rDecorateAfterIce(World world, Random rand, int chunkX, int chunkY, float strength) {
        if (decorateBaseBiome && strength > 0.3f) {
            baseBiome.decorate(world, rand, chunkX, chunkY);
        }
    }

    @Override
    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river) {
        float height = shallow ? 52f : 34f;
        return height + perlin.noise2(x / 220f, y / 220f) * 4f + perlin.noise2(x / 55f, y / 55f) * 1.5f;
    }

    @Override
    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river,
            float continent) {
        return rNoise(perlin, cell, x, y, ocean, border, river);
    }

    @Override
    public void rReplace(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        Block surface = shallow ? Blocks.sand : Blocks.gravel;
        int column = (y * 16 + x) * 256;
        for (int level = 255; level >= 0; level--) {
            Block block = blocks[column + level];
            if (block == Blocks.air || block == Blocks.water) {
                depth = -1;
            } else if (block == Blocks.stone) {
                depth++;
                if (depth < 6) {
                    blocks[column + level] = surface;
                    metadata[column + level] = 0;
                }
            }
        }
    }
}
