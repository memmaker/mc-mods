package dev.explorercraft.stealthandalert.client;

import dev.explorercraft.stealthandalert.StealthAndAlert;
import dev.explorercraft.stealthandalert.StealthItems;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class StealthAndAlertClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(StealthItems.PEBBLE_PROJECTILE, ThrownItemRenderer::new);

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                StealthAndAlert.id("visibility_bar"), VisibilityBar::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                StealthAndAlert.id("alert_symbols"), AlertSymbols::render);
    }
}
