package dev.explorercraft.immersiveaircraft.screen.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FuelSlot extends Slot {
    public FuelSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // ponytail: Slot has no Level reference to consult Utils.getFuelTime's new
        // fuelValues()-based lookup; accept anything here and let EngineVehicle.refuel()
        // (which does have a level) decide whether it actually burns.
        return true;
    }
}

