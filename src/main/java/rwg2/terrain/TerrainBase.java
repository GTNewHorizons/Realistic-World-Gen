package rwg2.terrain;

import rwg2.util.CellNoise;
import rwg2.util.NoiseGenerator;

public class TerrainBase {

    public TerrainBase() {}

    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border,
            float river) {
        return 70f;
    }
}
