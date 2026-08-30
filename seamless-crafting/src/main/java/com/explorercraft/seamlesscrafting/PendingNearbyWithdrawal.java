package com.explorercraft.seamlesscrafting;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** One stack pulled out of a nearby container into the crafting grid, so it can be put back. */
public final class PendingNearbyWithdrawal {
	private final Container sourceContainer;
	private final int sourceSlot;
	private final int craftingSlotIndex;
	private final ItemStack templateStack;
	private int remainingCount;

	public PendingNearbyWithdrawal(Container sourceContainer, int sourceSlot, int craftingSlotIndex, ItemStack templateStack, int remainingCount) {
		this.sourceContainer = sourceContainer;
		this.sourceSlot = sourceSlot;
		this.craftingSlotIndex = craftingSlotIndex;
		this.templateStack = templateStack;
		this.remainingCount = remainingCount;
	}

	public Container sourceContainer() {
		return this.sourceContainer;
	}

	public int sourceSlot() {
		return this.sourceSlot;
	}

	public int craftingSlotIndex() {
		return this.craftingSlotIndex;
	}

	public ItemStack templateStack() {
		return this.templateStack;
	}

	public int remainingCount() {
		return this.remainingCount;
	}

	public void setRemainingCount(int remainingCount) {
		this.remainingCount = remainingCount;
	}
}
