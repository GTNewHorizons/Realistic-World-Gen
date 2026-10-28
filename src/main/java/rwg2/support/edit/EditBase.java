package rwg2.support.edit;

import java.util.Random;

import net.minecraft.world.World;

import rwg2.util.CellNoise;
import rwg2.util.NoiseGenerator;

public class EditBase {

    public EditBase() {}

    public void decorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {}
}
