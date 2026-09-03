package dev.explorercraft.immersiveaircraft.network.c2s;

import dev.explorercraft.immersiveaircraft.cobalt.network.Message;
import dev.explorercraft.immersiveaircraft.cobalt.network.NetworkHandler;
import dev.explorercraft.immersiveaircraft.entity.InventoryVehicleEntity;
import dev.explorercraft.immersiveaircraft.network.s2c.InventoryUpdateMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RequestInventory extends Message {
    private final int vehicleId;

    public RequestInventory(int id) {
        this.vehicleId = id;
    }

    public RequestInventory(FriendlyByteBuf b) {
        vehicleId = b.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeInt(vehicleId);
    }

    @Override
    public void receive(Player e) {
        Entity entity = e.level().getEntity(vehicleId);
        if (entity instanceof InventoryVehicleEntity vehicle) {
            for (int i = 0; i < vehicle.getInventoryDescription().getLastSyncIndex(); i++) {
                ItemStack stack = vehicle.getInventory().getItem(i);
                NetworkHandler.sendToPlayer(new InventoryUpdateMessage(this.vehicleId, i, stack), (ServerPlayer) e);
            }
        }
    }
}
