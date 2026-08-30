package com.explorercraft.fireproof.mixin;

import com.explorercraft.fireproof.FireproofSuit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fire and lava damage, scaled down by whatever of the suit is worn.
 *
 * <p>Both hooks sit on LivingEntity rather than on the player, so a mob wearing the armour is
 * protected on exactly the same terms. There is no vanilla attribute for fire resistance —
 * BURNING_TIME only decides how long you burn — so the reduction has to happen in the damage
 * pipeline itself.
 */
@Mixin(LivingEntity.class)
public class LivingEntityFireMixin {

	/**
	 * The full suit refuses the damage outright instead of taking zero of it: at this point
	 * vanilla has not yet played the hurt sound, flashed the model or spent the hunger, and none
	 * of that should happen to someone who is immune.
	 */
	@Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
	private void fireproofsuit$fullSuitIgnoresFire(ServerLevel level, DamageSource source,
			CallbackInfoReturnable<Boolean> cir) {
		if (source.is(DamageTypeTags.IS_FIRE)
				&& FireproofSuit.fireProtection((LivingEntity) (Object) this) >= 1.0) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * Partial suits scale the damage down. getDamageAfterMagicAbsorb runs after armour and after
	 * the resistance effect, and every fire damage type reaches it — only starve is tagged
	 * bypasses_effects — so a single hook covers in_fire, on_fire, lava, campfire, hot_floor and
	 * fireballs alike.
	 */
	@Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
	private void fireproofsuit$suitAbsorbsFire(DamageSource source, float damage,
			CallbackInfoReturnable<Float> cir) {
		if (!source.is(DamageTypeTags.IS_FIRE)) {
			return;
		}

		double protection = FireproofSuit.fireProtection((LivingEntity) (Object) this);

		if (protection > 0.0) {
			cir.setReturnValue((float) (cir.getReturnValueF() * (1.0 - protection)));
		}
	}
}
