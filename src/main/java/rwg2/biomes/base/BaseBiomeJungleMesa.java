package rwg2.biomes.base;

import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.world.biome.BiomeGenBase;

/** The registered biome identity used by RWG's wet, terraced jungle islands. */
public class BaseBiomeJungleMesa extends BiomeGenBase {

    public BaseBiomeJungleMesa(int id, String biomeName) {
        super(id);
        setTemperatureRainfall(0.9f, 1.0f);
        setBiomeName(biomeName);
        waterColorMultiplier = 65326;

        spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntityOcelot.class, 2, 1, 1));
    }

    @Override
    public int getBiomeGrassColor(int x, int y, int z) {
        return 5762404;
    }

    @Override
    public int getBiomeFoliageColor(int x, int y, int z) {
        return 5762404;
    }
}
