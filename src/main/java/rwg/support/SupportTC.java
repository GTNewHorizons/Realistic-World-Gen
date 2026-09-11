package rwg.support;

import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.api.RWGBiomes;
import rwg.support.Support.BiomeCategory;
import rwg.support.Support.BiomePlacement;
import rwg.surface.SurfaceGrassland;
import rwg.terrain.TerrainSmallSupport;
import rwg.terrain.TerrainSwampMountain;

public class SupportTC {
    /*
     * Resolve by name because Thaumcraft biome IDs are pack-configurable. GTNH currently assigns Eerie 190, Eldritch
     * Lands 191, Magical Forest 192, and Tainted Land 193.
     */

    public static void init() {
        BiomeGenBase[] b = BiomeGenBase.getBiomeGenArray();

        for (int i = 0; i < 256; i++) {
            if (b[i] != null) {
                if ("Tainted Land".equals(b[i].biomeName)) {
                    Support.addBiome(
                            new RealisticBiomeSupport(
                                    b[i],
                                    RWGBiomes.baseRiverTemperate,
                                    new TerrainSmallSupport(),
                                    new SurfaceGrassland(
                                            b[i].topBlock,
                                            b[i].fillerBlock,
                                            Blocks.stone,
                                            Blocks.cobblestone)),
                            BiomeCategory.SMALL);
                }

                if ("Magical Forest".equals(b[i].biomeName)) {
                    Support.addBiome(
                            new RealisticBiomeSupport(
                                    b[i],
                                    RWGBiomes.baseRiverCold,
                                    new TerrainSwampMountain(135f, 300f),
                                    new SurfaceGrassland(
                                            b[i].topBlock,
                                            b[i].fillerBlock,
                                            Blocks.stone,
                                            Blocks.cobblestone)),
                            BiomeCategory.SNOW,
                            BiomePlacement.SMALL_ISLAND);
                    Support.addBiome(
                            new RealisticBiomeSupport(
                                    b[i],
                                    RWGBiomes.baseRiverCold,
                                    new TerrainSwampMountain(135f, 300f),
                                    new SurfaceGrassland(
                                            b[i].topBlock,
                                            b[i].fillerBlock,
                                            Blocks.stone,
                                            Blocks.cobblestone)),
                            BiomeCategory.COLD,
                            BiomePlacement.SMALL_ISLAND);
                }
            }
        }
    }

}
