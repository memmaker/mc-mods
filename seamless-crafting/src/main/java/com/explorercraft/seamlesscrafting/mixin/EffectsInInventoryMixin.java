package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.client.NearbyPanelHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla draws the effects where the nearby panel sits; EffectStrip draws them instead. */
@Mixin(EffectsInInventory.class)
public class EffectsInInventoryMixin {
	@Shadow
	@Final
	private AbstractContainerScreen<?> screen;

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void seamless$moveEffectsOutOfThePanel(GuiGraphicsExtractor extractor, int mouseX, int mouseY, CallbackInfo ci) {
		if (this.screen instanceof NearbyPanelHolder) {
			ci.cancel();
		}
	}
}
