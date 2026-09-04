package dev.explorercraft.crafttracker.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// `hoveredSlot` is protected and has no getter; this is the only way to know what the
/// player is pointing at when they hit the track key.
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("hoveredSlot")
    Slot getHoveredSlot();
}
