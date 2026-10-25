package rwg.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents individual village pieces from treating frozen or liquid water as a foundation. */
@Mixin(StructureVillagePieces.Village.class)
public abstract class MixinStructureVillagePiecesVillage {

    @Inject(method = "getAverageGroundLevel", at = @At("HEAD"), cancellable = true)
    private void rwg$rejectWaterFoundation(World world, StructureBoundingBox generationBounds,
            CallbackInfoReturnable<Integer> callback) {
        StructureBoundingBox buildingBounds = ((StructureComponent) (Object) this).getBoundingBox();
        for (int z = buildingBounds.minZ; z <= buildingBounds.maxZ; z++) {
            for (int x = buildingBounds.minX; x <= buildingBounds.maxX; x++) {
                if (!generationBounds.isVecInside(x, 64, z)) continue;
                int surfaceY = world.getPrecipitationHeight(x, z) - 1;
                Block foundation = world.getBlock(x, surfaceY, z);
                Material material = foundation.getMaterial();
                if (material == Material.water || material == Material.ice) {
                    callback.setReturnValue(-1);
                    return;
                }
            }
        }
    }
}
