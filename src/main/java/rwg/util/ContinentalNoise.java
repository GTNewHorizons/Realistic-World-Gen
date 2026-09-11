package rwg.util;

import rwg.config.ConfigRWG;

/**
 * A continent distance field built from priority-sampled Voronoi cells. Land occupies the area around a cell's feature
 * point and ocean occupies its edge. The lookup coordinates are strongly domain-warped to keep the cell boundaries from
 * looking geometric.
 */
public class ContinentalNoise {

    /** Outer radius used for volcano blending and exclusion zones. */
    public static final double VOLCANO_ISLAND_RADIUS = 160D;
    public static final double VOLCANO_RADIUS = 130D;
    private static final int POISSON_ROUNDS = 8;
    private static final double WARP_SCALE = 3600D;
    private static final double WARP_STRENGTH = 1800D;
    // Calibrated against preview-sized samples of the eight-round point field. This converts the second moment of
    // continent radii into approximate land coverage before overlapping continents begin to merge.
    private static final double LAND_COVERAGE_DENSITY = 1.82D;
    // Once radii cross the original ocean gap, neighboring continent discs overlap. Preview sweeps provide this small
    // nonlinear correction to the otherwise geometric estimate between roughly 50% and 30% ocean.
    private static final double OVERLAP_CORRECTION_BASE = .75D;
    private static final double OVERLAP_CORRECTION_SLOPE = .46D;
    private static final long CONTINENT_VOLCANO_SEED_SALT = 0x082EFA98EC4E6C89L;

    private final long seed;
    private final PoissonPointNoise points;
    private final IslandPointNoise islands;
    private final ContinentVolcanoNoise continentVolcanoes;
    private final double minimumContinentWidth;
    private final double continentWidthRange;
    private final double maximumIslandWidth;
    private final double minimumIslandWidth;
    private final double islandWidthRange;
    private final double voronoiRadius;
    private final double continentDilation;
    private final NoiseGenerator warpX;
    private final NoiseGenerator warpY;
    private final double warpScale;
    private final double warpStrength;
    private final double offsetX;
    private final double offsetY;
    private final ThreadLocal<double[]> samples = new ThreadLocal<double[]>() {

        @Override
        protected double[] initialValue() {
            return new double[5];
        }
    };
    private final ThreadLocal<LandformSample> landformSamples = new ThreadLocal<LandformSample>() {

        @Override
        protected LandformSample initialValue() {
            return new LandformSample();
        }
    };
    private final ThreadLocal<double[]> islandCenterSamples = new ThreadLocal<double[]>() {

        @Override
        protected double[] initialValue() {
            return new double[] { Double.NaN, Double.NaN, 0D, 0D };
        }
    };
    private final ThreadLocal<double[]> continentVolcanoSamples = new ThreadLocal<double[]>() {

        @Override
        protected double[] initialValue() {
            return new double[6];
        }
    };
    private final ThreadLocal<double[]> volcanoCenterSamples = new ThreadLocal<double[]>() {

        @Override
        protected double[] initialValue() {
            return new double[] { Double.NaN, Double.NaN, 0D, 0D };
        }
    };

    public ContinentalNoise(long seed) {
        this.seed = seed;
        minimumContinentWidth = Math.min(ConfigRWG.minimumContinentWidth, ConfigRWG.maximumContinentWidth);
        double maximumContinentWidth = Math.max(ConfigRWG.minimumContinentWidth, ConfigRWG.maximumContinentWidth);
        minimumIslandWidth = Math.min(ConfigRWG.minimumIslandWidth, ConfigRWG.maximumIslandWidth);
        maximumIslandWidth = Math.max(ConfigRWG.minimumIslandWidth, ConfigRWG.maximumIslandWidth);
        islandWidthRange = maximumIslandWidth - minimumIslandWidth;
        continentWidthRange = maximumContinentWidth - minimumContinentWidth;
        double averageContinentWidth = (minimumContinentWidth + maximumContinentWidth) * 0.5D;
        voronoiRadius = averageContinentWidth + ConfigRWG.averageOceanWidth;
        continentDilation = continentDilation(
                minimumContinentWidth,
                maximumContinentWidth,
                voronoiRadius,
                ConfigRWG.minimumOceanWidth,
                ConfigRWG.maximumOceanFraction);
        warpScale = WARP_SCALE;
        warpStrength = WARP_STRENGTH;
        warpX = new PerlinNoise(seed ^ 0x243F6A8885A308D3L);
        warpY = new PerlinNoise(seed ^ 0x13198A2E03707344L);
        // Prevent the largest two continents from overlapping; later rounds fill the remaining holes.
        points = new PoissonPointNoise(seed, maximumContinentWidth * 2D + ConfigRWG.minimumOceanWidth, POISSON_ROUNDS);
        islands = new IslandPointNoise(
                seed ^ 0xA4093822299F31D0L,
                points,
                maximumContinentWidth,
                ConfigRWG.minimumIslandWidth,
                ConfigRWG.maximumIslandWidth,
                ConfigRWG.minimumOceanWidth,
                ConfigRWG.islandPlacementChance);
        continentVolcanoes = new ContinentVolcanoNoise(
                seed,
                seed ^ CONTINENT_VOLCANO_SEED_SALT,
                points,
                minimumContinentWidth,
                maximumContinentWidth,
                voronoiRadius,
                ConfigRWG.averageContinentVolcanoCount,
                VOLCANO_ISLAND_RADIUS);

        double[] origin = new double[5];
        points.sample(0D, 0D, origin);
        offsetX = origin[3];
        offsetY = origin[4];
    }

    /** Returns distance from the shore in blocks: positive on land and negative at sea. */
    public float getValue(int x, int y) {
        return sampleLandform(x, y).value;
    }

    /** True when this land coordinate belongs to the smaller island field rather than a continent. */
    public boolean isIsland(int x, int y) {
        return sampleLandform(x, y).island;
    }

    /** Returns 0/1 for a small/large island, or -1 when the coordinate is not on an island. */
    public int getIslandSizeTier(int x, int y) {
        LandformSample sample = sampleLandform(x, y);
        if (!sample.island) return -1;
        double fraction = islandWidthRange == 0D ? 0D : (sample.islandWidth - minimumIslandWidth) / islandWidthRange;
        return fraction < .5D ? 0 : 1;
    }

    /** Returns the winning island seed coordinates, packed as two signed integers. */
    public long getIslandSeedCoordinates(int x, int y) {
        LandformSample sample = sampleLandform(x, y);
        if (!sample.island) return Long.MIN_VALUE;
        return (long) sample.islandSeedX << 32 | sample.islandSeedZ & 0xffffffffL;
    }

    /** Returns the approximate physical centre of the winning island after inverting the coastline warp. */
    public long getIslandCenterCoordinates(int x, int y) {
        LandformSample sample = sampleLandform(x, y);
        if (!sample.island) return Long.MIN_VALUE;
        double[] center = getIslandCenter(sample.islandSeedX, sample.islandSeedZ);
        return (long) Math.round(center[2]) << 32 | Math.round(center[3]) & 0xffffffffL;
    }

    private double[] getIslandCenter(int seedX, int seedZ) {
        double[] center = islandCenterSamples.get();
        if (center[0] == seedX && center[1] == seedZ) return center;
        double centerX = seedX - offsetX;
        double centerZ = seedZ - offsetY;
        for (int iteration = 0; iteration < 12; iteration++) {
            double noiseX = centerX / warpScale;
            double noiseZ = centerZ / warpScale;
            double warpedX = centerX + warpX.noise2((float) noiseX, (float) noiseZ) * warpStrength
                    + warpX.noise2((float) (noiseX * 2D), (float) (noiseZ * 2D)) * warpStrength
                    + offsetX;
            double warpedZ = centerZ + warpY.noise2((float) noiseX, (float) noiseZ) * warpStrength
                    + warpY.noise2((float) (noiseX * 2D), (float) (noiseZ * 2D)) * warpStrength
                    + offsetY;
            centerX -= (warpedX - seedX) * .5D;
            centerZ -= (warpedZ - seedZ) * .5D;
        }
        center[0] = seedX;
        center[1] = seedZ;
        center[2] = centerX;
        center[3] = centerZ;
        return center;
    }

    private LandformSample sampleLandform(int x, int y) {
        LandformSample result = landformSamples.get();
        if (result.valid && result.x == x && result.y == y) return result;

        double[] sample = samples.get();
        double noiseX = x / warpScale;
        double noiseY = y / warpScale;
        double warpedX = x + warpX.noise2((float) noiseX, (float) noiseY) * warpStrength
                + warpX.noise2((float) (noiseX * 2D), (float) (noiseY * 2D)) * warpStrength;
        double warpedY = y + warpY.noise2((float) noiseX, (float) noiseY) * warpStrength
                + warpY.noise2((float) (noiseX * 2D), (float) (noiseY * 2D)) * warpStrength;
        points.sample(warpedX + offsetX, warpedY + offsetY, sample);
        double continent = continentField(sample[0], (int) sample[1], (int) sample[2]);

        result.x = x;
        result.y = y;
        result.valid = true;
        result.island = false;
        result.islandWidth = 0D;
        result.islandSeedX = 0;
        result.islandSeedZ = 0;
        result.islandLocalX = 0f;
        result.islandLocalZ = 0f;
        result.volcano = false;
        result.volcanoIsland = false;
        result.volcanoKey = Long.MIN_VALUE;
        result.volcanoCenterX = 0D;
        result.volcanoCenterZ = 0D;
        if (continent >= 0D && ConfigRWG.averageContinentVolcanoCount > 0f) {
            double[] volcano = continentVolcanoSamples.get();
            continentVolcanoes.sample(warpedX + offsetX, warpedY + offsetY, volcano);
            if (Double.isFinite(volcano[0])) {
                double[] center = getVolcanoCenter(volcano[1], volcano[2]);
                double localX = x - center[2];
                double localZ = y - center[3];
                if (localX * localX + localZ * localZ <= VOLCANO_ISLAND_RADIUS * VOLCANO_ISLAND_RADIUS) {
                    result.volcano = true;
                    result.volcanoKey = ContinentVolcanoNoise.candidateKey(
                            seed ^ CONTINENT_VOLCANO_SEED_SALT,
                            (int) volcano[3],
                            (int) volcano[4],
                            (int) volcano[5]);
                    result.volcanoCenterX = center[2];
                    result.volcanoCenterZ = center[3];
                    result.islandLocalX = (float) localX;
                    result.islandLocalZ = (float) localZ;
                }
            }
        }
        if (continent >= maximumIslandWidth) {
            result.value = (float) continent;
            return result;
        }

        islands.sample(warpedX + offsetX, warpedY + offsetY, sample);
        double island = sample[0];
        result.value = (float) Math.max(continent, island);
        result.island = island >= 0D && island > continent;
        if (result.island) {
            result.islandWidth = sample[1];
            result.islandSeedX = (int) Math.round(sample[2]);
            result.islandSeedZ = (int) Math.round(sample[3]);
            double[] center = getIslandCenter(result.islandSeedX, result.islandSeedZ);
            result.islandLocalX = (float) (x - center[2]);
            result.islandLocalZ = (float) (y - center[3]);
            result.volcano = getIslandSizeTier(result.islandWidth) == 1
                    && random01(result.islandSeedX, result.islandSeedZ, 17) < ConfigRWG.largeIslandVolcanoChance;
            if (result.volcano) {
                result.volcanoIsland = true;
                result.volcanoKey = (long) result.islandSeedX << 32 | result.islandSeedZ & 0xffffffffL;
                result.volcanoCenterX = center[2];
                result.volcanoCenterZ = center[3];
            }
        }
        return result;
    }

    private double continentField(double distance, int cellX, int cellY) {
        double width = minimumContinentWidth + random01(cellX, cellY, 0) * continentWidthRange;
        return Math.min(voronoiRadius, width) + continentDilation - distance;
    }

    private static double continentDilation(double minimumWidth, double maximumWidth, double radiusLimit,
            double minimumOceanWidth, double oceanLimit) {
        if (oceanLimit >= 1D) return 0D;

        double lowerRadius = Math.min(radiusLimit, minimumWidth);
        double upperRadius = Math.min(radiusLimit, maximumWidth);
        double meanRadius = (lowerRadius + upperRadius) * .5D;
        double meanSquaredRadius = (lowerRadius * lowerRadius + lowerRadius * upperRadius + upperRadius * upperRadius)
                / 3D;
        double pointSpacing = maximumWidth * 2D + minimumOceanWidth;
        double requestedLandFraction = 1D - oceanLimit;
        double overlapCorrection = OVERLAP_CORRECTION_BASE + OVERLAP_CORRECTION_SLOPE * requestedLandFraction;
        double desiredLandFraction = Math.min(1D, requestedLandFraction * overlapCorrection);
        double desiredMeanSquaredRadius = desiredLandFraction * pointSpacing * pointSpacing / LAND_COVERAGE_DENSITY;
        if (desiredMeanSquaredRadius <= meanSquaredRadius) return 0D;
        return Math.sqrt(meanRadius * meanRadius + desiredMeanSquaredRadius - meanSquaredRadius) - meanRadius;
    }

    private int getIslandSizeTier(double width) {
        double fraction = islandWidthRange == 0D ? 0D : (width - minimumIslandWidth) / islandWidthRange;
        return fraction < .5D ? 0 : 1;
    }

    public long getVolcanoCoordinates(int x, int y) {
        return getVolcanoCoordinates(x, y, VOLCANO_RADIUS);
    }

    public long getVolcanoVicinityCoordinates(int x, int y) {
        return getVolcanoCoordinates(x, y, VOLCANO_ISLAND_RADIUS);
    }

    private long getVolcanoCoordinates(int x, int y, double radius) {
        LandformSample sample = sampleLandform(x, y);
        if (!sample.volcano || sample.islandLocalX * sample.islandLocalX + sample.islandLocalZ * sample.islandLocalZ
                > radius * radius)
            return Long.MIN_VALUE;
        return (long) Float.floatToRawIntBits(sample.islandLocalX) << 32
                | Float.floatToRawIntBits(sample.islandLocalZ) & 0xffffffffL;
    }

    public static float unpackVolcanoX(long coordinates) {
        return Float.intBitsToFloat((int) (coordinates >>> 32));
    }

    public static float unpackVolcanoY(long coordinates) {
        return Float.intBitsToFloat((int) coordinates);
    }

    public long getVolcanoSeedKey(int x, int y) {
        return sampleLandform(x, y).volcanoKey;
    }

    public boolean isIslandVolcano(int x, int y) {
        LandformSample sample = sampleLandform(x, y);
        return sample.volcano && sample.volcanoIsland;
    }

    public long getVolcanoCenterCoordinates(int x, int y) {
        LandformSample sample = sampleLandform(x, y);
        if (!sample.volcano) return Long.MIN_VALUE;
        return (long) Math.round(sample.volcanoCenterX) << 32 | Math.round(sample.volcanoCenterZ) & 0xffffffffL;
    }

    private double[] getVolcanoCenter(double seedX, double seedZ) {
        double[] center = volcanoCenterSamples.get();
        if (center[0] == seedX && center[1] == seedZ) return center;
        double centerX = seedX - offsetX;
        double centerZ = seedZ - offsetY;
        for (int iteration = 0; iteration < 12; iteration++) {
            double noiseX = centerX / warpScale;
            double noiseZ = centerZ / warpScale;
            double warpedX = centerX + warpX.noise2((float) noiseX, (float) noiseZ) * warpStrength
                    + warpX.noise2((float) (noiseX * 2D), (float) (noiseZ * 2D)) * warpStrength
                    + offsetX;
            double warpedZ = centerZ + warpY.noise2((float) noiseX, (float) noiseZ) * warpStrength
                    + warpY.noise2((float) (noiseX * 2D), (float) (noiseZ * 2D)) * warpStrength
                    + offsetY;
            centerX -= (warpedX - seedX) * .5D;
            centerZ -= (warpedZ - seedZ) * .5D;
        }
        center[0] = seedX;
        center[1] = seedZ;
        center[2] = centerX;
        center[3] = centerZ;
        return center;
    }

    private double random01(int cellX, int cellY, int salt) {
        long value = seed;
        value ^= (long) cellX * 341873128712L;
        value ^= (long) cellY * 132897987541L;
        value ^= (long) salt * 42317861L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static final class LandformSample {

        private int x;
        private int y;
        private boolean valid;
        private float value;
        private boolean island;
        private double islandWidth;
        private int islandSeedX;
        private int islandSeedZ;
        private float islandLocalX;
        private float islandLocalZ;
        private boolean volcano;
        private boolean volcanoIsland;
        private long volcanoKey;
        private double volcanoCenterX;
        private double volcanoCenterZ;
    }
}
