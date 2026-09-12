package rwg.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeCache;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import gnu.trove.map.hash.TLongByteHashMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.biomes.realistic.ocean.RealisticBiomeIslandVolcano;
import rwg.biomes.realistic.ocean.RealisticBiomeOcean;
import rwg.config.ConfigRWG;
import rwg.support.Support;
import rwg.support.Support.BiomePlacement;
import rwg.util.CellNoise;
import rwg.util.ContinentalNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;
import rwg.util.PoissonPointNoise;

public class ChunkManagerRealistic extends WorldChunkManager {

    private static final float SHALLOW_OCEAN_WIDTH = 300f;
    private static final float CLIMATE_WARP_SCALE_MULTIPLIER = .4f;
    private static final float CLIMATE_WARP_STRENGTH_MULTIPLIER = .8f;
    private static final float BIOME_WARP_SCALE_MULTIPLIER = .4f;
    private static final float BIOME_WARP_STRENGTH_MULTIPLIER = .175f;
    // Compensate for the spatial bias of the warped cell-noise values. Snow occupies approximately
    // 25% less area than before, with the Cold/Hot boundary calibrated from preview coverage.
    private static final float SNOW_CLIMATE_LIMIT = .16875f;
    private static final float COLD_CLIMATE_LIMIT = .545f;
    private static final float HOT_CLIMATE_LIMIT = .78f;
    private static final float THREE_CLIMATE_SNOW_LIMIT = .2475f;
    private static final float THREE_CLIMATE_COLD_LIMIT = .62375f;
    private static final double CLIMATE_BORDER_DISTANCE_DIFFERENCE = 288D;
    private static final float LITTORAL_WIDTH = 432f;
    private static final double SMALL_BIOME_RADIUS = 75D;
    private static final int VOLCANO_RIVER_SAMPLE_SPACING = 16;

    private BiomeCache biomeCache;
    private List biomesToSpawnIn;

    private NoiseGenerator perlin;
    private CellNoise cell;

    private CellNoise biomecell;
    private NoiseGenerator climateWarp;
    private PoissonPointNoise smallBiomePoints;
    private final ThreadLocal<double[]> smallPointSample = new ThreadLocal<double[]>() {

        @Override
        protected double[] initialValue() {
            return new double[5];
        }
    };
    private final ThreadLocal<double[]> climatePointSample = new ThreadLocal<double[]>() {

        @Override
        protected double[] initialValue() {
            return new double[4];
        }
    };
    private final ThreadLocal<double[]> climateWarpSample = new ThreadLocal<double[]>() {

        @Override
        protected double[] initialValue() {
            return new double[2];
        }
    };
    private float climateWidth = 1400f;
    private float biomeWidth = 500f;
    private boolean continental;
    private ContinentalNoise continents;

    private ArrayList<RealisticBiomeBase> biomes_snow;
    private ArrayList<RealisticBiomeBase> biomes_cold;
    private ArrayList<RealisticBiomeBase> biomes_hot;
    private ArrayList<RealisticBiomeBase> biomes_wet;
    private ArrayList<RealisticBiomeBase> biomes_small;
    private ArrayList<RealisticBiomeBase> biomes_test;
    private ArrayList<RealisticBiomeBase>[] borderBiomes;
    private ArrayList<RealisticBiomeBase>[] coldBorderBiomes;
    private ArrayList<RealisticBiomeBase>[] hotBorderBiomes;
    private ArrayList<RealisticBiomeBase>[] veryColdBorderBiomes;
    private ArrayList<RealisticBiomeBase>[] veryHotBorderBiomes;
    private ArrayList<RealisticBiomeBase>[] littoralBiomes;
    private ArrayList<RealisticBiomeBase>[] smallBiomes;
    private ArrayList<RealisticBiomeBase>[] islandBiomes;
    private ArrayList<RealisticBiomeBase>[] smallIslandBiomes;
    private ArrayList<RealisticBiomeBase>[] largeIslandBiomes;
    private int biomes_snowLength;
    private int biomes_coldLength;
    private int biomes_hotLength;
    private int biomes_wetLength;
    private int biomes_smallLength;
    private int biomes_testLength;

    private boolean wetEnabled;
    private boolean smallEnabled;

    private float[] borderNoise;

    protected ChunkManagerRealistic() {
        this.biomeCache = new BiomeCache(this);
        this.biomesToSpawnIn = new ArrayList();
        borderNoise = new float[256];
    }

    public ChunkManagerRealistic(World par1World) {
        this(par1World, false);
    }

    public ChunkManagerRealistic(World par1World, boolean continental) {
        this(par1World.getSeed(), continental);
    }

    public ChunkManagerRealistic(long seed, boolean continental) {
        this(seed, continental, 1400f, 500f);
    }

    public ChunkManagerRealistic(long seed, boolean continental, float climateWidth, float biomeWidth) {
        this();
        this.continental = continental;
        this.climateWidth = climateWidth;
        this.biomeWidth = biomeWidth;

        perlin = NoiseSelector.createNoiseGenerator(seed);
        cell = new CellNoise(seed, (short) 0);
        cell.setUseDistance(true);
        biomecell = new CellNoise(seed, (short) 0);
        climateWarp = NoiseSelector.createNoiseGenerator(seed ^ 0xBB67AE8584CAA73BL);
        // Point density is inversely proportional to spacing squared; 1020 instead of 1610 yields about 2.5x as many.
        smallBiomePoints = new PoissonPointNoise(seed ^ 0x510E527FADE682D1L, 1020D, 4);
        if (continental) {
            continents = new ContinentalNoise(seed ^ 0x6A09E667F3BCC909L);
        }

        biomes_snow = new ArrayList<RealisticBiomeBase>();
        biomes_cold = new ArrayList<RealisticBiomeBase>();
        biomes_hot = new ArrayList<RealisticBiomeBase>();
        biomes_wet = new ArrayList<RealisticBiomeBase>();
        biomes_small = new ArrayList<RealisticBiomeBase>();
        biomes_test = new ArrayList<RealisticBiomeBase>();
        borderBiomes = lists(Support.snow.border, Support.cold.border, Support.hot.border, Support.wet.border);
        coldBorderBiomes = lists(
                Support.snow.coldBorder,
                Support.cold.coldBorder,
                Support.hot.coldBorder,
                Support.wet.coldBorder);
        hotBorderBiomes = lists(
                Support.snow.hotBorder,
                Support.cold.hotBorder,
                Support.hot.hotBorder,
                Support.wet.hotBorder);
        veryColdBorderBiomes = lists(
                Support.snow.veryColdBorder,
                Support.cold.veryColdBorder,
                Support.hot.veryColdBorder,
                Support.wet.veryColdBorder);
        veryHotBorderBiomes = lists(
                Support.snow.veryHotBorder,
                Support.cold.veryHotBorder,
                Support.hot.veryHotBorder,
                Support.wet.veryHotBorder);
        littoralBiomes = lists(
                Support.snow.littoral,
                Support.cold.littoral,
                Support.hot.littoral,
                Support.wet.littoral);
        smallBiomes = lists(Support.snow.small, Support.cold.small, Support.hot.small, Support.wet.small);
        islandBiomes = lists(Support.snow.island, Support.cold.island, Support.hot.island, Support.wet.island);
        smallIslandBiomes = lists(
                Support.snow.smallIsland,
                Support.cold.smallIsland,
                Support.hot.smallIsland,
                Support.wet.smallIsland);
        largeIslandBiomes = lists(
                Support.snow.largeIsland,
                Support.cold.largeIsland,
                Support.hot.largeIsland,
                Support.wet.largeIsland);

        biomes_snow.add(RealisticBiomeBase.polar);
        biomes_snow.add(RealisticBiomeBase.snowHills);
        biomes_snow.add(RealisticBiomeBase.snowRivers);
        biomes_snow.add(RealisticBiomeBase.snowLakes);
        biomes_snow.add(RealisticBiomeBase.redwoodSnow);

        biomes_cold.add(RealisticBiomeBase.tundraHills);
        biomes_cold.add(RealisticBiomeBase.tundraPlains);
        biomes_cold.add(RealisticBiomeBase.taigaHills);
        biomes_cold.add(RealisticBiomeBase.taigaPlains);
        biomes_cold.add(RealisticBiomeBase.redwood);
        biomes_cold.add(RealisticBiomeBase.darkRedwood);
        biomes_cold.add(RealisticBiomeBase.darkRedwoodPlains);
        biomes_cold.add(RealisticBiomeBase.woodhills);
        biomes_cold.add(RealisticBiomeBase.woodmountains);

        biomes_cold.add(RealisticBiomeBase.woodhills);
        biomes_cold.add(RealisticBiomeBase.woodmountains);

        biomes_hot.add(RealisticBiomeBase.duneValleyForest);
        biomes_hot.add(RealisticBiomeBase.savanna);
        biomes_hot.add(RealisticBiomeBase.savannaForest);
        biomes_hot.add(RealisticBiomeBase.savannaDunes);
        biomes_hot.add(RealisticBiomeBase.stoneMountains);
        biomes_hot.add(RealisticBiomeBase.stoneMountainsCactus);
        biomes_hot.add(RealisticBiomeBase.hotForest);
        biomes_hot.add(RealisticBiomeBase.hotRedwood);
        biomes_hot.add(RealisticBiomeBase.canyonForest);
        biomes_hot.add(RealisticBiomeBase.mesaPlains);
        biomes_hot.add(RealisticBiomeBase.desert);
        biomes_hot.add(RealisticBiomeBase.desertMountains);
        biomes_hot.add(RealisticBiomeBase.duneValley);
        biomes_hot.add(RealisticBiomeBase.oasis);
        biomes_hot.add(RealisticBiomeBase.redDesertMountains);
        biomes_hot.add(RealisticBiomeBase.redDesertOasis);
        biomes_hot.add(RealisticBiomeBase.canyon);
        biomes_hot.add(RealisticBiomeBase.mesa);

        biomes_snow.addAll(Support.biomes_snow);
        biomes_cold.addAll(Support.biomes_cold);
        biomes_hot.addAll(Support.biomes_hot);
        biomes_wet.addAll(Support.biomes_wet);
        biomes_small.addAll(Support.biomes_small);
        biomes_test.addAll(Support.biomes_test);

        biomes_snowLength = biomes_snow.size();
        biomes_coldLength = biomes_cold.size();
        biomes_hotLength = biomes_hot.size();
        biomes_wetLength = biomes_wet.size();
        biomes_smallLength = biomes_small.size();
        biomes_testLength = biomes_test.size();

        wetEnabled = false;
        if (biomes_wetLength > 0) {
            wetEnabled = true;
        }

        smallEnabled = false;
        if (biomes_smallLength > 1) {
            smallEnabled = true;
        }
    }

    public int[] getBiomesGens(int par1, int par2, int par3, int par4) {
        int[] d = new int[par3 * par4];

        for (int i = 0; i < par3; i++) {
            for (int j = 0; j < par4; j++) {
                d[j * par3 + i] = getBiomeGenAt(par1 + i, par2 + j).biomeID;
            }
        }
        return d;
    }

    public RealisticBiomeBase[] getBiomesGensData(int par1, int par2, int par3, int par4) {
        RealisticBiomeBase[] data = new RealisticBiomeBase[par3 * par4];

        for (int i = 0; i < par3; i++) {
            for (int j = 0; j < par4; j++) {
                data[j * par3 + i] = getBiomeDataAt(par1 + i, par2 + j);
            }
        }
        return data;
    }

    public float getContinentValue(int x, int y) {
        return continental ? continents.getValue(landmassX(x), landmassZ(y)) : (getLegacyOceanValue(x, y) - 1f) * 100f;
    }

    List<RealisticBiomeBase> getConfiguredBiomes() {
        ArrayList<RealisticBiomeBase> result = new ArrayList<RealisticBiomeBase>();
        result.addAll(biomes_snow);
        result.addAll(biomes_cold);
        result.addAll(biomes_hot);
        result.addAll(biomes_wet);
        result.addAll(biomes_small);
        result.addAll(biomes_test);
        for (ArrayList<RealisticBiomeBase> list : borderBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : coldBorderBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : hotBorderBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : veryColdBorderBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : veryHotBorderBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : littoralBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : smallBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : islandBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : smallIslandBiomes) result.addAll(list);
        for (ArrayList<RealisticBiomeBase> list : largeIslandBiomes) result.addAll(list);
        result.add(RealisticBiomeBase.coastIce);
        result.add(RealisticBiomeBase.coastDunes);
        result.add(Support.oceanShallowSnow);
        result.add(Support.oceanShallowCold);
        result.add(Support.oceanShallowKelp);
        result.add(Support.oceanShallowTemperate);
        result.add(Support.oceanShallowCoral);
        result.add(Support.oceanShallowHot);
        result.add(Support.oceanShallowWet);
        result.add(Support.oceanDeep);
        result.add(Support.oceanDeepSnow);
        result.add(Support.oceanDeepCold);
        result.add(Support.oceanDeepHot);
        result.add(Support.oceanDeepWet);
        if (Support.volcanoIsland != null) result.add(Support.volcanoIsland);
        return new ArrayList<RealisticBiomeBase>(new LinkedHashSet<RealisticBiomeBase>(result));
    }

    int[] getConfiguredBiomeCategories() {
        int[] categories = new int[256];
        Arrays.fill(categories, 3);
        setCategory(categories, biomes_snow, 1);
        setCategory(categories, biomes_cold, 2);
        setCategory(categories, biomes_hot, 3);
        setCategory(categories, biomes_wet, 4);
        for (int index = 0; index < 4; index++) {
            setCategory(categories, borderBiomes[index], index + 1);
            setCategory(categories, coldBorderBiomes[index], index + 1);
            setCategory(categories, hotBorderBiomes[index], index + 1);
            setCategory(categories, veryColdBorderBiomes[index], index + 1);
            setCategory(categories, veryHotBorderBiomes[index], index + 1);
            setCategory(categories, littoralBiomes[index], index + 1);
            setCategory(categories, smallBiomes[index], index + 1);
            setCategory(categories, islandBiomes[index], index + 1);
            setCategory(categories, smallIslandBiomes[index], index + 1);
            setCategory(categories, largeIslandBiomes[index], index + 1);
        }
        setCategory(categories, RealisticBiomeBase.coastIce, 1);
        setCategory(categories, RealisticBiomeBase.coastDunes, 3);
        setCategory(categories, Support.volcanoIsland, 5);
        setCategory(categories, Support.oceanShallowSnow, 0);
        setCategory(categories, Support.oceanShallowCold, 0);
        setCategory(categories, Support.oceanShallowKelp, 0);
        setCategory(categories, Support.oceanShallowTemperate, 0);
        setCategory(categories, Support.oceanShallowCoral, 0);
        setCategory(categories, Support.oceanShallowHot, 0);
        setCategory(categories, Support.oceanShallowWet, 0);
        setCategory(categories, Support.oceanDeep, 0);
        setCategory(categories, Support.oceanDeepSnow, 0);
        setCategory(categories, Support.oceanDeepCold, 0);
        setCategory(categories, Support.oceanDeepHot, 0);
        setCategory(categories, Support.oceanDeepWet, 0);
        return categories;
    }

    int getMetaBiomeAt(int x, int y) {
        long coords = ChunkCoordIntPair.chunkXZ2Int(x, y);
        if (!metaBiomeDataMap.containsKey(coords)) getBiomeDataAt(x, y);
        return metaBiomeDataMap.get(coords);
    }

    int getPlacementAt(int metaBiome, RealisticBiomeBase biome) {
        if (metaBiome <= 0 || metaBiome > 4) return BiomePlacement.CORE.ordinal();
        int index = metaBiome - 1;
        if (islandBiomes[index].contains(biome)) return BiomePlacement.ISLAND.ordinal();
        if (smallIslandBiomes[index].contains(biome)) return BiomePlacement.SMALL_ISLAND.ordinal();
        if (largeIslandBiomes[index].contains(biome)) return BiomePlacement.LARGE_ISLAND.ordinal();
        if (smallBiomes[index].contains(biome)) return BiomePlacement.SMALL.ordinal();
        if (coldBorderBiomes[index].contains(biome)) return BiomePlacement.COLD_BORDER.ordinal();
        if (hotBorderBiomes[index].contains(biome)) return BiomePlacement.HOT_BORDER.ordinal();
        if (veryColdBorderBiomes[index].contains(biome)) return BiomePlacement.VERY_COLD_BORDER.ordinal();
        if (veryHotBorderBiomes[index].contains(biome)) return BiomePlacement.VERY_HOT_BORDER.ordinal();
        if (littoralBiomes[index].contains(biome)) return BiomePlacement.LITTORAL.ordinal();
        if (borderBiomes[index].contains(biome)) return BiomePlacement.BORDER.ordinal();
        return BiomePlacement.CORE.ordinal();
    }

    List<RealisticBiomeBase> getBiomesFor(int metaBiome, BiomePlacement placement) {
        if (metaBiome <= 0 || metaBiome > 4) return new ArrayList<RealisticBiomeBase>();
        int index = metaBiome - 1;
        if (placement == BiomePlacement.BORDER) return borderBiomes[index];
        if (placement == BiomePlacement.COLD_BORDER) return coldBorderBiomes[index];
        if (placement == BiomePlacement.HOT_BORDER) return hotBorderBiomes[index];
        if (placement == BiomePlacement.VERY_COLD_BORDER) return veryColdBorderBiomes[index];
        if (placement == BiomePlacement.VERY_HOT_BORDER) return veryHotBorderBiomes[index];
        if (placement == BiomePlacement.LITTORAL) return littoralBiomes[index];
        if (placement == BiomePlacement.SMALL) return smallBiomes[index];
        if (placement == BiomePlacement.ISLAND) return islandBiomes[index];
        if (placement == BiomePlacement.SMALL_ISLAND) return smallIslandBiomes[index];
        if (placement == BiomePlacement.LARGE_ISLAND) return largeIslandBiomes[index];
        return index == 0 ? biomes_snow : index == 1 ? biomes_cold : index == 2 ? biomes_hot : biomes_wet;
    }

    private static void setCategory(int[] categories, List<RealisticBiomeBase> biomes, int category) {
        for (RealisticBiomeBase biome : biomes) setCategory(categories, biome, category);
    }

    private static void setCategory(int[] categories, RealisticBiomeBase biome, int category) {
        if (biome != null) categories[biome.biomeID] = category;
    }

    public long getVolcanoCoordinates(int x, int y) {
        if (!continental) return Long.MIN_VALUE;
        long coordinates = continents.getVolcanoCoordinates(landmassX(x), landmassZ(y));
        return coordinates != Long.MIN_VALUE && canGenerateVolcanoAt(x, y) ? coordinates : Long.MIN_VALUE;
    }

    public long getVolcanoVicinityCoordinates(int x, int y) {
        if (!continental) return Long.MIN_VALUE;
        long coordinates = continents.getVolcanoVicinityCoordinates(landmassX(x), landmassZ(y));
        return coordinates != Long.MIN_VALUE && canGenerateVolcanoAt(x, y) ? coordinates : Long.MIN_VALUE;
    }

    private boolean canGenerateVolcanoAt(int x, int y) {
        if (!(Support.volcanoIsland instanceof RealisticBiomeIslandVolcano)) return false;
        int landmassX = landmassX(x);
        int landmassZ = landmassZ(y);
        long key = continents.getVolcanoSeedKey(landmassX, landmassZ);
        if (key == Long.MIN_VALUE) return false;
        if (volcanoEligibilityMap.containsKey(key)) return volcanoEligibilityMap.get(key) == 1;

        long centerCoordinates = continents.getVolcanoCenterCoordinates(landmassX, landmassZ);
        int centerX = (int) (centerCoordinates >> 32) - ConfigRWG.landmassOffsetX;
        int centerZ = (int) centerCoordinates - ConfigRWG.landmassOffsetZ;
        boolean eligible = !hasRiverNearVolcano(centerX, centerZ)
                && ((RealisticBiomeIslandVolcano) Support.volcanoIsland)
                        .canGenerateAtHeight(getVolcanoBaseHeight(x, y));
        if (volcanoEligibilityMap.size() > 256) volcanoEligibilityMap.clear();
        volcanoEligibilityMap.put(key, (byte) (eligible ? 1 : 2));
        return eligible;
    }

    private boolean hasRiverNearVolcano(int centerX, int centerZ) {
        int radius = (int) Math.ceil(ContinentalNoise.VOLCANO_ISLAND_RADIUS);
        int radiusSquared = radius * radius;
        for (int offsetX = -radius; offsetX <= radius; offsetX += VOLCANO_RIVER_SAMPLE_SPACING) {
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ += VOLCANO_RIVER_SAMPLE_SPACING) {
                if (offsetX * offsetX + offsetZ * offsetZ <= radiusSquared
                        && getRawRiverStrength(centerX + offsetX, centerZ + offsetZ) < 0f) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Converts the new continent distance field to the 0-2 range expected by the existing coast terrain
     * implementations.
     */
    public float getTerrainOceanValue(int x, int y) {
        return continental ? getTerrainOceanValue(getContinentValue(x, y)) : getLegacyOceanValue(x, y);
    }

    public float getTerrainOceanValue(float continent) {
        float value = 1f + continent / 100f;
        return value < 0f ? 0f : value > 2f ? 2f : value;
    }

    /** @deprecated Use {@link #getTerrainOceanValue(int, int)}. */
    @Deprecated
    public float getOceanValue(int x, int y) {
        return getTerrainOceanValue(x, y);
    }

    private float getLegacyOceanValue(int x, int y) {
        float sample1 = perlin.noise2(x / 1200f, y / 1200f);
        float sample2;
        float highest = sample1 == 0f ? 1f : 0f;

        if (diff(sample1, sample2 = perlin.noise2((x - 100f) / 1200f, y / 1200f))) {
            highest = Math.max(highest, 1f - Math.abs(sample1 / Math.abs(sample1 - sample2)));
        } else if (diff(sample1, sample2 = perlin.noise2((x + 100f) / 1200f, y / 1200f))) {
            highest = Math.max(highest, 1f - Math.abs(sample1 / Math.abs(sample1 - sample2)));
        }

        if (diff(sample1, sample2 = perlin.noise2(x / 1200f, (y + 100f) / 1200f))) {
            highest = Math.max(highest, 1f - Math.abs(sample1 / Math.abs(sample1 - sample2)));
        } else if (diff(sample1, sample2 = perlin.noise2(x / 1200f, (y - 100f) / 1200f))) {
            highest = Math.max(highest, 1f - Math.abs(sample1 / Math.abs(sample1 - sample2)));
        }

        return sample1 > 0f ? 2f - highest : highest;
    }

    private boolean diff(float sample1, float sample2) {
        return sample1 < 0f && sample2 > 0f || sample1 > 0f && sample2 < 0f;
    }

    public BiomeGenBase getBiomeGenAt(int par1, int par2) {
        return getBiomeDataAt(par1, par2).baseBiome;
    }

    public boolean isOceanBiomeAt(int x, int z) {
        return getBiomeDataAt(x, z) instanceof RealisticBiomeOcean;
    }

    public RealisticBiomeBase getBiomeDataAt(int par1, int par2) {
        long coords = ChunkCoordIntPair.chunkXZ2Int(par1, par2);
        RealisticBiomeBase output = biomeDataMap.get(coords);

        if (output != null) {
            return output;
        }

        int metaBiome;
        if (!continental) {
            metaBiome = getClimateAt(par1, par2);
            output = getLandBiomeAt(par1, par2, metaBiome);
        } else {
            float continent = getContinentValue(par1, par2);
            if (continent < 0f) {
                metaBiome = 0;
                output = getOceanBiome(continent, getClimateAt(par1, par2), par1, par2);
            } else if (Support.volcanoIsland instanceof RealisticBiomeIslandVolcano
                    && getVolcanoCoordinates(par1, par2) != Long.MIN_VALUE) {
                        metaBiome = getClimateAt(par1, par2);
                        output = Support.volcanoIsland;
                    } else {
                        int climate = getClimateAt(par1, par2);
                        metaBiome = climate;
                        output = null;
                        boolean islandBiomeSelected = false;
                        boolean littoralBiomeSelected = false;
                        int islandTier = continents.getIslandSizeTier(landmassX(par1), landmassZ(par2));
                        if (islandTier >= 0) {
                            long seedCoordinates = continents
                                    .getIslandSeedCoordinates(landmassX(par1), landmassZ(par2));
                            int seedX = (int) (seedCoordinates >> 32);
                            int seedY = (int) seedCoordinates;
                            int islandClimate = getClimateAt(seedX, seedY);
                            output = selectIslandBiome(islandTier, islandClimate, seedX, seedY);
                            if (output != null) {
                                metaBiome = islandClimate;
                                islandBiomeSelected = true;
                            }
                        }
                        if (output == null) {
                            output = getLandBiomeAt(par1, par2, climate);
                            if (continent < LITTORAL_WIDTH && !littoralBiomes[climate - 1].isEmpty()) {
                                output = selectBiome(
                                        littoralBiomes[climate - 1],
                                        littoralBiomes[climate - 1].size(),
                                        biomeX(par1),
                                        biomeZ(par2));
                                littoralBiomeSelected = true;
                            }
                        }
                        if (!islandBiomeSelected && !littoralBiomeSelected && continent < 24f) {
                            output = output.baseBiome.temperature < 0.15f ? RealisticBiomeBase.coastIce
                                    : RealisticBiomeBase.coastDunes;
                        }
                    }
        }

        if (biomeDataMap.size() > 4096) {
            biomeDataMap.clear();
            metaBiomeDataMap.clear();
        }

        biomeDataMap.put(coords, output);
        metaBiomeDataMap.put(coords, (byte) metaBiome);
        return output;
    }

    /** Samples the terrain beneath a volcano before the volcanic overlay is applied. */
    public float getVolcanoBaseHeight(int x, int y) {
        int landmassX = landmassX(x);
        int landmassZ = landmassZ(y);
        long seedCoordinates = continents.getVolcanoSeedKey(landmassX, landmassZ);
        if (seedCoordinates == Long.MIN_VALUE) return 63f;
        if (volcanoBaseHeightMap.containsKey(seedCoordinates)) return volcanoBaseHeightMap.get(seedCoordinates);
        long centerCoordinates = continents.getVolcanoCenterCoordinates(landmassX, landmassZ);
        int centerX = (int) (centerCoordinates >> 32) - ConfigRWG.landmassOffsetX;
        int centerY = (int) centerCoordinates - ConfigRWG.landmassOffsetZ;
        int climate = getClimateAt(centerX, centerY);
        RealisticBiomeBase biome = continents.isIslandVolcano(landmassX, landmassZ)
                ? selectIslandBiome(1, climate, centerX, centerY)
                : getLandBiomeAt(centerX, centerY, climate);
        if (biome == null) biome = getLandBiomeAt(centerX, centerY, climate);
        float continent = getContinentValue(centerX, centerY);
        float river = getRawRiverStrength(centerX, centerY);
        float height = biome
                .rNoise(perlin, cell, centerX, centerY, getTerrainOceanValue(continent), 1f, river + 1f, continent);
        height = calculateRiver(centerX, centerY, river, height);
        if (volcanoBaseHeightMap.size() > 256) volcanoBaseHeightMap.clear();
        volcanoBaseHeightMap.put(seedCoordinates, height);
        return height;
    }

    /** Samples the large island terrain at this column without applying the volcanic cone. */
    public float getVolcanoUnderlyingHeight(int x, int y) {
        RealisticBiomeBase biome = getVolcanoUnderlyingBiome(x, y);
        if (biome == null) return 63f;
        float continent = getContinentValue(x, y);
        float river = getRawRiverStrength(x, y);
        float height = biome.rNoise(perlin, cell, x, y, getTerrainOceanValue(continent), 1f, river + 1f, continent);
        return calculateRiver(x, y, river, height);
    }

    public RealisticBiomeBase getVolcanoUnderlyingBiome(int x, int y) {
        int shiftedX = landmassX(x);
        int shiftedZ = landmassZ(y);
        long centerCoordinates = continents.getVolcanoCenterCoordinates(shiftedX, shiftedZ);
        if (centerCoordinates == Long.MIN_VALUE) return null;
        int centerX = (int) (centerCoordinates >> 32) - ConfigRWG.landmassOffsetX;
        int centerZ = (int) centerCoordinates - ConfigRWG.landmassOffsetZ;
        int climate = getClimateAt(centerX, centerZ);
        RealisticBiomeBase biome = continents.isIslandVolcano(shiftedX, shiftedZ)
                ? selectIslandBiome(1, climate, centerX, centerZ)
                : getLandBiomeAt(x, y, getClimateAt(x, y));
        if (biome == null) biome = getLandBiomeAt(centerX, centerZ, climate);
        return biome;
    }

    private RealisticBiomeBase selectIslandBiome(int tier, int climate, int seedX, int seedY) {
        ArrayList<RealisticBiomeBase> sized = tier == 0 ? smallIslandBiomes[climate - 1]
                : largeIslandBiomes[climate - 1];
        ArrayList<RealisticBiomeBase> general = islandBiomes[climate - 1];
        return general.isEmpty() && sized.isEmpty() ? null
                : selectCombinedBiome(general, sized, biomeX(seedX), biomeZ(seedY));
    }

    /** @deprecated The ocean value is derived from the continental field. */
    @Deprecated
    public RealisticBiomeBase getBiomeDataAt(int par1, int par2, float ocean) {
        return getBiomeDataAt(par1, par2);
    }

    private TLongObjectHashMap<RealisticBiomeBase> biomeDataMap = new TLongObjectHashMap<RealisticBiomeBase>();
    private TLongByteHashMap metaBiomeDataMap = new TLongByteHashMap();
    private TLongFloatHashMap volcanoBaseHeightMap = new TLongFloatHashMap();
    private TLongByteHashMap volcanoEligibilityMap = new TLongByteHashMap();

    private RealisticBiomeBase getOceanBiome(float continent, int climate, int x, int y) {
        if (continent < -SHALLOW_OCEAN_WIDTH) {
            return new RealisticBiomeBase[] { null, Support.oceanDeepSnow, Support.oceanDeepCold, Support.oceanDeepHot,
                    Support.oceanDeepWet }[climate];
        }
        int biomeX = biomeX(x);
        int biomeZ = biomeZ(y);
        float patch = perlin.noise2(biomeX / 180f, biomeZ / 180f) * .7f
                + perlin.noise2(biomeX / 55f, biomeZ / 55f) * .3f;
        if ((climate == 1 || climate == 2) && continent < -90f && patch > 0f) return Support.oceanShallowKelp;
        if ((climate == 3 || climate == 4) && continent < -20f && continent > -150f && patch > .07f) {
            return Support.oceanShallowCoral;
        }
        return new RealisticBiomeBase[] { null, Support.oceanShallowSnow, Support.oceanShallowCold,
                Support.oceanShallowHot, Support.oceanShallowWet }[climate];
    }

    private int getClimateAt(int x, int y) {
        return getClimateFromValue(getClimateValue(x, y));
    }

    private int getClimateFromValue(float climate) {
        if (wetEnabled) return climate < SNOW_CLIMATE_LIMIT ? 1
                : climate < COLD_CLIMATE_LIMIT ? 2 : climate < HOT_CLIMATE_LIMIT ? 3 : 4;
        return climate < THREE_CLIMATE_SNOW_LIMIT ? 1 : climate < THREE_CLIMATE_COLD_LIMIT ? 2 : 3;
    }

    private float getClimateValue(int x, int y) {
        double[] warped = climateWarpSample.get();
        warpClimateCoordinates(biomeX(x), biomeZ(y), warped);
        return (biomecell.noise((warped[0] + 4000D) / climateWidth, warped[1] / climateWidth, 1D) * .5f) + .5f;
    }

    private void warpClimateCoordinates(int x, int y, double[] output) {
        float scale = climateWidth * CLIMATE_WARP_SCALE_MULTIPLIER;
        float strength = climateWidth * CLIMATE_WARP_STRENGTH_MULTIPLIER;
        output[0] = x + climateWarp.noise2(x / scale, y / scale) * strength;
        output[1] = y + climateWarp.noise2((x + 1731f) / scale, (y - 2459f) / scale) * strength;
    }

    private RealisticBiomeBase selectBiome(List<RealisticBiomeBase> biomes, int length, int x, int y) {
        float value = sampleBiomeSelector(x, y);
        value = value < 0f ? 0f : value >= .9999999f ? .9999999f : value;
        return biomes.get((int) (value * length));
    }

    private float sampleBiomeSelector(int x, int y) {
        float scale = biomeWidth * BIOME_WARP_SCALE_MULTIPLIER;
        float strength = biomeWidth * BIOME_WARP_STRENGTH_MULTIPLIER;
        float warpedX = x + climateWarp.noise2((x - 8191f) / scale, (y + 3137f) / scale) * strength;
        float warpedY = y + climateWarp.noise2((x + 5171f) / scale, (y - 6971f) / scale) * strength;
        return (biomecell.noise(warpedX / biomeWidth, warpedY / biomeWidth, 1D) * .5f) + .5f;
    }

    private RealisticBiomeBase getLandBiomeAt(int par1, int par2) {
        return getLandBiomeAt(par1, par2, getClimateAt(par1, par2));
    }

    /** Returns the core biome selected at this coordinate without border, island, or small-biome replacement. */
    public RealisticBiomeBase getCoreBiomeAt(int x, int z) {
        int climate = getClimateAt(x, z);
        int sampleX = biomeX(x);
        int sampleZ = biomeZ(z);
        switch (climate) {
            case 1:
                return selectBiome(biomes_snow, biomes_snowLength, sampleX, sampleZ);
            case 2:
                return selectBiome(biomes_cold, biomes_coldLength, sampleX, sampleZ);
            case 4:
                return selectBiome(biomes_wet, biomes_wetLength, sampleX, sampleZ);
            default:
                return selectBiome(biomes_hot, biomes_hotLength, sampleX, sampleZ);
        }
    }

    private RealisticBiomeBase getLandBiomeAt(int par1, int par2, int climate) {
        par1 = biomeX(par1);
        par2 = biomeZ(par2);
        int climateIndex = climate - 1;
        ArrayList<RealisticBiomeBase> small = smallBiomes[climateIndex];
        if (!small.isEmpty()) {
            double[] point = smallPointSample.get();
            smallBiomePoints.sample(par1, par2, point);
            if (point[0] < SMALL_BIOME_RADIUS) {
                return selectBiome(small, small.size(), (int) point[3], (int) point[4]);
            }
        }
        double[] climatePoints = climatePointSample.get();
        double[] warped = climateWarpSample.get();
        warpClimateCoordinates(par1, par2, warped);
        biomecell.sampleTwo2D((warped[0] + 4000D) / climateWidth, warped[1] / climateWidth, 1D, climatePoints);
        int neighborClimate = getClimateFromValue((float) (climatePoints[3] * .5D + .5D));
        boolean climateBorder = neighborClimate != climate
                && (climatePoints[2] - climatePoints[0]) * climateWidth < CLIMATE_BORDER_DISTANCE_DIFFERENCE;
        if (climateBorder) {
            int borderDirection = neighborClimate < climate ? -1 : 1;
            boolean extreme = Math.abs(neighborClimate - climate) > 1;
            if (extreme) {
                ArrayList<RealisticBiomeBase> extremeDirectional = borderDirection < 0
                        ? veryColdBorderBiomes[climateIndex]
                        : veryHotBorderBiomes[climateIndex];
                if (!extremeDirectional.isEmpty()) {
                    return selectBiome(extremeDirectional, extremeDirectional.size(), par1, par2);
                }
            }
            ArrayList<RealisticBiomeBase> directional = borderDirection < 0 ? coldBorderBiomes[climateIndex]
                    : hotBorderBiomes[climateIndex];
            ArrayList<RealisticBiomeBase> shared = borderBiomes[climateIndex];
            if (!shared.isEmpty() || !directional.isEmpty()) {
                return selectCombinedBiome(shared, directional, par1, par2);
            }
        }
        switch (climate) {
            case 1:
                return selectBiome(biomes_snow, biomes_snowLength, par1, par2);
            case 2:
                return selectBiome(biomes_cold, biomes_coldLength, par1, par2);
            case 4:
                return selectBiome(biomes_wet, biomes_wetLength, par1, par2);
            default:
                return selectBiome(biomes_hot, biomes_hotLength, par1, par2);
        }

        /*
         * if(par1 + par2 < 0) { return RealisticBiomeBase.landTaigaFields; } else { return
         * RealisticBiomeBase.landTaigaHills; }
         */

        /*
         * float h = (biomecell.noise(par1 / 450D, par2 / 450D, 1D) * 0.5f) + 0.5f; h = h < 0f ? 0f : h >= 0.9999999f ?
         * 0.9999999f : h; float temp = 0.5f + (perlin.noise2((par1 + 2000f) / 2000f, par2 / 2000f) * 1.1f); float hum =
         * 0.5f + (perlin.noise2((par1 - 2000f) / 2000f, par2 / 2000f) * 1.1f); temp = temp > 1f ? 1f : temp < 0f ? 0f :
         * temp; hum = hum > 1f ? 1f : hum < 0f ? 0f : hum; if((1f - temp) + hum > 1f) { hum -= temp; temp += hum; }
         * if(temp < 0.15f) { h *= 2f; return biomes_polar[(int)(h)]; } else if(hum < 0.2f) { h *= 8f; return
         * biomes_tundra[(int)(h)]; } else if(temp < 0.5f) { h *= 5f; return biomes_snow[(int)(h)]; } else if(temp >
         * 0.85f && hum > 0.85f) { return RealisticBiomeBase.landRedwoodSpikes; } else { h *= 9f; return
         * biomes_taiga[(int)(h)]; }
         */

        // int x = (int)(temp * 7f);
        // int y = (int)(hum * 7f);

        // x = x < 0 ? 0 : x > 6 ? 6 : x;
        // y = y < 0 ? 0 : y > 6 ? 6 : y;

        /*
         * if(par1 % 100 == 0 && par2 % 100 == 0) { System.out.println(par1 + " " + par2 + " " + x + " " + y + " - " +
         * temp + " " + hum); }
         */

        // return biomes[x * 7 + y];

        /*
         * ocean = ocean > 1f ? 1f : ocean < 0f ? 0f : ocean; if(ocean < 0.45f) { return biomeLayerOcean.getBiome(temp,
         * hum); } else if (ocean > 0.55f) { return biomeLayerLand.getBiome(temp, hum); } else { return
         * biomeLayerCoast.getBiome(temp, hum); }
         */
    }

    private RealisticBiomeBase selectCombinedBiome(List<RealisticBiomeBase> shared,
            List<RealisticBiomeBase> directional, int x, int y) {
        int length = shared.size() + directional.size();
        float value = sampleBiomeSelector(x, y);
        int index = (int) (Math.max(0f, Math.min(.9999999f, value)) * length);
        return index < shared.size() ? shared.get(index) : directional.get(index - shared.size());
    }

    @SafeVarargs
    private static ArrayList<RealisticBiomeBase>[] lists(ArrayList<RealisticBiomeBase>... lists) {
        return lists;
    }

    public float getNoiseAt(int x, int y) {
        float river = getRiverStrength(x, y) + 1f;
        if (river < 0.5f) {
            return 59f;
        }

        float ocean = getTerrainOceanValue(x, y);
        return getBiomeDataAt(x, y).rNoise(perlin, cell, x, y, ocean, 1f, river);
    }

    public float getNoiseWithRiverOceanAt(int x, int y, float river, float ocean) {
        return getBiomeDataAt(x, y).rNoise(perlin, cell, x, y, ocean, 1f, river);
    }

    public float calculateRiver(int x, int y, float st, float biomeHeight) {

        if (st < 0f && biomeHeight > 59f) {
            float pX = x + (perlin.noise1(y / 240f) * 220f);
            float pY = y + (perlin.noise1(x / 240f) * 220f);
            float r = cell.border(pX / 1250D, pY / 1250D, 50D / 1300D, 1f);
            return (biomeHeight * (r + 1f))
                    + ((59f + perlin.noise2(x / 12f, y / 12f) * 2f + perlin.noise2(x / 8f, y / 8f) * 1.5f) * (-r));
        } else {
            return biomeHeight;
        }
    }

    public float calculateRiver(int x, int y, float st, float biomeHeight, float[] sample) {
        if (st >= 0f || biomeHeight <= 59f) return biomeHeight;
        float riverBorder = sample[2];
        if (Float.isNaN(riverBorder)) {
            riverBorder = cell.border(sample[0] / 1250D, sample[1] / 1250D, 50D / 1300D, 1f);
            sample[2] = riverBorder;
            sample[3] = 59f + perlin.noise2(x / 12f, y / 12f) * 2f + perlin.noise2(x / 8f, y / 8f) * 1.5f;
        }
        return (biomeHeight * (riverBorder + 1f)) + (sample[3] * (-riverBorder));
    }

    public float getRiverStrength(int x, int y) {
        float pX = x + (perlin.noise1(y / 240f) * 220f);
        float pY = y + (perlin.noise1(x / 240f) * 220f);
        return getRiverStrength(x, y, pX, pY);
    }

    public float getRiverStrength(int x, int y, float[] sample) {
        float pX = x + (perlin.noise1(y / 240f) * 220f);
        float pY = y + (perlin.noise1(x / 240f) * 220f);
        sample[0] = pX;
        sample[1] = pY;
        sample[2] = Float.NaN;
        return getRiverStrength(x, y, pX, pY);
    }

    public float getRiverTunnelStrength(int x, int y) {
        float warpedX = x + perlin.noise1(y / 240f) * 220f;
        float warpedY = y + perlin.noise1(x / 240f) * 220f;
        return -cell.border(warpedX / 1250D, warpedY / 1250D, 9D / 1250D, 1f);
    }

    public float getRiverJunctionStrength(int x, int y) {
        float warpedX = x + perlin.noise1(y / 240f) * 220f;
        float warpedY = y + perlin.noise1(x / 240f) * 220f;
        return -cell.junction(warpedX / 1250D, warpedY / 1250D, 60D / 1250D, 1f);
    }

    private float getRiverStrength(int x, int y, float pX, float pY) {
        float strength = getRawRiverStrength(pX, pY);
        if (!continental) {
            return strength;
        }

        long coordinates = continents.getVolcanoVicinityCoordinates(landmassX(x), landmassZ(y));
        if (coordinates == Long.MIN_VALUE) {
            return strength;
        }
        float localX = ContinentalNoise.unpackVolcanoX(coordinates);
        float localY = ContinentalNoise.unpackVolcanoY(coordinates);
        float distance = (float) Math.sqrt(localX * localX + localY * localY);
        if (distance <= ContinentalNoise.VOLCANO_RADIUS) {
            return Math.max(0f, strength);
        }

        float blend = (float) ((distance - ContinentalNoise.VOLCANO_RADIUS)
                / (ContinentalNoise.VOLCANO_ISLAND_RADIUS - ContinentalNoise.VOLCANO_RADIUS));
        return strength < 0f ? strength * blend : strength;
    }

    private float getRawRiverStrength(int x, int y) {
        float pX = x + (perlin.noise1(y / 240f) * 220f);
        float pY = y + (perlin.noise1(x / 240f) * 220f);
        return getRawRiverStrength(pX, pY);
    }

    private float getRawRiverStrength(float pX, float pY) {
        return cell.border(pX / 1250D, pY / 1250D, 50D / 300D, 1f);
    }

    public boolean isBorderlessAt(int x, int y) {
        int bx, by;

        for (bx = -2; bx <= 2; bx++) {
            for (by = -2; by <= 2; by++) {
                borderNoise[getBiomeDataAt(x + bx * 16, y + by * 16).biomeID] += 0.04f;
            }
        }

        by = 0;
        for (bx = 0; bx < 256; bx++) {
            if (borderNoise[bx] > 0.98f) {
                by = 1;
            }
            borderNoise[bx] = 0;
        }

        return by == 1 ? true : false;
    }

    public List getBiomesToSpawnIn() {
        return this.biomesToSpawnIn;
    }

    private static int landmassX(int x) {
        return x + ConfigRWG.landmassOffsetX;
    }

    private static int landmassZ(int z) {
        return z + ConfigRWG.landmassOffsetZ;
    }

    private static int biomeX(int x) {
        return x + ConfigRWG.biomeOffsetX;
    }

    private static int biomeZ(int z) {
        return z + ConfigRWG.biomeOffsetZ;
    }

    public float[] getRainfall(float[] par1ArrayOfFloat, int par2, int par3, int par4, int par5) {
        if (par1ArrayOfFloat == null || par1ArrayOfFloat.length < par4 * par5) {
            par1ArrayOfFloat = new float[par4 * par5];
        }

        int var6[] = getBiomesGens(par2, par3, par4, par5);

        for (int var7 = 0; var7 < par4 * par5; ++var7) {
            float var8 = (float) BiomeGenBase.getBiome(var6[var7]).getIntRainfall() / 65536.0F;

            if (var8 > 1.0F) {
                var8 = 1.0F;
            }

            par1ArrayOfFloat[var7] = var8;
        }

        return par1ArrayOfFloat;
    }

    public float getTemperatureAtHeight(float par1, int par2) {
        return par1;
    }

    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] par1ArrayOfBiomeGenBase, int par2, int par3, int par4,
            int par5) {
        if (par1ArrayOfBiomeGenBase == null || par1ArrayOfBiomeGenBase.length < par4 * par5) {
            par1ArrayOfBiomeGenBase = new BiomeGenBase[par4 * par5];
        }

        int var7[] = getBiomesGens(par2, par3, par4, par5);

        for (int var8 = 0; var8 < par4 * par5; ++var8) {
            par1ArrayOfBiomeGenBase[var8] = BiomeGenBase.getBiome(var7[var8]);
        }

        return par1ArrayOfBiomeGenBase;
    }

    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] par1ArrayOfBiomeGenBase, int par2, int par3, int par4,
            int par5) {
        return this.getBiomeGenAt(par1ArrayOfBiomeGenBase, par2, par3, par4, par5, true);
    }

    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] par1ArrayOfBiomeGenBase, int par2, int par3, int par4, int par5,
            boolean par6) {
        if (par1ArrayOfBiomeGenBase == null || par1ArrayOfBiomeGenBase.length < par4 * par5) {
            par1ArrayOfBiomeGenBase = new BiomeGenBase[par4 * par5];
        }

        int var7[] = getBiomesGens(par2, par3, par4, par5);

        for (int var8 = 0; var8 < par4 * par5; ++var8) {
            par1ArrayOfBiomeGenBase[var8] = BiomeGenBase.getBiome(var7[var8]);
        }

        return par1ArrayOfBiomeGenBase;
    }

    public boolean areBiomesViable(int x, int y, int par3, List par4List) {
        float centerNoise = getNoiseAt(x, y);
        if (centerNoise < 62) {
            return false;
        }

        float lowestNoise = centerNoise;
        float highestNoise = centerNoise;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 && j != 0) {
                    float n = getNoiseAt(x + i * 16, y + j * 16);
                    if (n < lowestNoise) {
                        lowestNoise = n;
                    }
                    if (n > highestNoise) {
                        highestNoise = n;
                    }
                }
            }
        }

        if (highestNoise - lowestNoise < 22) {
            return true;
        }

        return false;
    }

    public ChunkPosition findBiomePosition(int p_150795_1_, int p_150795_2_, int p_150795_3_, List p_150795_4_,
            Random p_150795_5_) {
        return null;
    }

    public void cleanupCache() {
        this.biomeCache.cleanupCache();
    }
}
