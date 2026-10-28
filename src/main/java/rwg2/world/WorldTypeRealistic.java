package rwg2.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

public class WorldTypeRealistic extends WorldType {

    private final boolean continents;

    public WorldTypeRealistic(String name, boolean continents) {
        super(name);
        this.continents = continents;
    }

    public WorldChunkManager getChunkManager(World world) {
        return new ChunkManagerRealistic(world, continents);
    }

    public IChunkProvider getChunkGenerator(World world, String generatorOptions) {
        return new ChunkGeneratorRealistic(world, world.getSeed(), continents);
    }

    public float getCloudHeight() {
        return 256F;
    }
}
