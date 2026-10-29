package rwg2.mixin;

import java.util.Random;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mods.natura.worldgen.RedwoodTreeGen;
import rwg2.support.WorldgenDiagnostics;

/** Traces Natura redwood writes without changing chunk load or population order. */
@Mixin(RedwoodTreeGen.class)
public abstract class MixinNaturaRedwoodTreeGen {

    @Inject(method = "generate", at = @At("HEAD"))
    private void rwg2$beginTreeTrace(World world, Random random, int x, int y, int z,
            CallbackInfoReturnable<Boolean> callback) {
        WorldgenDiagnostics.beginTree("Natura Redwood", world, x, y, z);
    }

    @Inject(method = "generate", at = @At("RETURN"))
    private void rwg2$endTreeTrace(World world, Random random, int x, int y, int z,
            CallbackInfoReturnable<Boolean> callback) {
        WorldgenDiagnostics.endTree(callback.getReturnValue());
    }
}
