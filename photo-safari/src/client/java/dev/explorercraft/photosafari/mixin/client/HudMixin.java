package dev.explorercraft.photosafari.mixin.client;

import dev.explorercraft.photosafari.client.PhotoSafariClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Camerapture cancels the whole HUD while the viewfinder is up and draws its own overlay
/// instead, which also swallows any Fabric HUD element — so the loot mode indicator only
/// ever appeared once the camera had been lowered again. Drawing at HEAD gets in before
/// that cancel, so the indicator is on screen for as long as the viewfinder is.
@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void photosafari$drawLootIndicator(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
            CallbackInfo ci) {
        PhotoSafariClient.renderLootIndicator(graphics);
    }
}
