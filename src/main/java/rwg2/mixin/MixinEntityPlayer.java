package rwg2.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies horizontal flight speed after Et Futurum's flight-speed hook, which otherwise overwrites it. */
@Mixin(value = EntityPlayer.class, priority = 900)
public abstract class MixinEntityPlayer extends EntityLivingBase {

    private static final float VANILLA_WALK_SPEED = 0.1f;
    private static final float VANILLA_FLY_SPEED = 0.05f;

    @Shadow
    public PlayerCapabilities capabilities;

    protected MixinEntityPlayer(World world) {
        super(world);
    }

    @Inject(
            method = "moveEntityWithHeading",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;moveEntityWithHeading(FF)V",
                    ordinal = 0))
    private void rwg2$applyHorizontalFlightSpeed(float strafe, float forward, CallbackInfo callback) {
        if (capabilities.isFlying) {
            jumpMovementFactor = VANILLA_FLY_SPEED * capabilities.getWalkSpeed() / VANILLA_WALK_SPEED;
        }
    }
}
