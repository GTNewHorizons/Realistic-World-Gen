package rwg2.util;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;

/** Selects deterministic island seeds from the unused darts in the continent Poisson field. */
final class IslandPointNoise {

    private static final int CACHE_LIMIT = 32768;

    private final long seed;
    private final PoissonPointNoise continents;
    private final double maximumContinentWidth;
    private final double minimumIslandWidth;
    private final double islandWidthRange;
    private final double maximumIslandWidth;
    private final double minimumOceanWidth;
    private final double placementChance;
    private final int islandSearchCells;
    private final int neighbourCells;
    private final TLongObjectHashMap<IslandCandidate[]> candidates = new TLongObjectHashMap<IslandCandidate[]>();
    private final TLongObjectHashMap<IslandCandidate[]> regions = new TLongObjectHashMap<IslandCandidate[]>();
    private final double[] point = new double[2];
    private final double[] nearestContinent = new double[5];

    IslandPointNoise(long seed, PoissonPointNoise continents, double maximumContinentWidth, double firstIslandWidth,
            double secondIslandWidth, double minimumOceanWidth, double placementChance) {
        this.seed = seed;
        this.continents = continents;
        this.maximumContinentWidth = maximumContinentWidth;
        minimumIslandWidth = Math.min(firstIslandWidth, secondIslandWidth);
        maximumIslandWidth = Math.max(firstIslandWidth, secondIslandWidth);
        islandWidthRange = maximumIslandWidth - minimumIslandWidth;
        this.minimumOceanWidth = minimumOceanWidth;
        this.placementChance = placementChance;
        islandSearchCells = (int) Math.ceil(maximumIslandWidth / continents.getCellSize()) + 1;
        neighbourCells = (int) Math.ceil((maximumIslandWidth * 2D + minimumOceanWidth) / continents.getCellSize()) + 1;
    }

    /** Output: winning island field value, radius, seed X, and seed Z. */
    synchronized void sample(double x, double z, double[] output) {
        int cellX = floor(x / continents.getCellSize());
        int cellZ = floor(z / continents.getCellSize());
        double best = -Double.MAX_VALUE;
        double bestWidth = 0D;
        double bestX = 0D;
        double bestZ = 0D;
        for (IslandCandidate candidate : region(cellX, cellZ)) {
            double dx = x - candidate.x;
            double dz = z - candidate.z;
            double distanceSquared = dx * dx + dz * dz;
            if (best != -Double.MAX_VALUE) {
                double improvementRadius = candidate.width - best;
                if (improvementRadius <= 0D || distanceSquared >= improvementRadius * improvementRadius) continue;
            }
            double value = candidate.width - Math.sqrt(distanceSquared);
            if (value > best) {
                best = value;
                bestWidth = candidate.width;
                bestX = candidate.x;
                bestZ = candidate.z;
            }
        }
        output[0] = best;
        output[1] = bestWidth;
        output[2] = bestX;
        output[3] = bestZ;
    }

    private IslandCandidate[] region(int cellX, int cellZ) {
        long key = cellKey(cellX, cellZ);
        IslandCandidate[] cached = regions.get(key);
        if (cached != null) return cached;
        if (regions.size() >= CACHE_LIMIT) regions.clear();

        List<IslandCandidate> placed = new ArrayList<IslandCandidate>();
        for (int offsetZ = -islandSearchCells; offsetZ <= islandSearchCells; offsetZ++) {
            for (int offsetX = -islandSearchCells; offsetX <= islandSearchCells; offsetX++) {
                for (int round = 0; round < continents.getRounds(); round++) {
                    IslandCandidate candidate = candidate(cellX + offsetX, cellZ + offsetZ, round);
                    if (isPlaced(candidate)) placed.add(candidate);
                }
            }
        }
        IslandCandidate[] result = placed.toArray(new IslandCandidate[placed.size()]);
        regions.put(key, result);
        return result;
    }

    private boolean isPlaced(IslandCandidate candidate) {
        return isIdentified(candidate)
                && unit(hash(candidate.cellX, candidate.cellZ, candidate.round * 4 + 3)) < placementChance;
    }

    private boolean isIdentified(IslandCandidate candidate) {
        if (candidate.identified != 0) {
            return candidate.identified > 0;
        }
        if (!isHoleCandidate(candidate)) {
            candidate.identified = -1;
            return false;
        }

        for (int offsetZ = -neighbourCells; offsetZ <= neighbourCells; offsetZ++) {
            for (int offsetX = -neighbourCells; offsetX <= neighbourCells; offsetX++) {
                for (int round = 0; round < continents.getRounds(); round++) {
                    IslandCandidate neighbour = candidate(candidate.cellX + offsetX, candidate.cellZ + offsetZ, round);
                    if (neighbour == candidate || !isHoleCandidate(neighbour) || !overlaps(candidate, neighbour)) {
                        continue;
                    }
                    if (Long.compareUnsigned(neighbour.priority, candidate.priority) < 0) {
                        candidate.identified = -1;
                        return false;
                    }
                }
            }
        }
        candidate.identified = 1;
        return true;
    }

    private boolean isHoleCandidate(IslandCandidate candidate) {
        if (candidate.hole != 0) {
            return candidate.hole > 0;
        }
        if (continents.isAccepted(candidate.cellX, candidate.cellZ, candidate.round)) {
            candidate.hole = -1;
            return false;
        }
        continents.sample(candidate.x, candidate.z, nearestContinent);
        double requiredClearance = maximumContinentWidth + candidate.width + minimumOceanWidth;
        candidate.hole = nearestContinent[0] >= requiredClearance ? (byte) 1 : (byte) -1;
        return candidate.hole > 0;
    }

    private boolean overlaps(IslandCandidate left, IslandCandidate right) {
        double dx = left.x - right.x;
        double dz = left.z - right.z;
        double minimumDistance = left.width + right.width + minimumOceanWidth;
        return dx * dx + dz * dz < minimumDistance * minimumDistance;
    }

    private IslandCandidate candidate(int cellX, int cellZ, int round) {
        long key = cellKey(cellX, cellZ);
        IslandCandidate[] cell = candidates.get(key);
        if (cell == null) {
            if (candidates.size() >= CACHE_LIMIT) {
                candidates.clear();
            }
            cell = new IslandCandidate[continents.getRounds()];
            candidates.put(key, cell);
        }
        IslandCandidate candidate = cell[round];
        if (candidate == null) {
            continents.getCandidate(cellX, cellZ, round, point);
            double width = minimumIslandWidth + unit(hash(cellX, cellZ, round * 4)) * islandWidthRange;
            candidate = new IslandCandidate(
                    cellX,
                    cellZ,
                    round,
                    point[0],
                    point[1],
                    width,
                    hash(cellX, cellZ, round * 4 + 1));
            cell[round] = candidate;
        }
        return candidate;
    }

    private long hash(int cellX, int cellZ, int salt) {
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

    private double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private long cellKey(int cellX, int cellZ) {
        return (long) cellX & 0xffffffffL | (long) cellZ << 32;
    }

    private int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static final class IslandCandidate {

        private final int cellX;
        private final int cellZ;
        private final int round;
        private final double x;
        private final double z;
        private final double width;
        private final long priority;
        private byte hole;
        private byte identified;

        private IslandCandidate(int cellX, int cellZ, int round, double x, double z, double width, long priority) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.round = round;
            this.x = x;
            this.z = z;
            this.width = width;
            this.priority = priority;
        }
    }
}
