package rwg2.mixin;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import rwg2.support.WorldgenDiagnostics;

/** Records block writes only while one of the explicitly traced large-tree generators is active. */
@Mixin(World.class)
public abstract class MixinWorld {

    @Inject(method = "setBlock(IIILnet/minecraft/block/Block;II)Z", at = @At("HEAD"))
    private void rwg2$recordLargeTreeWriteAttempt(int x, int y, int z, Block block, int metadata, int flags,
            CallbackInfoReturnable<Boolean> callback) {
        WorldgenDiagnostics.recordTreeWriteAttempt((World) (Object) this, x, y, z);
    }

    @Inject(method = "setBlock(IIILnet/minecraft/block/Block;II)Z", at = @At("RETURN"))
    private void rwg2$recordLargeTreeWrite(int x, int y, int z, Block block, int metadata, int flags,
            CallbackInfoReturnable<Boolean> callback) {
        WorldgenDiagnostics.recordTreeWrite(x, y, z, callback.getReturnValue());
    }
}
