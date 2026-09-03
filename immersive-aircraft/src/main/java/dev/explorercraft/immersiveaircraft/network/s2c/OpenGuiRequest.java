package dev.explorercraft.immersiveaircraft.network.s2c;

import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.cobalt.network.Message;
import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class OpenGuiRequest extends Message {
    private final int vehicle;
    private final int syncId;

    public OpenGuiRequest(VehicleEntity vehicle, int syncId) {
        this.vehicle = vehicle.getId();
        this.syncId = syncId;
    }

    public OpenGuiRequest(FriendlyByteBuf b) {
        vehicle = b.readInt();
        syncId = b.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeInt(vehicle);
        b.writeInt(syncId);
    }

    @Override
    public void receive(Player e) {
        Main.networkManager.handleOpenGuiRequest(this);
    }

    public int getVehicle() {
        return vehicle;
    }

    public int getSyncId() {
        return syncId;
    }
}
