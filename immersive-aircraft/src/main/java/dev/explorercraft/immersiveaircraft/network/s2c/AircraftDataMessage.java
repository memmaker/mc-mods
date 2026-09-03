package dev.explorercraft.immersiveaircraft.network.s2c;

import dev.explorercraft.immersiveaircraft.cobalt.network.Message;
import dev.explorercraft.immersiveaircraft.data.VehicleDataLoader;
import dev.explorercraft.immersiveaircraft.entity.misc.VehicleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class AircraftDataMessage extends Message {
    private final Map<Identifier, VehicleData> data;

    public AircraftDataMessage() {
        this.data = VehicleDataLoader.REGISTRY;
    }

    public AircraftDataMessage(FriendlyByteBuf buffer) {
        data = new HashMap<>();

        int dataCount = buffer.readInt();
        for (int i = 0; i < dataCount; i++) {
            Identifier identifier = buffer.readIdentifier();
            data.put(identifier, new VehicleData(buffer));
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(data.size());

        for (Identifier identifier : data.keySet()) {
            buffer.writeIdentifier(identifier);
            data.get(identifier).encode(buffer);
        }
    }

    @Override
    public void receive(Player player) {
        VehicleDataLoader.CLIENT_REGISTRY.clear();
        VehicleDataLoader.CLIENT_REGISTRY.putAll(data);
    }
}
