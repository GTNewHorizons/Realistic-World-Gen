package rwg.mixin;

import java.util.Random;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import biomesoplenty.common.world.features.trees.WorldGenSacredOak;
import rwg.support.LargeTreeWorldgenCompat;

/** Pre-generates the complete footprint of BOP's sacred oak during RWG decoration. */
@Mixin(WorldGenSacredOak.class)
public abstract class MixinBOPSacredOak {

    @Inject(method = "generate", at = @At("HEAD"))
    private void rwg$prepareTreeFootprint(World world, Random random, int x, int y, int z,
            CallbackInfoReturnable<Boolean> callback) {
        LargeTreeWorldgenCompat.prepareFootprint(world, x, z);
    }
}
