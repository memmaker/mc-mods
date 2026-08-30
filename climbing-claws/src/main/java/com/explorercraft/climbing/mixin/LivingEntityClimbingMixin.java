package com.explorercraft.climbing.mixin;

import com.explorercraft.climbing.ClimbingClaws;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Treat a wall the player is looking straight at as a ladder while the claws
 * sit in the player's hotbar.
 * Reusing vanilla's climbable path gives ladder physics, sneak-to-hold and
 * server-side agreement for free — no custom movement, no fly kicks.
 */
@Mixin(LivingEntity.class)
public class LivingEntityClimbingMixin {
	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	private void climbingclaws$clawsGripWalls(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (self instanceof Player player
				&& !player.isSpectator()
				&& self.horizontalCollision
				&& ClimbingClaws.hasClawsInHotbar(player)
				&& ClimbingClaws.facingWall(player)) {
			cir.setReturnValue(true);
		}
	}
}
