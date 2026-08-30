package com.explorercraft.fxglobals.mixin;

import com.explorercraft.fxglobals.FxGlobalsConfig;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Every source of hunger — sprinting, jumping, mining, regeneration — routes through
 * addExhaustion, so scaling its argument slows the whole bar without touching the
 * places that call it.
 */
@Mixin(FoodData.class)
public class FoodDataMixin {
	@ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
	private float fxglobals$slowHunger(float exhaustion) {
		return exhaustion * FxGlobalsConfig.hungerFactor;
	}
}
