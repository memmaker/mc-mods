package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.client.NearbyPanelHolder;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Typing goes to the panel's search box before the recipe book sees it. */
@Mixin(AbstractRecipeBookScreen.class)
public class AbstractRecipeBookScreenMixin {
	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void seamless$handleCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
		if ((Object)this instanceof NearbyPanelHolder holder && holder.seamless$getNearbyPanel().charTyped(event)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void seamless$handleKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if ((Object)this instanceof NearbyPanelHolder holder && holder.seamless$getNearbyPanel().keyPressed(event)) {
			cir.setReturnValue(true);
		}
	}
}
