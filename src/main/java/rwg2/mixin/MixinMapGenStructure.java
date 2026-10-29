package rwg2.mixin;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import rwg2.support.WorldgenDiagnostics;

/** Reports the chunks in which a known village actually emits blocks. */
@Mixin(MapGenStructure.class)
public abstract class MixinMapGenStructure {

    @Inject(method = "generateStructuresInChunk", at = @At("RETURN"))
    private void rwg2$recordVillageEmission(World world, Random random, int chunkX, int chunkZ,
            CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof MapGenVillage && callback.getReturnValue()) {
            WorldgenDiagnostics.recordVillageEmission(world, chunkX, chunkZ);
        }
    }
}
