package dev.explorercraft.immersiveaircraft.screen.slot;

import dev.explorercraft.immersiveaircraft.entity.InventoryVehicleEntity;
import dev.explorercraft.immersiveaircraft.entity.inventory.VehicleInventoryDescription;
import dev.explorercraft.immersiveaircraft.item.upgrade.VehicleUpgradeRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeSlot extends Slot {

    private final InventoryVehicleEntity vehicle;
    private final int stackSize;

    public UpgradeSlot(InventoryVehicleEntity vehicle, int stackSize, Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);

        this.vehicle = vehicle;
        this.stackSize = stackSize;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return VehicleUpgradeRegistry.INSTANCE.hasUpgrade(stack.getItem())
                && vehicle.getSlots(VehicleInventoryDescription.UPGRADE).stream().noneMatch(s -> s.getItem() == stack.getItem());
    }

    @Override
    public int getMaxStackSize() {
        return stackSize;
    }
}
