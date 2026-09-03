package dev.explorercraft.immersiveaircraft.client.render;

// Duck interface implemented by AvatarRenderStateMixin so the vehicle pitch/roll stashed during
// extractRenderState can be read back out during setupRotations.
public interface VehicleRiderRenderState {
    float ia$getVehicleXRot();

    void ia$setVehicleXRot(float xRot);

    float ia$getVehicleRoll();

    void ia$setVehicleRoll(float roll);
}
