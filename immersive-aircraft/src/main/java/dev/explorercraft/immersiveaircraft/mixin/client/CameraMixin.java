package dev.explorercraft.immersiveaircraft.mixin.client;

import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// What's ported here: camera roll/pitch matching the vehicle's bank angle, which upstream
// implemented in GameRendererMixin by injecting extra PoseStack rotations right after
// GameRenderer#renderLevel's Camera#setup call. That whole call shape is gone (renderLevel is now
// renderLevel(DeltaTracker), with no PoseStack and no Camera#setup call inside it at all - camera
// state is instead produced once via Camera#extractRenderState(CameraRenderState, float) and
// consumed later by the deferred render pipeline). The equivalent hook is here: rotate the
// extracted CameraRenderState's orientation quaternion by the same roll/pitch upstream applied.
//
// Also ported: the third-person zoom offset while riding a vehicle. Upstream's old CameraMixin
// injected at the TAIL of Camera#setup(...) and called the (then-)protected move/getMaxZoom
// helpers directly. Camera#setup is gone, replaced by a private Camera#alignWithEntity(float)
// (called from the public Camera#update(DeltaTracker)) with an attribute-driven
// (Attributes.CAMERA_DISTANCE) base zoom for LivingEntity riders/mounts - but Mixin injects into
// bytecode merged directly into the target class, so a private target method is just as valid an
// injection point as a public one; only @Shadow'ing a private member needs the "private" (not
// "public") modifier to match. alignWithEntity already moves the detached (third-person) camera
// back by the rider's own base distance; this mixin runs after that and, when the root vehicle is
// our own VehicleEntity, nudges the camera back further by vehicle.getZoom() - mirroring
// upstream's original (additive) behavior exactly.
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ia$extractRenderState(CameraRenderState cameraState, float cameraEntityPartialTicks, CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        Entity entity = self.entity();
        if (entity != null && !self.isDetached() && entity.getRootVehicle() instanceof VehicleEntity vehicle && vehicle.adaptPlayerRotation) {
            float roll = (float) Math.toRadians(vehicle.getRoll(cameraEntityPartialTicks));
            float pitch = (float) Math.toRadians(vehicle.getViewXRot(cameraEntityPartialTicks));
            cameraState.orientation.mul(new Quaternionf().rotationZ(roll)).mul(new Quaternionf().rotationX(pitch));
        }
    }

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void ia$alignWithEntity(float partialTicks, CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        Entity entity = self.entity();
        if (self.isDetached() && entity != null && entity.getVehicle() instanceof VehicleEntity vehicle) {
            move(-getMaxZoom((float) vehicle.getZoom()), 0.0f, 0.0f);
        }
    }

    @Shadow
    protected abstract void move(float forwards, float up, float right);

    @Shadow
    private float getMaxZoom(float cameraDist) {
        throw new AssertionError();
    }
}
