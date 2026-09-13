package rwg.config;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.FMLInjectionData;

public class ConfigRWG {

    private static final String CONTINENTAL_CATEGORY = "Continental RWG";
    private static final int[] DEFAULT_BIOME_IDS = { 200, 202, 205, 207, 209, 211, 213, 214, 216, 218, 237, 223, 224,
            225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 240 };

    public static Configuration config;
    public static int[] biomeIDs = new int[26];

    public static boolean generateEmeralds = true;
    public static boolean enableCobblestoneBoulders = true;
    public static boolean generateCaves = true;
    public static boolean generateMineshafts = true;
    public static boolean generateVillages = true;
    public static boolean generateUndergroundLakes = true;
    public static boolean generateUndergroundLavaLakes = true;
    public static float minimumContinentWidth = 2700f;
    public static float maximumContinentWidth = 2700f;
    public static float averageOceanWidth = 100f;
    public static float minimumOceanWidth = 50f;
    public static float maximumOceanFraction = 1.0f;
    public static float minimumIslandWidth = 300f;
    public static float maximumIslandWidth = 600f;
    public static float islandPlacementChance = 0.30f;
    public static float largeIslandVolcanoChance = 0.15f;
    public static float averageLandmarksPerTypeAndContinent = 0.25f;
    public static int landmassOffsetX = 0;
    public static int landmassOffsetZ = 0;
    public static int biomeOffsetX = 0;
    public static int biomeOffsetZ = 0;

    public static void init(FMLPreInitializationEvent event) {
        init(event.getSuggestedConfigurationFile());
    }

    public static void init(File file) {
        ensureForgeConfigEnvironment(file);
        config = new Configuration(file);
        for (int c = 0; c < biomeIDs.length; c++) {
            biomeIDs[c] = DEFAULT_BIOME_IDS[c];
        }

        try {
            config.load();
            renameOldProperties();
            biomeIDs[0] = config.get("biome ids", "00 Ice River", DEFAULT_BIOME_IDS[0], "Ice River").getInt();
            biomeIDs[1] = config.get("biome ids", "01 Cold River", DEFAULT_BIOME_IDS[1], "Cold River").getInt();
            biomeIDs[2] = config.get("biome ids", "02 Temperate River", DEFAULT_BIOME_IDS[2], "Temperate River")
                    .getInt();
            biomeIDs[3] = config.get("biome ids", "03 Hot River", DEFAULT_BIOME_IDS[3], "Hot River").getInt();
            biomeIDs[4] = config.get("biome ids", "04 Wet River", DEFAULT_BIOME_IDS[4], "Wet River").getInt();
            biomeIDs[5] = config.get("biome ids", "05 River Oasis", DEFAULT_BIOME_IDS[5], "River Oasis").getInt();
            biomeIDs[6] = config.get("biome ids", "06 Ice Ocean", DEFAULT_BIOME_IDS[6], "Ice Ocean").getInt();
            biomeIDs[7] = config.get("biome ids", "07 Cold Ocean", DEFAULT_BIOME_IDS[7], "Cold Ocean").getInt();
            biomeIDs[8] = config.get("biome ids", "08 Temperate Ocean", DEFAULT_BIOME_IDS[8], "Temperate Ocean")
                    .getInt();
            biomeIDs[9] = config.get("biome ids", "09 Hot Ocean", DEFAULT_BIOME_IDS[9], "Hot Ocean").getInt();
            biomeIDs[10] = config.get("biome ids", "10 Wet Ocean", DEFAULT_BIOME_IDS[10], "Wet Ocean").getInt();
            biomeIDs[11] = config.get("biome ids", "11 Ocean Oasis", DEFAULT_BIOME_IDS[11], "Ocean Oasis").getInt();
            biomeIDs[12] = config.get("biome ids", "12 Snow Desert", DEFAULT_BIOME_IDS[12], "Snow Desert").getInt();
            biomeIDs[13] = config.get("biome ids", "13 Snow Forest", DEFAULT_BIOME_IDS[13], "Snow Forest").getInt();
            biomeIDs[14] = config.get("biome ids", "14 Cold Plains", DEFAULT_BIOME_IDS[14], "Cold Plains").getInt();
            biomeIDs[15] = config.get("biome ids", "15 Cold Forest", DEFAULT_BIOME_IDS[15], "Cold Forest").getInt();
            biomeIDs[16] = config.get("biome ids", "16 Hot Plains", DEFAULT_BIOME_IDS[16], "Hot Plains").getInt();
            biomeIDs[17] = config.get("biome ids", "17 Hot Forest", DEFAULT_BIOME_IDS[17], "Hot Forest").getInt();
            biomeIDs[18] = config.get("biome ids", "18 Hot Desert", DEFAULT_BIOME_IDS[18], "Hot Desert").getInt();
            biomeIDs[19] = config.get("biome ids", "19 Plains (RWG)", DEFAULT_BIOME_IDS[19], "Plains").getInt();
            biomeIDs[20] = config.get("biome ids", "20 Tropical Island", DEFAULT_BIOME_IDS[20], "Tropical Island")
                    .getInt();
            biomeIDs[21] = config.get("biome ids", "21 Redwood", DEFAULT_BIOME_IDS[21], "Redwood").getInt();
            biomeIDs[22] = config.get("biome ids", "22 Jungle (RWG)", DEFAULT_BIOME_IDS[22], "Jungle").getInt();
            biomeIDs[23] = config.get("biome ids", "23 Oasis", DEFAULT_BIOME_IDS[23], "Oasis").getInt();
            biomeIDs[24] = config.get("biome ids", "24 Temperate Forest", DEFAULT_BIOME_IDS[24], "Temperate Forest")
                    .getInt();
            biomeIDs[25] = config.get("biome ids", "25 Jungle Mesa", DEFAULT_BIOME_IDS[25], "Jungle Mesa").getInt();

            generateEmeralds = config.getBoolean("Generate Emeralds", "Settings", true, "");
            enableCobblestoneBoulders = config.getBoolean("Enable Cobblestone Boulders", "Settings", true, "");
            generateCaves = config.getBoolean("Generate Caves", "Settings", true, "");
            generateMineshafts = config.getBoolean("Generate Mineshafts", "Settings", true, "");
            generateVillages = config.getBoolean("Generate Villages", "Settings", true, "");
            generateUndergroundLakes = config.getBoolean("Generate Underground Lakes", "Settings", true, "");
            generateUndergroundLavaLakes = config.getBoolean("Generate Underground Lava Lakes", "Settings", true, "");
            String worldgenWarning = "Changing this after creating a world causes borders between old and new chunks.";
            String continentWidthDescription = "Distance in blocks from a continent's Voronoi centre to its coast. "
                    + worldgenWarning;
            minimumContinentWidth = config.getFloat(
                    "Minimum Continent Width",
                    CONTINENTAL_CATEGORY,
                    2700f,
                    1f,
                    100000f,
                    continentWidthDescription);
            maximumContinentWidth = config.getFloat(
                    "Maximum Continent Width",
                    CONTINENTAL_CATEGORY,
                    2700f,
                    1f,
                    100000f,
                    continentWidthDescription);
            averageOceanWidth = config.getFloat(
                    "Average Ocean Width",
                    CONTINENTAL_CATEGORY,
                    100f,
                    1f,
                    100000f,
                    "Average ocean band width in blocks; continent plus ocean width sets the Voronoi radius. "
                            + worldgenWarning);
            minimumOceanWidth = config.getFloat(
                    "Minimum Ocean Width",
                    CONTINENTAL_CATEGORY,
                    50f,
                    0f,
                    100000f,
                    "Minimum total water gap in blocks between two maximum-sized continents. " + worldgenWarning);
            maximumOceanFraction = config.getFloat(
                    "Maximum Ocean Fraction",
                    CONTINENTAL_CATEGORY,
                    1.0f,
                    0f,
                    1f,
                    "Approximate upper limit on the fraction of the world covered by ocean. 1.0 disables the limit. "
                            + "For reference, the default settings produce roughly 50% ocean. Lower values dilate "
                            + "continents after placement, so oceans that normally separate continents may become "
                            + "interior seas. The result is approximate and varies with the world seed and the other "
                            + "continent settings. "
                            + worldgenWarning);
            String islandWidthDescription = "Distance in blocks from an island seed to its coast. " + worldgenWarning;
            minimumIslandWidth = config
                    .getFloat("Minimum Island Width", CONTINENTAL_CATEGORY, 300f, 1f, 100000f, islandWidthDescription);
            maximumIslandWidth = config
                    .getFloat("Maximum Island Width", CONTINENTAL_CATEGORY, 600f, 1f, 100000f, islandWidthDescription);
            islandPlacementChance = config.getFloat(
                    "Island Placement Chance",
                    CONTINENTAL_CATEGORY,
                    0.30f,
                    0f,
                    1f,
                    "Chance to retain an island after valid island seeds have been identified. " + worldgenWarning);
            largeIslandVolcanoChance = config.getFloat(
                    "Large Island Volcano Chance",
                    CONTINENTAL_CATEGORY,
                    0.15f,
                    0f,
                    1f,
                    "Chance for a large island to contain a volcano. " + worldgenWarning);
            averageLandmarksPerTypeAndContinent = config.getFloat(
                    "Average Landmarks Per Type And Continent",
                    CONTINENTAL_CATEGORY,
                    0.25f,
                    0f,
                    100f,
                    "Approximate average number of each enabled landmark type per continent. Candidate probability "
                            + "is corrected for the configured continental land fraction. "
                            + worldgenWarning);
            config.setCategoryPropertyOrder(
                    CONTINENTAL_CATEGORY,
                    Arrays.asList(
                            "Minimum Continent Width",
                            "Maximum Continent Width",
                            "Average Ocean Width",
                            "Minimum Ocean Width",
                            "Maximum Ocean Fraction",
                            "Minimum Island Width",
                            "Maximum Island Width",
                            "Island Placement Chance",
                            "Large Island Volcano Chance",
                            "Average Landmarks Per Type And Continent"));
            String expertCategory = "Expert / Worldgen Migration";
            config.setCategoryComment(
                    expertCategory,
                    "EXPERT ONLY! With the same seed, coordinates, and biome, RWG produces the same terrain shape. "
                            + "New world-generation versions can move continents and biomes, so these offsets let you "
                            + "place an existing base inside the appropriate biome and on land again. After aligning the "
                            + "new map with runPreview, Server Utilities can restore claimed chunks from backup into the "
                            + "surrounding terrain more seamlessly. Finalize these values before generating new chunks.");
            String offsetWarning = "Expert migration setting. Change only before generating new chunks. Positive values "
                    + "sample the generator at larger coordinates, moving the corresponding map features toward negative "
                    + "coordinates.";
            landmassOffsetX = config.getInt(
                    "Landmass Placement Offset X",
                    expertCategory,
                    0,
                    -30000000,
                    30000000,
                    "Offsets continent and island placement without moving biome terrain noise. " + offsetWarning);
            landmassOffsetZ = config.getInt(
                    "Landmass Placement Offset Z",
                    expertCategory,
                    0,
                    -30000000,
                    30000000,
                    "Offsets continent and island placement without moving biome terrain noise. " + offsetWarning);
            biomeOffsetX = config.getInt(
                    "Biome Placement Offset X",
                    expertCategory,
                    0,
                    -30000000,
                    30000000,
                    "Offsets climate and biome selection without moving the selected biome's terrain noise. "
                            + offsetWarning);
            biomeOffsetZ = config.getInt(
                    "Biome Placement Offset Z",
                    expertCategory,
                    0,
                    -30000000,
                    30000000,
                    "Offsets climate and biome selection without moving the selected biome's terrain noise. "
                            + offsetWarning);
        } catch (Exception e) {
            for (int c = 0; c < biomeIDs.length; c++) {
                biomeIDs[c] = DEFAULT_BIOME_IDS[c];
            }
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    private static void ensureForgeConfigEnvironment(File configFile) {
        if (FMLInjectionData.data()[6] != null) return;
        File configDirectory = configFile.getAbsoluteFile().getParentFile();
        File minecraftDirectory = configDirectory != null && "config".equals(configDirectory.getName())
                ? configDirectory.getParentFile()
                : new File(System.getProperty("user.dir"));
        try {
            Field minecraftHome = FMLInjectionData.class.getDeclaredField("minecraftHome");
            minecraftHome.setAccessible(true);
            minecraftHome.set(null, minecraftDirectory);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not initialize Forge configuration for the offline preview",
                    exception);
        }
    }

    private static void renameOldProperties() {
        config.moveProperty("Continental World", "Minimum Continent Width", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Maximum Continent Width", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Average Ocean Width", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Minimum Ocean Width", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Maximum Ocean Fraction", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Minimum Island Width", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Maximum Island Width", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Island Placement Chance", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Large Island Volcano Chance", CONTINENTAL_CATEGORY);
        config.moveProperty("Continental World", "Average Volcano Count Per Continent", CONTINENTAL_CATEGORY);
        config.renameProperty(
                CONTINENTAL_CATEGORY,
                "Average Volcano Count Per Continent",
                "Average Landmarks Per Type And Continent");
        config.moveProperty("Continental World", "Large Ocean Chance", CONTINENTAL_CATEGORY);
        config.getCategory(CONTINENTAL_CATEGORY).remove("Large Ocean Chance");
        config.renameProperty("biome ids", "00 rwg_riverIce", "00 Ice River");
        config.renameProperty("biome ids", "01 rwg_riverCold", "01 Cold River");
        config.renameProperty("biome ids", "02 rwg_riverTemperate", "02 Temperate River");
        config.renameProperty("biome ids", "03 rwg_riverHot", "03 Hot River");
        config.renameProperty("biome ids", "04 rwg_riverWet", "04 Wet River");
        config.renameProperty("biome ids", "05 rwg_riverOasis", "05 River Oasis");
        config.renameProperty("biome ids", "06 rwg_oceanIce", "06 Ice Ocean");
        config.renameProperty("biome ids", "07 rwg_oceanCold", "07 Cold Ocean");
        config.renameProperty("biome ids", "08 rwg_oceanTemperate", "08 Temperate Ocean");
        config.renameProperty("biome ids", "09 rwg_oceanHot", "09 Hot Ocean");
        config.renameProperty("biome ids", "10 rwg_oceanWet", "10 Wet Ocean");
        config.renameProperty("biome ids", "11 rwg_oceanOasis", "11 Ocean Oasis");
        config.renameProperty("biome ids", "12 rwg_snowDesert", "12 Snow Desert");
        config.renameProperty("biome ids", "13 rwg_snowForest", "13 Snow Forest");
        config.renameProperty("biome ids", "14 rwg_coldPlains", "14 Cold Plains");
        config.renameProperty("biome ids", "15 rwg_coldForest", "15 Cold Forest");
        config.renameProperty("biome ids", "16 rwg_hotPlains", "16 Hot Plains");
        config.renameProperty("biome ids", "17 rwg_hotForest", "17 Hot Forest");
        config.renameProperty("biome ids", "18 rwg_hotDesert", "18 Hot Desert");
        config.renameProperty("biome ids", "19 rwg_plains", "19 Plains (RWG)");
        config.renameProperty("biome ids", "20 rwg_tropical", "20 Tropical Island");
        config.renameProperty("biome ids", "21 rwg_redwood", "21 Redwood");
        config.renameProperty("biome ids", "22 rwg_jungle", "22 Jungle (RWG)");
        config.renameProperty("biome ids", "23 rwg_oasis", "23 Oasis");
        config.renameProperty("biome ids", "24 rwg_temperateForest", "24 Temperate Forest");
    }
}
