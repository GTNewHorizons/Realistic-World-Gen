package rwg.biomes.realistic.ocean;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import biomesoplenty.api.content.BOPCBlocks;
import biomesoplenty.common.blocks.BlockBOPCoral;

/** BOP ocean decoration with stricter placement checks for its underwater plants. */
public class RealisticBiomeBOPOcean extends RealisticBiomeOcean {

    private static final int MIN_DECORATION_OFFSET = -3;
    private static final int MAX_DECORATION_OFFSET = 30;

    public RealisticBiomeBOPOcean(BiomeGenBase biome, String variantName) {
        super(biome, true, true, variantName);
    }

    @Override
    public void rDecorateAfterIce(World world, Random rand, int chunkX, int chunkZ, float strength) {
        super.rDecorateAfterIce(world, rand, chunkX, chunkZ, strength);
        if (strength <= 0.3f) return;

        for (int x = chunkX + MIN_DECORATION_OFFSET; x <= chunkX + MAX_DECORATION_OFFSET; x++) {
            for (int z = chunkZ + MIN_DECORATION_OFFSET; z <= chunkZ + MAX_DECORATION_OFFSET; z++) {
                sanitizeColumn(world, x, z);
            }
        }
    }

    private void sanitizeColumn(World world, int x, int z) {
        for (int y = 1; y < 63; y++) {
            Block block = world.getBlock(x, y, z);
            int metadata = world.getBlockMetadata(x, y, z);
            if (block == BOPCBlocks.coral1 && metadata >= 8 && metadata <= 11) {
                y = sanitizeKelp(world, x, y, z);
            } else if (block == BOPCBlocks.coral1 || block == BOPCBlocks.coral2) {
                sanitizeCoral(world, x, y, z, block, metadata);
            }
        }
    }

    private int sanitizeKelp(World world, int x, int bottom, int z) {
        int top = bottom;
        while (top + 1 < 63 && isKelp(world, x, top + 1, z)) top++;

        boolean valid = isMatchingBiome(world, x, z)
                && ((BlockBOPCoral) BOPCBlocks.coral1).canBlockStay(world, x, bottom, z, 11)
                && world.getBlock(x, top + 1, z) == Blocks.water;

        if (!valid) {
            for (int y = bottom; y <= top; y++) world.setBlock(x, y, z, Blocks.water, 0, 2);
        } else if (bottom == top) {
            world.setBlockMetadataWithNotify(x, bottom, z, 11, 2);
        } else {
            world.setBlockMetadataWithNotify(x, bottom, z, 8, 2);
            for (int y = bottom + 1; y < top; y++) world.setBlockMetadataWithNotify(x, y, z, 9, 2);
            world.setBlockMetadataWithNotify(x, top, z, 10, 2);
        }
        return top;
    }

    private void sanitizeCoral(World world, int x, int y, int z, Block block, int metadata) {
        boolean valid = isMatchingBiome(world, x, z) && world.getBlock(x, y + 1, z) == Blocks.water
                && ((BlockBOPCoral) block).canBlockStay(world, x, y, z, metadata);
        if (!valid) world.setBlock(x, y, z, Blocks.water, 0, 2);
    }

    private boolean isKelp(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != BOPCBlocks.coral1) return false;
        int metadata = world.getBlockMetadata(x, y, z);
        return metadata >= 8 && metadata <= 11;
    }

    private boolean isMatchingBiome(World world, int x, int z) {
        return world.getBiomeGenForCoords(x, z) == baseBiome;
    }
}
