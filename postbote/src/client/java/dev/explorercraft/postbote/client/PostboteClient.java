package dev.explorercraft.postbote.client;

import dev.explorercraft.postbote.Postbote;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;

public class PostboteClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Not addLast: that anchors the element to the vanilla player list, which is only
        // extracted while Tab is held down — so the indicator only ever appeared with the
        // player list open. MISC_OVERLAYS is extracted unconditionally, same as the other
        // HUD elements in this pack.
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Postbote.id("distance_indicator"), PostboteClient::renderDistance);
    }

    private static void renderDistance(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) {
            return;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() != Postbote.COMPASS) {
                continue;
            }

            LodestoneTracker tracker = held.get(DataComponents.LODESTONE_TRACKER);
            GlobalPos target = tracker == null ? null : tracker.target().orElse(null);
            if (target == null || target.dimension() != player.level().dimension()) {
                continue;
            }

            long distanceBlocks = Math.round(Math.sqrt(player.blockPosition().distSqr(target.pos())));
            Component text = Component.translatable("text.postbote.distance", distanceBlocks);
            graphics.text(client.font, text, 4, 4, 0xFFFFFF, true);
            return;
        }
    }
}
