package com.explorercraft.seamlesscrafting;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Implemented by {@code CraftingMenu} through a mixin. */
public interface NearbyCraftingAccess {
	ContainerLevelAccess seamless$getLevelAccess();

	void seamless$recordNearbyWithdrawal(Container container, int sourceSlot, int craftingSlotIndex, ItemStack stack, int count, int baselineCount);

	void seamless$prepareNearbyWithdrawalsForAutofill();

	void seamless$cancelNearbyWithdrawals();
}
