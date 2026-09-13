package rwg.mixin;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenLiquids;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import rwg.util.ContinentalNoise;
import rwg.world.ChunkManagerRealistic;

/** Prevents spring decorators from flooding the central RWG lava-cave landmark. */
@Mixin(WorldGenLiquids.class)
public abstract class MixinWorldGenLiquids {

    private static final float RWG_WATER_EXCLUSION_RADIUS = 120f;

    @Shadow
    private Block field_150521_a;

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void rwg$preventWaterNearLavaCave(World world, Random random, int x, int y, int z,
            CallbackInfoReturnable<Boolean> callback) {
        if (field_150521_a != Blocks.water && field_150521_a != Blocks.flowing_water) return;
        if (!(world.getWorldChunkManager() instanceof ChunkManagerRealistic)) return;
        long cave = ((ChunkManagerRealistic) world.getWorldChunkManager()).getLavaCaveCoordinates(x, z);
        if (cave == Long.MIN_VALUE) return;
        float caveX = ContinentalNoise.unpackVolcanoX(cave);
        float caveZ = ContinentalNoise.unpackVolcanoY(cave);
        if (caveX * caveX + caveZ * caveZ <= RWG_WATER_EXCLUSION_RADIUS * RWG_WATER_EXCLUSION_RADIUS) {
            callback.setReturnValue(false);
        }
    }
}
