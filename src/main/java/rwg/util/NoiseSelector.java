package rwg.util;

import rwg.config.ConfigRWG;

public class NoiseSelector {
    public static NoiseGenerator createNoiseGenerator() {

        if (ConfigRWG.noiseFunction.equals("opensimplex")) return new OpenSimplexNoise();

        return new PerlinNoise();
    }

    public static NoiseGenerator createNoiseGenerator(long seed) {

        if (ConfigRWG.noiseFunction.equals("opensimplex")) return new OpenSimplexNoise(seed);

        return new PerlinNoise(seed);
    }
}
