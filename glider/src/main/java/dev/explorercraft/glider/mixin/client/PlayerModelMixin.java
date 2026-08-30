package dev.explorercraft.glider.mixin.client;

import dev.explorercraft.glider.GliderMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Both arms overhead on the control bar, legs hanging straight, while the canopy is open. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {

    private static final float GRIP_X_ROT = (float) (Math.PI * 2 - 2.9);

    public PlayerModelMixin(ModelPart part) {
        super(part);
    }

    /**
     * TAIL sits after PlayerModel's own body, which is only part-visibility flags and a call up to
     * HumanoidModel.setupAnim — so the limb angles set here are the last word.
     */
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL"))
    private void glider$gripBar(AvatarRenderState state, CallbackInfo ci) {
        if (!state.getMainHandItemStack().has(GliderMod.GLIDING)) return;
        leftArm.xRot = GRIP_X_ROT;
        leftArm.zRot = 0;
        rightArm.xRot = GRIP_X_ROT;
        rightArm.zRot = 0;
        leftLeg.xRot = 0;
        rightLeg.xRot = 0;
    }
}
