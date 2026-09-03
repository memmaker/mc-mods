package dev.explorercraft.stealthandalert.client;

import dev.explorercraft.stealthandalert.StealthAndAlert;
import dev.explorercraft.stealthandalert.StealthItems;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class StealthAndAlertClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(StealthItems.PEBBLE_PROJECTILE, ThrownItemRenderer::new);

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                StealthAndAlert.id("visibility_bar"), VisibilityBar::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                StealthAndAlert.id("alert_symbols"), AlertSymbols::render);
    }
}
