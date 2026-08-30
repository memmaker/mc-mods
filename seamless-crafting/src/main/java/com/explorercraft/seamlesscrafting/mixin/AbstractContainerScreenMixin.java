package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.client.NearbyPanelHolder;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clicks and scrolls over the panel must not fall through to the slots behind it. */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
	@Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
	private void seamless$handleMouseClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if ((Object)this instanceof NearbyPanelHolder holder && holder.seamless$getNearbyPanel().mouseClicked(event)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void seamless$handleScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
		if ((Object)this instanceof NearbyPanelHolder holder
				&& holder.seamless$getNearbyPanel().mouseScrolled(mouseX, mouseY, verticalAmount)) {
			cir.setReturnValue(true);
		}
	}
}
