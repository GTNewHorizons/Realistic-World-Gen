package rwg.surface;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

/** Paints volcanic terrain uniformly, without substituting stone or beach materials on steep slopes. */
public class SurfaceVolcanoAsh extends SurfaceBase {

    private static final int MIN_ASH_HEIGHT = 61;

    public SurfaceVolcanoAsh(Block ash, Block ashStone) {
        super(ash, ashStone);
    }

    @Override
    public void paintTerrain(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        paintTerrain(blocks, metadata, i, j, x, y, depth, world, rand, perlin, cell, noise, river, base, 6);
    }

    public void paintTerrain(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base,
            int surfaceDepth) {
        int column = (y * 16 + x) * 256;
        boolean ashSurface = false;
        for (int level = 255; level >= 0; level--) {
            Block block = blocks[column + level];
            if (block == Blocks.air) {
                depth = -1;
                ashSurface = false;
            } else if (block == Blocks.stone) {
                depth++;
                if (depth == 0) {
                    ashSurface = level >= MIN_ASH_HEIGHT;
                    if (ashSurface) {
                        blocks[column + level] = topBlock;
                        metadata[column + level] = 0;
                    }
                } else if (ashSurface && depth < 6 && depth < surfaceDepth) {
                    blocks[column + level] = fillerBlock;
                    metadata[column + level] = 0;
                }
            }
        }
    }
}
