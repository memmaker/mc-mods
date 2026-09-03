package dev.explorercraft.immersiveaircraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.explorercraft.immersiveaircraft.client.render.VehicleRiderRenderState;
import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Old PlayerEntityRendererMixin injected into PlayerRenderer#setModelProperties (TAIL) and forced
// PlayerModel#crouching = false whenever the player's root vehicle was a VehicleEntity - it never
// hid the model, it only suppressed the crouch pose while seated. PlayerRenderer is now
// AvatarRenderer and #setModelProperties is gone, but the crouch flag it used to poke directly on
// the model now lives on the extracted render state instead: AvatarRenderer#extractRenderState
// calls HumanoidMobRenderer#extractHumanoidRenderState, which sets
// AvatarRenderState/HumanoidRenderState#isCrouching = entity.isCrouching(); that field is read
// later both by PlayerModel's pose setup and by AvatarRenderer#getRenderOffset (which drops the
// render position while crouching). So the equivalent hook is here: inject at the TAIL of
// AvatarRenderer#extractRenderState (public, no access hacks needed) and force isCrouching back
// to false under the same condition upstream used.
//
// Old LivingEntityRendererMixin injected at the TAIL of LivingEntityRenderer#setupRotations(T
// entity, PoseStack, ...) and rotated the PoseStack by the ridden vehicle's pitch/roll, tilting
// the rider's own rendered model in world space - this is what makes the player visibly bank with
// the plane in third person (CameraMixin's roll hack only fires in first person, matching
// upstream's original GameRendererMixin, which was also gated on !isDetached()). setupRotations
// now takes a render state instead of the entity, so there's no entity to read the vehicle off of
// at that call site anymore. Bridged the same way as the crouch flag above: stash the vehicle's
// pitch/roll on the render state during extractRenderState (via the AvatarRenderStateMixin/
// VehicleRiderRenderState duck interface, since AvatarRenderState has no field for this), then
// read it back and apply the rotation at the TAIL of AvatarRenderer#setupRotations.
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ia$extractRenderState(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        VehicleRiderRenderState riderState = (VehicleRiderRenderState) state;
        if (((Entity) entity).getRootVehicle() instanceof VehicleEntity vehicle) {
            state.isCrouching = false;
            riderState.ia$setVehicleXRot(vehicle.getViewXRot(partialTicks));
            riderState.ia$setVehicleRoll(vehicle.getRoll(partialTicks));
        } else {
            riderState.ia$setVehicleXRot(0.0f);
            riderState.ia$setVehicleRoll(0.0f);
        }
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void ia$setupRotations(AvatarRenderState state, PoseStack poseStack, float bodyRot, float entityScale, CallbackInfo ci) {
        VehicleRiderRenderState riderState = (VehicleRiderRenderState) state;
        float xRot = riderState.ia$getVehicleXRot();
        float roll = riderState.ia$getVehicleRoll();
        if (xRot != 0.0f || roll != 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-roll));
        }
    }
}
