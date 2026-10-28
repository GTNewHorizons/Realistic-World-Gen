package rwg2.mixin;

import net.minecraft.client.entity.EntityPlayerSP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Makes vertical creative flight honor the multiplier carried by the synchronized walking speed. */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    private static final double VANILLA_VERTICAL_FLIGHT_SPEED = 0.15d;
    private static final float VANILLA_WALK_SPEED = 0.1f;

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(doubleValue = VANILLA_VERTICAL_FLIGHT_SPEED))
    private double rwg2$scaleVerticalFlight(double speed) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        return speed * player.capabilities.getWalkSpeed() / VANILLA_WALK_SPEED;
    }
}
