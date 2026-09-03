package dev.explorercraft.immersiveaircraft.cobalt.registration;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class CobaltFuelRegistry {
    public static CobaltFuelRegistry INSTANCE = null;

    public abstract int get(Level level, ItemStack stack);
}
