package dev.explorercraft.immersiveaircraft.cobalt.registration;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// ponytail: Fabric API's FuelRegistry is gone - vanilla replaced the hardcoded furnace fuel map
// with a data-driven Level.fuelValues() lookup. Custom mod fuels now register their burn time by
// contributing to that same data-driven table instead of a separate Fabric registry; this just
// asks vanilla directly, which still covers all default fuel items.
public class CobaltFuelRegistryImpl extends CobaltFuelRegistry {
    public CobaltFuelRegistryImpl() {
        INSTANCE = this;
    }

    @Override
    public int get(Level level, ItemStack stack) {
        return level.fuelValues().burnDuration(stack);
    }
}
