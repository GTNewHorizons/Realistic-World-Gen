package rwg.util;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;

/** Selects deterministic landmark centres from raw darts of the continent Poisson field which land on continents. */
final class ContinentLandmarkNoise {

    private static final int CACHE_LIMIT = 4096;

    private final long continentSeed;
    private final long selectionSeed;
    private final PoissonPointNoise continents;
    private final double minimumContinentWidth;
    private final double continentWidthRange;
    private final double voronoiRadius;
    private final double selectionChance;
    private final int searchCells;
    private final TLongObjectHashMap<Candidate[]> regions = new TLongObjectHashMap<Candidate[]>();
    private final double[] point = new double[2];
    private final double[] nearestContinent = new double[5];

    ContinentLandmarkNoise(long continentSeed, long selectionSeed, PoissonPointNoise continents,
            double minimumContinentWidth, double maximumContinentWidth, double voronoiRadius,
            double averagePerContinent, double searchRadius) {
        this.continentSeed = continentSeed;
        this.selectionSeed = selectionSeed;
        this.continents = continents;
        this.minimumContinentWidth = minimumContinentWidth;
        continentWidthRange = maximumContinentWidth - minimumContinentWidth;
        this.voronoiRadius = voronoiRadius;
        double averageWidth = (minimumContinentWidth + maximumContinentWidth) * 0.5D;
        double landFraction = Math
                .max(0.01D, Math.min(1D, averageWidth * averageWidth / (voronoiRadius * voronoiRadius)));
        selectionChance = Math.min(1D, averagePerContinent / (continents.getRounds() * landFraction));
        searchCells = (int) Math.ceil(searchRadius / continents.getCellSize()) + 1;
    }

    /** Output: warped distance, point X/Z, candidate cell X/Z, and dart round. */
    synchronized void sample(double x, double z, double[] output) {
        int cellX = floor(x / continents.getCellSize());
        int cellZ = floor(z / continents.getCellSize());
        Candidate best = null;
        double bestSquared = Double.POSITIVE_INFINITY;
        for (Candidate candidate : region(cellX, cellZ)) {
            double dx = x - candidate.x;
            double dz = z - candidate.z;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared < bestSquared) {
                best = candidate;
                bestSquared = distanceSquared;
            }
        }
        output[0] = Math.sqrt(bestSquared);
        output[1] = best == null ? 0D : best.x;
        output[2] = best == null ? 0D : best.z;
        output[3] = best == null ? 0D : best.cellX;
        output[4] = best == null ? 0D : best.cellZ;
        output[5] = best == null ? 0D : best.round;
    }

    private Candidate[] region(int cellX, int cellZ) {
        long key = cellKey(cellX, cellZ);
        Candidate[] cached = regions.get(key);
        if (cached != null) return cached;
        if (regions.size() >= CACHE_LIMIT) regions.clear();
        List<Candidate> selected = new ArrayList<Candidate>();
        for (int offsetZ = -searchCells; offsetZ <= searchCells; offsetZ++) {
            for (int offsetX = -searchCells; offsetX <= searchCells; offsetX++) {
                int candidateCellX = cellX + offsetX;
                int candidateCellZ = cellZ + offsetZ;
                for (int round = 0; round < continents.getRounds(); round++) {
                    if (unit(hash(selectionSeed, candidateCellX, candidateCellZ, round * 2 + 1)) >= selectionChance)
                        continue;
                    continents.getCandidate(candidateCellX, candidateCellZ, round, point);
                    continents.sample(point[0], point[1], nearestContinent);
                    double width = minimumContinentWidth
                            + unit(hash(continentSeed, (int) nearestContinent[1], (int) nearestContinent[2], 0))
                                    * continentWidthRange;
                    if (Math.min(voronoiRadius, width) - nearestContinent[0] < 0D) continue;
                    selected.add(new Candidate(point[0], point[1], candidateCellX, candidateCellZ, round));
                }
            }
        }
        Candidate[] result = selected.toArray(new Candidate[selected.size()]);
        regions.put(key, result);
        return result;
    }

    static long candidateKey(long seed, int cellX, int cellZ, int round) {
        long value = hash(seed, cellX, cellZ, round * 2 + 2);
        return value == Long.MIN_VALUE ? Long.MIN_VALUE + 1 : value;
    }

    private static long hash(long seed, int cellX, int cellZ, int salt) {
        long value = seed;
        value ^= (long) cellX * 341873128712L;
        value ^= (long) cellZ * 132897987541L;
        value ^= (long) salt * 42317861L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }

    private static double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long cellKey(int x, int z) {
        return (long) x & 0xffffffffL | (long) z << 32;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static final class Candidate {

        private final double x;
        private final double z;
        private final int cellX;
        private final int cellZ;
        private final int round;

        private Candidate(double x, double z, int cellX, int cellZ, int round) {
            this.x = x;
            this.z = z;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.round = round;
        }
    }
}
