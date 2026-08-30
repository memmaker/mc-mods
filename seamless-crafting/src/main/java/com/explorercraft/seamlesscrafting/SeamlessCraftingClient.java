package com.explorercraft.seamlesscrafting;

import com.explorercraft.seamlesscrafting.client.NearbyItemsClientState;
import com.explorercraft.seamlesscrafting.net.NearbyHighlightResponsePayload;
import com.explorercraft.seamlesscrafting.net.NearbyItemsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class SeamlessCraftingClient implements ClientModInitializer {
	private int ticksSinceRefresh;

	@Override
	public void onInitializeClient() {
		SeamlessCraftingConfig.load();

		ClientPlayNetworking.registerGlobalReceiver(NearbyItemsPayload.ID, (payload, context) ->
				NearbyItemsClientState.applyPayload(payload));

		ClientPlayNetworking.registerGlobalReceiver(NearbyHighlightResponsePayload.ID, (payload, context) ->
				NearbyItemsClientState.showHighlight(payload.positions()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			NearbyItemsClientState.tick(client);

			if (!(client.gui.screen() instanceof CraftingScreen) && !(client.gui.screen() instanceof InventoryScreen)) {
				this.ticksSinceRefresh = 0;
				return;
			}

			if (++this.ticksSinceRefresh >= SeamlessCraftingConfig.getAutoRefreshTicks()) {
				NearbyItemsClientState.requestUpdate();
				this.ticksSinceRefresh = 0;
			}
		});
	}
}
