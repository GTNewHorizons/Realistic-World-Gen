package rwg2.mixin;

import java.util.Random;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import biomesoplenty.common.world.features.trees.WorldGenSacredOak;
import rwg2.support.WorldgenDiagnostics;

/** Traces BOP sacred-oak writes without changing chunk load or population order. */
@Mixin(WorldGenSacredOak.class)
public abstract class MixinBOPSacredOak {

    @Inject(method = "generate", at = @At("HEAD"))
    private void rwg2$beginTreeTrace(World world, Random random, int x, int y, int z,
            CallbackInfoReturnable<Boolean> callback) {
        WorldgenDiagnostics.beginTree("Biomes O' Plenty Sacred Oak", world, x, y, z);
    }

    @Inject(method = "generate", at = @At("RETURN"))
    private void rwg2$endTreeTrace(World world, Random random, int x, int y, int z,
            CallbackInfoReturnable<Boolean> callback) {
        WorldgenDiagnostics.endTree(callback.getReturnValue());
    }
}
