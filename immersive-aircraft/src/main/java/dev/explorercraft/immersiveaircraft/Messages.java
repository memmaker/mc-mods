package dev.explorercraft.immersiveaircraft;

import dev.explorercraft.immersiveaircraft.cobalt.network.NetworkHandler;
import dev.explorercraft.immersiveaircraft.network.c2s.*;
import dev.explorercraft.immersiveaircraft.network.s2c.*;

public class Messages {
    public static void loadMessages() {
        NetworkHandler.registerMessage(EnginePowerMessage.class, EnginePowerMessage::new);
        NetworkHandler.registerMessage(CommandMessage.class, CommandMessage::new);
        NetworkHandler.registerMessage(OpenGuiRequest.class, OpenGuiRequest::new);
        NetworkHandler.registerMessage(InventoryUpdateMessage.class, InventoryUpdateMessage::new);
        NetworkHandler.registerMessage(RequestInventory.class, RequestInventory::new);
        NetworkHandler.registerMessage(CollisionMessage.class, CollisionMessage::new);
        NetworkHandler.registerMessage(VehicleUpgradesMessage.class, VehicleUpgradesMessage::new);
        NetworkHandler.registerMessage(AircraftDataMessage.class, AircraftDataMessage::new);
        NetworkHandler.registerMessage(FireMessage.class, FireMessage::new);
        NetworkHandler.registerMessage(FireResponse.class, FireResponse::new);
    }
}
