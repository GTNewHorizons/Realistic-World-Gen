package rwg.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

/** A submerged shelf with the same hills as the Cherry Blossom Grove terrain. */
public class TerrainSmallIsland extends TerrainBase {

    private static final float HILL_START = 6f;
    private static final float HILL_HEIGHT = 120f;
    private static final float BASE_HEIGHT = 58f;
    private static final float HILL_WIDTH = 200f;

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border,
            float river) {
        float h = perlin.noise2(x / HILL_WIDTH, y / HILL_WIDTH) * HILL_HEIGHT * river;
        h = h < HILL_START ? HILL_START + ((h - HILL_START) / 4.5f) : h;

        if (h > 0f) {
            float st = h * 1.5f > 15f ? 15f : h * 1.5f;
            h += cell.noise(x / 70D, y / 70D, 1D) * st;
        }

        h += perlin.noise2(x / 20f, y / 20f) * 5f;
        h += perlin.noise2(x / 12f, y / 12f) * 3f;
        h += perlin.noise2(x / 5f, y / 5f) * 1.5f;

        return Math.max(BASE_HEIGHT, BASE_HEIGHT + h);
    }
}
