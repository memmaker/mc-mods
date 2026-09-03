package dev.explorercraft.immersiveaircraft.entity.inventory.slots;

import com.google.gson.JsonObject;
import dev.explorercraft.immersiveaircraft.entity.InventoryVehicleEntity;
import dev.explorercraft.immersiveaircraft.screen.slot.FuelSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class FuelSlotDescription extends TooltippedSlotDescription {
    public FuelSlotDescription(String type, int index, int x, int y, JsonObject json) {
        super(type, index, x, y, json);
    }

    public FuelSlotDescription(String type, FriendlyByteBuf buffer) {
        super(type, buffer);
    }

    @Override
    public Slot getSlot(InventoryVehicleEntity vehicle, Container inventory) {
        return new FuelSlot(inventory, index, x, y);
    }
}
