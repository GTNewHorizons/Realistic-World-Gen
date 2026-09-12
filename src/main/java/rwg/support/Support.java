package rwg.support;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.common.Loader;
import rwg.api.RWGBiomes;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.biomes.realistic.land.RealisticBiomeMountainChain;
import rwg.biomes.realistic.ocean.RealisticBiomeOcean;
import rwg.surface.SurfaceGrassland;
import rwg.terrain.TerrainHighland;
import rwg.terrain.TerrainMarsh;

public class Support {

    public static ArrayList<RealisticBiomeBase> biomes_snow;
    public static ArrayList<RealisticBiomeBase> biomes_cold;
    public static ArrayList<RealisticBiomeBase> biomes_hot;
    public static ArrayList<RealisticBiomeBase> biomes_wet;
    public static ArrayList<RealisticBiomeBase> biomes_small;
    public static ArrayList<RealisticBiomeBase> biomes_test;
    public static BiomeLists snow;
    public static BiomeLists cold;
    public static BiomeLists hot;
    public static BiomeLists wet;
    public static RealisticBiomeBase volcanoIsland;
    public static RealisticBiomeBase oceanShallowKelp;
    public static RealisticBiomeBase oceanShallowSnow;
    public static RealisticBiomeBase oceanShallowCold;
    public static RealisticBiomeBase oceanShallowTemperate;
    public static RealisticBiomeBase oceanShallowCoral;
    public static RealisticBiomeBase oceanShallowHot;
    public static RealisticBiomeBase oceanShallowWet;
    public static RealisticBiomeBase oceanDeep;
    public static RealisticBiomeBase oceanDeepSnow;
    public static RealisticBiomeBase oceanDeepCold;
    public static RealisticBiomeBase oceanDeepHot;
    public static RealisticBiomeBase oceanDeepWet;

    public enum BiomeCategory {
        SNOW,
        COLD,
        HOT,
        WET,
        SMALL,
        TEST
    }

    /** Independently selectable pools belonging to a land meta-biome. */
    public static final class BiomeLists {

        public final ArrayList<RealisticBiomeBase> core = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> border = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> coldBorder = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> hotBorder = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> veryColdBorder = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> veryHotBorder = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> littoral = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> small = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> island = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> smallIsland = new ArrayList<RealisticBiomeBase>();
        public final ArrayList<RealisticBiomeBase> largeIsland = new ArrayList<RealisticBiomeBase>();
    }

    public enum BiomePlacement {
        CORE,
        BORDER,
        COLD_BORDER,
        HOT_BORDER,
        VERY_COLD_BORDER,
        VERY_HOT_BORDER,
        LITTORAL,
        SMALL,
        ISLAND,
        SMALL_ISLAND,
        LARGE_ISLAND
    }

    public static void init() {
        init(true);
    }

    public static void init(boolean loadModBiomes) {
        snow = new BiomeLists();
        cold = new BiomeLists();
        hot = new BiomeLists();
        wet = new BiomeLists();
        biomes_snow = snow.core;
        biomes_cold = cold.core;
        biomes_hot = hot.core;
        biomes_wet = wet.core;
        biomes_small = new ArrayList<RealisticBiomeBase>();
        biomes_test = new ArrayList<RealisticBiomeBase>();
        RealisticBiomeBase.hotPlainsCanyonIsland.setDisplayName("Hot Plains (Canyon)");
        addBiome(RealisticBiomeBase.hotPlainsCanyonIsland, BiomeCategory.HOT, BiomePlacement.LARGE_ISLAND);
        addBiome(
                new RealisticBiomeSupport(
                        BiomeGenBase.mushroomIsland,
                        RWGBiomes.baseRiverWet,
                        new TerrainHighland(6f, 120f, 65f, 200f),
                        new SurfaceGrassland(
                                BiomeGenBase.mushroomIsland.topBlock,
                                BiomeGenBase.mushroomIsland.fillerBlock,
                                Blocks.stone,
                                Blocks.cobblestone)),
                BiomeCategory.WET,
                BiomePlacement.SMALL_ISLAND);
        addBiome(
                new RealisticBiomeSupport(
                        BiomeGenBase.jungle,
                        RWGBiomes.baseRiverWet,
                        new TerrainHighland(0f, 140f, 68f, 200f),
                        new SurfaceGrassland(
                                BiomeGenBase.jungle.topBlock,
                                BiomeGenBase.jungle.fillerBlock,
                                Blocks.stone,
                                Blocks.cobblestone)),
                BiomeCategory.WET);
        BiomeGenBase icePlainsSpikes = BiomeGenBase.getBiome(BiomeGenBase.icePlains.biomeID + 128);
        addBiome(
                new RealisticBiomeSupport(
                        icePlainsSpikes,
                        RWGBiomes.baseRiverIce,
                        new TerrainHighland(0f, 140f, 68f, 200f),
                        new SurfaceGrassland(
                                icePlainsSpikes.topBlock,
                                icePlainsSpikes.fillerBlock,
                                Blocks.stone,
                                Blocks.cobblestone)),
                BiomeCategory.SNOW,
                BiomePlacement.SMALL);
        BiomeGenBase sunflowerPlains = BiomeGenBase.getBiome(BiomeGenBase.plains.biomeID + 128);
        addBiome(
                new RealisticBiomeSupport(
                        sunflowerPlains,
                        RWGBiomes.baseRiverTemperate,
                        new TerrainMarsh(),
                        new SurfaceGrassland(
                                sunflowerPlains.topBlock,
                                sunflowerPlains.fillerBlock,
                                Blocks.stone,
                                Blocks.cobblestone)),
                BiomeCategory.COLD,
                BiomePlacement.SMALL);
        volcanoIsland = null;
        oceanShallowSnow = new RealisticBiomeOcean(
                RWGBiomes.baseOceanCold,
                true,
                false,
                "RealisticBiomeOceanSnowShallow");
        oceanShallowCold = new RealisticBiomeOcean(
                RWGBiomes.baseOceanCold,
                true,
                false,
                "RealisticBiomeOceanColdShallow");
        oceanShallowKelp = oceanShallowCold;
        oceanShallowTemperate = oceanShallowCold;
        oceanShallowCoral = oceanShallowTemperate;
        oceanShallowHot = new RealisticBiomeOcean(RWGBiomes.baseOceanHot, true, false, "RealisticBiomeOceanHotShallow");
        oceanShallowWet = new RealisticBiomeOcean(RWGBiomes.baseOceanWet, true, false, "RealisticBiomeOceanWetShallow");
        oceanDeepSnow = new RealisticBiomeOcean(RWGBiomes.baseOceanCold, false, false, "RealisticBiomeOceanSnowDeep");
        oceanDeepCold = new RealisticBiomeOcean(BiomeGenBase.deepOcean, false, false, "RealisticBiomeOceanColdDeep");
        oceanDeepHot = new RealisticBiomeOcean(RWGBiomes.baseOceanHot, false, false, "RealisticBiomeOceanHotDeep");
        oceanDeepWet = new RealisticBiomeOcean(RWGBiomes.baseOceanWet, false, false, "RealisticBiomeOceanWetDeep");
        oceanDeep = oceanDeepCold;

        if (loadModBiomes && Loader.isModLoaded("BiomesOPlenty")) {
            SupportBOP.init();
        }

        if (loadModBiomes && Loader.isModLoaded("ExtrabiomesXL")) {
            SupportEBXL.init();
        }

        if (loadModBiomes && Loader.isModLoaded("Thaumcraft")) {
            SupportTC.init();
        }
        if (loadModBiomes && Loader.isModLoaded("ChromatiCraft")) {
            SupportCC.init();
        }
        rebuildExtremeBorderMountains();
        /** ChromatiCraft is non-supported content. if this ever errors out in some way feel free to remove this. */
    }

    public static void rebuildExtremeBorderMountains() {
        snow.veryHotBorder.clear();
        cold.veryHotBorder.clear();
        hot.veryColdBorder.clear();
        wet.veryColdBorder.clear();
        addExtremeBorderMountains(snow.hotBorder, snow.veryHotBorder);
        addExtremeBorderMountains(cold.hotBorder, cold.veryHotBorder);
        addExtremeBorderMountains(hot.coldBorder, hot.veryColdBorder);
        addExtremeBorderMountains(wet.coldBorder, wet.veryColdBorder);
    }

    private static void addExtremeBorderMountains(List<RealisticBiomeBase> source, List<RealisticBiomeBase> target) {
        for (RealisticBiomeBase biome : source) {
            target.add(RealisticBiomeMountainChain.forBiome(biome));
        }
    }

    public static void addBiome(RealisticBiomeBase b, BiomeCategory cat) {
        addBiome(b, cat, cat == BiomeCategory.SMALL ? BiomePlacement.SMALL : BiomePlacement.CORE);
    }

    public static void addBiome(RealisticBiomeBase b, BiomeCategory cat, BiomePlacement placement) {
        try {
            if (placement != BiomePlacement.CORE && cat != BiomeCategory.SMALL && cat != BiomeCategory.TEST) {
                listFor(cat, placement).add(b);
                return;
            }
            switch (cat) {
                case SNOW:
                    biomes_snow.add(b);
                    break;
                case COLD:
                    biomes_cold.add(b);
                    break;
                case HOT:
                    biomes_hot.add(b);
                    break;
                case WET:
                    biomes_wet.add(b);
                    break;
                case SMALL:
                    biomes_small.add(b);
                    // Legacy SMALL integrations predate climate-specific pools and remain available in every climate.
                    snow.small.add(b);
                    cold.small.add(b);
                    hot.small.add(b);
                    wet.small.add(b);
                    break;
                case TEST:
                    biomes_test.add(b);
                    break;
            }
        } catch (Error e) {
            System.out.println("RWG Support: failed to add biome");
        }
    }

    private static ArrayList<RealisticBiomeBase> listFor(BiomeCategory category, BiomePlacement placement) {
        BiomeLists lists = category == BiomeCategory.SNOW ? snow
                : category == BiomeCategory.COLD ? cold : category == BiomeCategory.WET ? wet : hot;
        return placement == BiomePlacement.BORDER ? lists.border
                : placement == BiomePlacement.COLD_BORDER ? lists.coldBorder
                        : placement == BiomePlacement.HOT_BORDER ? lists.hotBorder
                                : placement == BiomePlacement.VERY_COLD_BORDER ? lists.veryColdBorder
                                        : placement == BiomePlacement.VERY_HOT_BORDER ? lists.veryHotBorder
                                                : placement == BiomePlacement.LITTORAL ? lists.littoral
                                                        : placement == BiomePlacement.SMALL ? lists.small
                                                                : placement == BiomePlacement.ISLAND ? lists.island
                                                                        : placement == BiomePlacement.SMALL_ISLAND
                                                                                ? lists.smallIsland
                                                                                : placement
                                                                                        == BiomePlacement.LARGE_ISLAND
                                                                                                ? lists.largeIsland
                                                                                                : lists.core;
    }
}
