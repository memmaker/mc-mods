package com.explorercraft.seamlesscrafting;

import net.minecraft.world.inventory.CraftingContainer;

/** Implemented by {@code AbstractCraftingMenu} through a mixin. */
public interface CraftingGridAccess {
	CraftingContainer seamless$getCraftSlots();
}
