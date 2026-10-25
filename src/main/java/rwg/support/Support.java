package rwg.support;

import java.util.ArrayList;

import cpw.mods.fml.common.Loader;
import rwg.biomes.realistic.RealisticBiomeBase;

public class Support {

    private static ArrayList<RealisticBiomeSupport> biomes;
    private static ArrayList<BiomeCategory> categories;

    public enum BiomeCategory {
        SNOW,
        COLD,
        HOT,
        WET,
        SMALL,
        TEST
    }

    public static void init() {
        biomes = new ArrayList<RealisticBiomeSupport>();
        categories = new ArrayList<BiomeCategory>();

        if (Loader.isModLoaded("BiomesOPlenty")) {
            SupportBOP.init();
        }

        if (Loader.isModLoaded("ExtrabiomesXL")) {
            SupportEBXL.init();
        }

        if (Loader.isModLoaded("Thaumcraft")) {
            SupportTC.init();
        }
        if (Loader.isModLoaded("ChromatiCraft")) {
            SupportCC.init();
        }
        /** ChromatiCraft is non-supported content. if this ever errors out in some way feel free to remove this. */
    }

    public static void addBiome(RealisticBiomeSupport b, BiomeCategory cat) {
        biomes.add(b);
        categories.add(cat);
    }

    /**
     * Legacy lists reproduce the old switch fall-through, where a biome was added to its own category and every
     * category after it. Existing worlds depend on that list order and size.
     */
    public static ArrayList<RealisticBiomeBase> getBiomes(BiomeCategory cat, boolean legacy) {
        ArrayList<RealisticBiomeBase> result = new ArrayList<RealisticBiomeBase>();
        for (int i = 0; i < biomes.size(); i++) {
            BiomeCategory c = categories.get(i);
            if (legacy ? c.ordinal() <= cat.ordinal() : c == cat) {
                result.add(biomes.get(i));
            }
        }
        return result;
    }
}
