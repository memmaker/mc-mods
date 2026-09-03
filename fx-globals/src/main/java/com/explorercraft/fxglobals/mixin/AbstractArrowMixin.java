package com.explorercraft.fxglobals.mixin;

import com.explorercraft.fxglobals.FxGlobalsConfig;
import com.explorercraft.fxglobals.Headshots;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla marks arrows shot by mobs DISALLOWED, so they can never be collected. Answering
 * the same way it answers for a player's own arrows is all "pick up every arrow" needs.
 *
 * <p>CREATIVE_ONLY is deliberately left to vanilla — those arrows exist out of nothing, and
 * letting survival players harvest them would be an item duplicator. The arrow tag keeps
 * this to actual arrows, so a drowned's trident is still loot rather than a free trident.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	@Shadow
	public AbstractArrow.Pickup pickup;

	@Shadow
	protected abstract ItemStack getPickupItem();

	@Inject(method = "tryPickup", at = @At("HEAD"), cancellable = true)
	private void fxglobals$pickUpStrayArrows(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (!FxGlobalsConfig.pickupArrows || this.pickup != AbstractArrow.Pickup.DISALLOWED) {
			return;
		}

		ItemStack stack = this.getPickupItem();

		if (stack.is(holder -> holder.is(ItemTags.ARROWS))) {
			cir.setReturnValue(player.getInventory().add(stack));
		}
	}

	/**
	 * The hit result only carries where the arrow met the target's full hitbox, not whether that
	 * was "the head" — so this re-plays the arrow's own flight for this tick as a ray and asks
	 * {@link Headshots#apply} to clip it against the head box. This runs before the outer move()
	 * has repositioned the arrow, so {@code position()} is still the tick's starting point.
	 */
	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void fxglobals$headshot(EntityHitResult result, CallbackInfo ci) {
		Entity target = result.getEntity();

		if (target instanceof LivingEntity living) {
			AbstractArrow self = (AbstractArrow) (Object) this;
			Vec3 start = self.position();
			Vec3 end = start.add(self.getDeltaMovement());
			Headshots.apply(living, start, end);
		}
	}
}
