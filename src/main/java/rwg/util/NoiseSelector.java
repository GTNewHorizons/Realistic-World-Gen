package rwg.util;

public class NoiseSelector {
    public static NoiseGenerator CreateNoiseGenerator(){
        return new PerlinNoise();
    }
    public static NoiseGenerator CreateNoiseGenerator(long seed){
        return new PerlinNoise(seed);
    }
}
