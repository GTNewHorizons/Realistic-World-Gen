package rwg.world;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import com.falsepattern.endlessids.mixin.helpers.ChunkBiomeHook;

/** Writes unsigned 16-bit biome IDs through EndlessIDs' extended chunk storage. */
final class ChunkBiomeArrayCompat {

    private ChunkBiomeArrayCompat() {}

    static void setBiomes(Chunk chunk, BiomeGenBase[] biomes) {
        short[] ids = ((ChunkBiomeHook) chunk).getBiomeShortArray();
        if (ids.length != biomes.length) throw new IllegalStateException("Unexpected extended biome array length");
        for (int index = 0; index < ids.length; index++) ids[index] = (short) biomes[index].biomeID;
    }
}
