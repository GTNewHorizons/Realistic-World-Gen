package rwg2.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TLongObjectHashMap;

/** Deterministic, fixed-round Poisson dart throwing over an infinite hashed grid. */
public class PoissonPointNoise {

    private static final int CELL_CACHE_LIMIT = 32768;
    private static final int REGION_CACHE_LIMIT = 128;
    private static final double SQRT_TWO = 1.4142135623730951D;

    private final long seed;
    private final double minimumDistance;
    private final double minimumDistanceSquared;
    private final double cellSize;
    private final int neighbourCells;
    private final int rounds;
    private final TLongObjectHashMap<Cell> cells = new TLongObjectHashMap<Cell>();
    private final Map<Long, Candidate[]> regions = new LinkedHashMap<Long, Candidate[]>(
            REGION_CACHE_LIMIT + 1,
            1F,
            false) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Candidate[]> eldest) {
            return size() > REGION_CACHE_LIMIT;
        }
    };

    public PoissonPointNoise(long seed, double minimumDistance, int rounds) {
        if (!(minimumDistance > 0D) || rounds < 1) {
            throw new IllegalArgumentException("minimumDistance and rounds must be positive");
        }
        this.seed = seed;
        this.minimumDistance = minimumDistance;
        this.minimumDistanceSquared = minimumDistance * minimumDistance;
        this.cellSize = minimumDistance / SQRT_TWO;
        this.neighbourCells = (int) Math.ceil(minimumDistance / cellSize) + 1;
        this.rounds = rounds;
    }

    /** Output: distance, owner cell X/Z, point X/Z, and round. */
    public void sample(double x, double z, double[] output) {
        int queryCellX = floor(x / cellSize);
        int queryCellZ = floor(z / cellSize);
        Candidate best = null;
        double bestSquared = Double.POSITIVE_INFINITY;
        for (Candidate candidate : region(queryCellX, queryCellZ)) {
            double dx = candidate.x - x;
            double dz = candidate.z - z;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared < bestSquared) {
                bestSquared = distanceSquared;
                best = candidate;
            }
        }
        output[0] = Math.sqrt(bestSquared);
        output[1] = best.cellX;
        output[2] = best.cellZ;
        output[3] = best.x;
        output[4] = best.z;
        if (output.length > 5) {
            output[5] = best.round;
        }
    }

    /** Output contains the nearest point in slots 0-4 and the second-nearest point in slots 5-9. */
    public void sampleTwo(double x, double z, double[] output) {
        int queryCellX = floor(x / cellSize);
        int queryCellZ = floor(z / cellSize);
        Candidate first = null, second = null;
        double firstSquared = Double.POSITIVE_INFINITY, secondSquared = Double.POSITIVE_INFINITY;
        for (Candidate candidate : region(queryCellX, queryCellZ)) {
            double dx = candidate.x - x;
            double dz = candidate.z - z;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared < firstSquared) {
                second = first;
                secondSquared = firstSquared;
                first = candidate;
                firstSquared = distanceSquared;
            } else if (distanceSquared < secondSquared) {
                second = candidate;
                secondSquared = distanceSquared;
            }
        }
        writeSample(output, 0, first, firstSquared);
        writeSample(output, 5, second, secondSquared);
    }

    private static void writeSample(double[] output, int offset, Candidate candidate, double distanceSquared) {
        output[offset] = Math.sqrt(distanceSquared);
        output[offset + 1] = candidate == null ? 0 : candidate.cellX;
        output[offset + 2] = candidate == null ? 0 : candidate.cellZ;
        output[offset + 3] = candidate == null ? 0 : candidate.x;
        output[offset + 4] = candidate == null ? 0 : candidate.z;
    }

    public double getMinimumDistance() {
        return minimumDistance;
    }

    public double getCellSize() {
        return cellSize;
    }

    public int getRounds() {
        return rounds;
    }

    public boolean isAccepted(int cellX, int cellZ, int round) {
        return isAccepted(candidate(cellX, cellZ, round));
    }

    public void getCandidate(int cellX, int cellZ, int round, double[] output) {
        Candidate candidate = candidate(cellX, cellZ, round);
        output[0] = candidate.x;
        output[1] = candidate.z;
    }

    private synchronized Candidate[] region(int queryCellX, int queryCellZ) {
        long key = cellKey(queryCellX, queryCellZ);
        Candidate[] cached = regions.get(key);
        if (cached != null) {
            return cached;
        }

        List<Candidate> accepted = new ArrayList<Candidate>();
        double upperBoundSquared = Double.POSITIVE_INFINITY;
        for (int ring = 0;; ring++) {
            for (int offsetZ = -ring; offsetZ <= ring; offsetZ++) {
                for (int offsetX = -ring; offsetX <= ring; offsetX++) {
                    if (ring != 0 && Math.abs(offsetX) != ring && Math.abs(offsetZ) != ring) {
                        continue;
                    }
                    int cellX = queryCellX + offsetX;
                    int cellZ = queryCellZ + offsetZ;
                    for (int round = 0; round < rounds; round++) {
                        Candidate candidate = candidate(cellX, cellZ, round);
                        if (isAccepted(candidate)) {
                            accepted.add(candidate);
                            upperBoundSquared = Math.min(
                                    upperBoundSquared,
                                    farthestCellCornerSquared(candidate, queryCellX, queryCellZ));
                        }
                    }
                }
            }
            double unsearchedDistance = ring * cellSize;
            if (!accepted.isEmpty() && unsearchedDistance * unsearchedDistance >= upperBoundSquared) {
                Candidate[] result = accepted.toArray(new Candidate[accepted.size()]);
                regions.put(key, result);
                return result;
            }
        }
    }

    private boolean isAccepted(Candidate candidate) {
        if (candidate.accepted != 0) {
            return candidate.accepted > 0;
        }
        if (!isClearOfEarlierRounds(candidate)) {
            candidate.accepted = -1;
            return false;
        }

        for (int offsetZ = -neighbourCells; offsetZ <= neighbourCells; offsetZ++) {
            for (int offsetX = -neighbourCells; offsetX <= neighbourCells; offsetX++) {
                if (offsetX == 0 && offsetZ == 0) {
                    continue;
                }
                Candidate neighbour = candidate(candidate.cellX + offsetX, candidate.cellZ + offsetZ, candidate.round);
                if (withinDistance(candidate, neighbour) && lowerPriority(neighbour, candidate)
                        && isClearOfEarlierRounds(neighbour)) {
                    candidate.accepted = -1;
                    return false;
                }
            }
        }
        candidate.accepted = 1;
        return true;
    }

    private boolean isClearOfEarlierRounds(Candidate candidate) {
        for (int round = 0; round < candidate.round; round++) {
            for (int offsetZ = -neighbourCells; offsetZ <= neighbourCells; offsetZ++) {
                for (int offsetX = -neighbourCells; offsetX <= neighbourCells; offsetX++) {
                    Candidate earlier = candidate(candidate.cellX + offsetX, candidate.cellZ + offsetZ, round);
                    if (withinDistance(candidate, earlier) && isAccepted(earlier)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean withinDistance(Candidate left, Candidate right) {
        double dx = left.x - right.x;
        double dz = left.z - right.z;
        return dx * dx + dz * dz < minimumDistanceSquared;
    }

    private boolean lowerPriority(Candidate left, Candidate right) {
        int comparison = Long.compareUnsigned(left.priority, right.priority);
        return comparison < 0 || comparison == 0 && cellKey(left.cellX, left.cellZ) < cellKey(right.cellX, right.cellZ);
    }

    private Candidate candidate(int cellX, int cellZ, int round) {
        long key = cellKey(cellX, cellZ);
        Cell cell = cells.get(key);
        if (cell == null) {
            if (cells.size() >= CELL_CACHE_LIMIT) {
                cells.clear();
            }
            cell = new Cell(rounds);
            cells.put(key, cell);
        }
        Candidate candidate = cell.candidates[round];
        if (candidate == null) {
            int salt = round * 3;
            double x = (cellX + unit(hash(cellX, cellZ, salt))) * cellSize;
            double z = (cellZ + unit(hash(cellX, cellZ, salt + 1))) * cellSize;
            candidate = new Candidate(cellX, cellZ, round, x, z, hash(cellX, cellZ, salt + 2));
            cell.candidates[round] = candidate;
        }
        return candidate;
    }

    private double farthestCellCornerSquared(Candidate candidate, int cellX, int cellZ) {
        double minimumX = cellX * cellSize;
        double maximumX = minimumX + cellSize;
        double minimumZ = cellZ * cellSize;
        double maximumZ = minimumZ + cellSize;
        double dx = Math.max(Math.abs(candidate.x - minimumX), Math.abs(candidate.x - maximumX));
        double dz = Math.max(Math.abs(candidate.z - minimumZ), Math.abs(candidate.z - maximumZ));
        return dx * dx + dz * dz;
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

    private static final class Cell {

        private final Candidate[] candidates;

        private Cell(int rounds) {
            candidates = new Candidate[rounds];
        }
    }

    private static final class Candidate {

        private final int cellX;
        private final int cellZ;
        private final int round;
        private final double x;
        private final double z;
        private final long priority;
        private byte accepted;

        private Candidate(int cellX, int cellZ, int round, double x, double z, long priority) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.round = round;
            this.x = x;
            this.z = z;
            this.priority = priority;
        }
    }
}
