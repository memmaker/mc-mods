package dev.explorercraft.immersiveaircraft.item;

// ponytail: DyeableLeatherItem (and the cauldron-washes-off-dye hookup that used it) was removed
// with the old NBT-based item dye system; this mod tracks vehicle dye color itself via
// DyeableVehicleEntity, so the item class only needs to exist as a distinct AircraftItem type.
public class DyeableAircraftItem extends AircraftItem {
    public DyeableAircraftItem(Properties settings, AircraftConstructor constructor) {
        super(settings, constructor);
    }
}
