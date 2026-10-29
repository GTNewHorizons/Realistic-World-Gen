package rwg2.mixin;

import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import rwg2.support.WorldgenDiagnostics;

/** Reports village discovery, including any intersecting chunks populated before the start was known. */
@Mixin(MapGenVillage.class)
public abstract class MixinMapGenVillage extends MapGenStructure {

    @Inject(method = "getStructureStart", at = @At("RETURN"))
    private void rwg2$recordVillageStart(int chunkX, int chunkZ, CallbackInfoReturnable<StructureStart> callback) {
        WorldgenDiagnostics.recordVillageStart(worldObj, chunkX, chunkZ, callback.getReturnValue());
    }
}
