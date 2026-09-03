package dev.explorercraft.immersiveaircraft.mixin.client;

import dev.explorercraft.immersiveaircraft.client.render.VehicleRiderRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// Carries the ridden vehicle's pitch/roll from AvatarRendererMixin#extractRenderState to
// AvatarRendererMixin#setupRotations, since setupRotations no longer has entity access - only the
// render state extracted earlier in the frame.
@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements VehicleRiderRenderState {
    @Unique
    private float ia$vehicleXRot;
    @Unique
    private float ia$vehicleRoll;

    @Override
    public float ia$getVehicleXRot() {
        return ia$vehicleXRot;
    }

    @Override
    public void ia$setVehicleXRot(float xRot) {
        ia$vehicleXRot = xRot;
    }

    @Override
    public float ia$getVehicleRoll() {
        return ia$vehicleRoll;
    }

    @Override
    public void ia$setVehicleRoll(float roll) {
        ia$vehicleRoll = roll;
    }
}
