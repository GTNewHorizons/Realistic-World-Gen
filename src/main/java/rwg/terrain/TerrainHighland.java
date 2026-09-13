package rwg.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

public class TerrainHighland extends TerrainBase {

    private float start;
    private float height;
    private float base;
    private float width;
    private float flatDetailStrength;

    public TerrainHighland(float hillStart, float landHeight, float baseHeight, float hillWidth) {
        this(hillStart, landHeight, baseHeight, hillWidth, 1f);
    }

    public TerrainHighland(float hillStart, float landHeight, float baseHeight, float hillWidth,
            float flatDetailStrength) {
        start = hillStart;
        height = landHeight;
        base = baseHeight;
        width = hillWidth;
        this.flatDetailStrength = flatDetailStrength;
    }

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border,
            float river) {
        float h = perlin.noise2(x / width, y / width) * height * river;
        h = h < start ? start + ((h - start) / 4.5f) : h;

        if (h > 0f) {
            float st = h * 1.5f > 15f ? 15f : h * 1.5f;
            h += cell.noise(x / 70D, y / 70D, 1D) * st;
        }

        float hillDetail = Math.max(0f, Math.min(1f, (h - start) / 15f));
        float detailStrength = flatDetailStrength + (1f - flatDetailStrength) * hillDetail;
        h += perlin.noise2(x / 20f, y / 20f) * 5f * detailStrength;
        h += perlin.noise2(x / 12f, y / 12f) * 3f * detailStrength;
        h += perlin.noise2(x / 5f, y / 5f) * 1.5f * detailStrength;

        return base + h;
    }
}
