package com.explorercraft.climbing.mixin;

import com.explorercraft.climbing.ClimbingClaws;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Treat a wall the player is looking straight at as a ladder while the claws
 * are held in a hand.
 * Reusing vanilla's climbable path gives ladder physics, sneak-to-hold and
 * server-side agreement for free — no custom movement, no fly kicks.
 */
@Mixin(LivingEntity.class)
public class LivingEntityClimbingMixin {
	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	private void climbingclaws$clawsGripWalls(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (self instanceof Player player && ClimbingClaws.clawGrip(player) > 0.0F) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * Vanilla pins the climb to a flat 0.2 blocks/tick; scale that by the metal of the claws.
	 * Only upward motion is touched, so sliding down and sneak-to-hold stay vanilla.
	 */
	@Inject(method = "travel", at = @At("TAIL"))
	private void climbingclaws$tieredClimbSpeed(Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		Vec3 delta = self.getDeltaMovement();

		if (delta.y <= 0.0 || !(self instanceof Player player)) {
			return;
		}

		float speed = ClimbingClaws.clawGrip(player);

		if (speed > 0.0F) {
			self.setDeltaMovement(delta.x, delta.y * speed, delta.z);
		}
	}
}
