package dev.explorercraft.immersiveaircraft.network;

import dev.explorercraft.immersiveaircraft.network.s2c.FireResponse;
import dev.explorercraft.immersiveaircraft.network.s2c.InventoryUpdateMessage;
import dev.explorercraft.immersiveaircraft.network.s2c.OpenGuiRequest;

public interface NetworkManager {
    void handleOpenGuiRequest(OpenGuiRequest request);

    void handleInventoryUpdate(InventoryUpdateMessage message);

    void handleFire(FireResponse fireResponse);
}
