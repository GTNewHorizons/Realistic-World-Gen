package rwg.biomes.base;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import rwg.api.RWGBiomes;
import rwg.config.ConfigRWG;

public class BaseBiomes {

    public static void load() {
        RWGBiomes.baseRiverIce = withColor(new BaseBiomeRiver(ConfigRWG.biomeIDs[0], 0, "Ice River"), 0xA0A0FF);
        RWGBiomes.baseRiverCold = withColor(new BaseBiomeRiver(ConfigRWG.biomeIDs[1], 1, "Cold River"), 0x4848FF);
        RWGBiomes.baseRiverTemperate = withColor(
                new BaseBiomeRiver(ConfigRWG.biomeIDs[2], 2, "Temperate River"),
                0x0000FF);
        RWGBiomes.baseRiverHot = withColor(new BaseBiomeRiver(ConfigRWG.biomeIDs[3], 3, "Hot River"), 0x0000C8);
        RWGBiomes.baseRiverWet = withColor(new BaseBiomeRiver(ConfigRWG.biomeIDs[4], 4, "Wet River"), 0x0048DC);
        RWGBiomes.baseRiverOasis = withColor(new BaseBiomeRiver(ConfigRWG.biomeIDs[5], 5, "River Oasis"), 0x0088FF);

        RWGBiomes.baseOceanIce = withColor(new BaseBiomeOcean(ConfigRWG.biomeIDs[6], 0, "Ice Ocean"), 0x9090A0);
        RWGBiomes.baseOceanCold = withColor(new BaseBiomeOcean(ConfigRWG.biomeIDs[7], 1, "Cold Ocean"), 0x181890);
        RWGBiomes.baseOceanTemperate = withColor(
                new BaseBiomeOcean(ConfigRWG.biomeIDs[8], 2, "Temperate Ocean"),
                0x000070);
        RWGBiomes.baseOceanHot = withColor(new BaseBiomeOcean(ConfigRWG.biomeIDs[9], 3, "Hot Ocean"), 0x000050);
        RWGBiomes.baseOceanWet = withColor(new BaseBiomeOcean(ConfigRWG.biomeIDs[10], 4, "Wet Ocean"), 0x003070);
        RWGBiomes.baseOceanOasis = withColor(new BaseBiomeOcean(ConfigRWG.biomeIDs[11], 5, "Ocean Oasis"), 0x005A78);

        RWGBiomes.baseSnowDesert = withColor(new BaseBiomeSnowDesert(ConfigRWG.biomeIDs[12], "Snow Desert"), 0xFFFFFF);
        RWGBiomes.baseSnowForest = withColor(new BaseBiomeSnowForest(ConfigRWG.biomeIDs[13], "Snow Forest"), 0x31554A);
        RWGBiomes.baseColdPlains = withColor(new BaseBiomeColdPlains(ConfigRWG.biomeIDs[14], "Cold Plains"), 0x8FB0A0);
        RWGBiomes.baseColdForest = withColor(new BaseBiomeColdForest(ConfigRWG.biomeIDs[15], "Cold Forest"), 0x0B6659);
        RWGBiomes.baseHotPlains = withColor(new BaseBiomeHotPlains(ConfigRWG.biomeIDs[16], "Hot Plains"), 0xBDB25F);
        RWGBiomes.baseHotForest = withColor(new BaseBiomeHotForest(ConfigRWG.biomeIDs[17], "Hot Forest"), 0xA79D64);
        RWGBiomes.baseHotDesert = withColor(new BaseBiomeHotDesert(ConfigRWG.biomeIDs[18], "Hot Desert"), 0xFA9418);
        RWGBiomes.basePlains = withColor(new BaseBiomePlains(ConfigRWG.biomeIDs[19], "Plains (RWG)"), 0x8DB360);
        RWGBiomes.baseTropicalIsland = withColor(
                new BaseBiomeTropicalIsland(ConfigRWG.biomeIDs[20], "Tropical Island"),
                0x5BC25B);
        RWGBiomes.baseRedwood = withColor(new BaseBiomeRedwood(ConfigRWG.biomeIDs[21], "Redwood"), 0x596651);
        RWGBiomes.baseJungle = withColor(new BaseBiomeJungle(ConfigRWG.biomeIDs[22], "Jungle (RWG)"), 0x537B09);
        RWGBiomes.baseOasis = withColor(new BaseBiomeRiver(ConfigRWG.biomeIDs[23], 5, "Oasis"), 0x00B3A4);
        RWGBiomes.baseTemperateForest = withColor(
                new BaseBiomeTemperateForest(ConfigRWG.biomeIDs[24], "Temperate Forest"),
                0x056621);

        // RIVER
        BiomeDictionary.registerBiomeType(RWGBiomes.baseRiverIce, Type.RIVER, Type.COLD, Type.SNOWY);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseRiverCold, Type.RIVER, Type.COLD, Type.CONIFEROUS, Type.FOREST);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseRiverTemperate, Type.RIVER, Type.COLD, Type.FOREST);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseRiverHot, Type.RIVER, Type.HOT, Type.DRY, Type.SANDY);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseRiverWet, Type.RIVER, Type.HOT, Type.WET, Type.JUNGLE);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseRiverOasis, Type.RIVER, Type.HOT, Type.WET, Type.JUNGLE);

        // OCEAN
        BiomeDictionary.registerBiomeType(RWGBiomes.baseOceanIce, Type.OCEAN, Type.BEACH, Type.COLD, Type.SNOWY);
        BiomeDictionary.registerBiomeType(
                RWGBiomes.baseOceanCold,
                Type.OCEAN,
                Type.BEACH,
                Type.COLD,
                Type.CONIFEROUS,
                Type.FOREST);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseOceanTemperate, Type.OCEAN, Type.BEACH, Type.COLD, Type.FOREST);
        BiomeDictionary
                .registerBiomeType(RWGBiomes.baseOceanHot, Type.OCEAN, Type.BEACH, Type.HOT, Type.DRY, Type.SANDY);
        BiomeDictionary
                .registerBiomeType(RWGBiomes.baseOceanWet, Type.OCEAN, Type.BEACH, Type.HOT, Type.WET, Type.JUNGLE);
        BiomeDictionary
                .registerBiomeType(RWGBiomes.baseOceanOasis, Type.OCEAN, Type.BEACH, Type.HOT, Type.WET, Type.JUNGLE);

        // LAND
        BiomeDictionary.registerBiomeType(RWGBiomes.baseSnowDesert, Type.COLD, Type.SNOWY, Type.WASTELAND);
        BiomeDictionary
                .registerBiomeType(RWGBiomes.baseSnowForest, Type.COLD, Type.SNOWY, Type.CONIFEROUS, Type.FOREST);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseColdPlains, Type.COLD, Type.WASTELAND);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseColdForest, Type.COLD, Type.CONIFEROUS, Type.FOREST);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseHotPlains, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseHotForest, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseHotDesert, Type.HOT, Type.DRY, Type.SANDY);
        BiomeDictionary.registerBiomeType(RWGBiomes.basePlains, Type.PLAINS);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseTropicalIsland, Type.HOT, Type.WET, Type.JUNGLE);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseRedwood, Type.COLD, Type.CONIFEROUS, Type.FOREST);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseJungle, Type.HOT, Type.WET, Type.JUNGLE);
        BiomeDictionary.registerBiomeType(RWGBiomes.baseColdForest, Type.FOREST, Type.DENSE, Type.HILLS);
    }

    private static <T extends BiomeGenBase> T withColor(T biome, int color) {
        biome.setColor(color);
        return biome;
    }
}
