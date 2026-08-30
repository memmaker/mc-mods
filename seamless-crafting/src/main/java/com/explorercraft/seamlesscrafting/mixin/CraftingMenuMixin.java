package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.CraftingGridAccess;
import com.explorercraft.seamlesscrafting.NearbyCraftingAccess;
import com.explorercraft.seamlesscrafting.PendingNearbyWithdrawal;
import com.explorercraft.seamlesscrafting.net.NearbyItemsSync;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks every stack the mod pulled out of a nearby container so the cancel button can put
 * them back. The bookkeeping lives on the server: the client only asks for the return.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin implements NearbyCraftingAccess {
	@Unique
	private final List<PendingNearbyWithdrawal> seamless$pendingWithdrawals = new ArrayList<>();

	@Unique
	private final Map<Integer, Integer> seamless$slotBaselineCounts = new HashMap<>();

	@Unique
	private boolean seamless$reconciling;

	@Unique
	private boolean seamless$cancelling;

	@Unique
	private boolean seamless$placingRecipe;

	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Shadow
	@Final
	private Player player;

	@Shadow
	public abstract List<Slot> getInputGridSlots();

	@Shadow
	public abstract void slotsChanged(Container container);

	@Override
	public ContainerLevelAccess seamless$getLevelAccess() {
		return this.access;
	}

	@Override
	public void seamless$recordNearbyWithdrawal(Container container, int sourceSlot, int craftingSlotIndex, ItemStack stack, int count, int baselineCount) {
		if (count <= 0 || craftingSlotIndex < 0) {
			return;
		}

		this.seamless$slotBaselineCounts.putIfAbsent(craftingSlotIndex, baselineCount);
		this.seamless$pendingWithdrawals.add(new PendingNearbyWithdrawal(
				container,
				sourceSlot,
				craftingSlotIndex,
				stack.copyWithCount(1),
				count
		));
	}

	@Override
	public void seamless$prepareNearbyWithdrawalsForAutofill() {
		this.seamless$returnNearbyWithdrawals(false);
	}

	@Override
	public void seamless$cancelNearbyWithdrawals() {
		this.seamless$returnNearbyWithdrawals(true);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void seamless$sendInitialNearbyItems(int syncId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		if (this.player instanceof ServerPlayer serverPlayer) {
			NearbyItemsSync.sendNearbyItems(serverPlayer);
		}
	}

	@Inject(method = "beginPlacingRecipe", at = @At("HEAD"))
	private void seamless$markPlacingRecipeStart(CallbackInfo ci) {
		this.seamless$placingRecipe = true;
	}

	@Inject(method = "slotsChanged", at = @At("TAIL"))
	private void seamless$reconcileOnSlotsChanged(Container container, CallbackInfo ci) {
		if (!this.seamless$cancelling && !this.seamless$placingRecipe) {
			this.seamless$reconcileWithdrawals();
		}
	}

	@Inject(method = "finishPlacingRecipe", at = @At("TAIL"))
	private void seamless$refreshAfterPlacing(ServerLevel level, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
		this.seamless$placingRecipe = false;
		this.seamless$reconcileWithdrawals();
		if (this.player instanceof ServerPlayer serverPlayer) {
			NearbyItemsSync.sendNearbyItems(serverPlayer);
		}
	}

	@Inject(method = "removed", at = @At("TAIL"))
	private void seamless$clearWithdrawals(Player player, CallbackInfo ci) {
		this.seamless$pendingWithdrawals.clear();
		this.seamless$slotBaselineCounts.clear();
	}

	/** Drops bookkeeping for anything the player has since moved, split or crafted away. */
	@Unique
	private void seamless$reconcileWithdrawals() {
		if (this.seamless$reconciling || this.seamless$pendingWithdrawals.isEmpty()) {
			return;
		}

		this.seamless$reconciling = true;
		try {
			List<Slot> inputSlots = this.getInputGridSlots();
			Map<Integer, Integer> cancelableCounts = new HashMap<>();
			for (Integer slotIndex : this.seamless$slotBaselineCounts.keySet()) {
				if (slotIndex < 0 || slotIndex >= inputSlots.size()) {
					continue;
				}

				ItemStack slotStack = inputSlots.get(slotIndex).getItem();
				if (slotStack.isEmpty()) {
					cancelableCounts.put(slotIndex, 0);
					continue;
				}

				int baselineCount = this.seamless$slotBaselineCounts.getOrDefault(slotIndex, 0);
				cancelableCounts.put(slotIndex, Math.max(0, slotStack.getCount() - baselineCount));
			}

			for (Iterator<PendingNearbyWithdrawal> iterator = this.seamless$pendingWithdrawals.iterator(); iterator.hasNext(); ) {
				PendingNearbyWithdrawal withdrawal = iterator.next();
				if (withdrawal.craftingSlotIndex() < 0 || withdrawal.craftingSlotIndex() >= inputSlots.size()) {
					iterator.remove();
					continue;
				}

				ItemStack slotStack = inputSlots.get(withdrawal.craftingSlotIndex()).getItem();
				if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, withdrawal.templateStack())) {
					iterator.remove();
					continue;
				}

				int availableForCancel = cancelableCounts.getOrDefault(withdrawal.craftingSlotIndex(), 0);
				if (availableForCancel <= 0) {
					iterator.remove();
					continue;
				}

				withdrawal.setRemainingCount(Math.min(withdrawal.remainingCount(), availableForCancel));
				cancelableCounts.put(withdrawal.craftingSlotIndex(), availableForCancel - withdrawal.remainingCount());
				if (withdrawal.remainingCount() <= 0) {
					iterator.remove();
				}
			}

			this.seamless$cleanupSlotBaselines();
		} finally {
			this.seamless$reconciling = false;
		}
	}

	@Unique
	private void seamless$returnNearbyWithdrawals(boolean refreshAfter) {
		boolean changed = false;
		this.seamless$cancelling = true;
		try {
			while (true) {
				this.seamless$reconcileWithdrawals();
				if (this.seamless$pendingWithdrawals.isEmpty()) {
					break;
				}

				List<Slot> inputSlots = this.getInputGridSlots();
				boolean passChanged = false;
				for (Iterator<PendingNearbyWithdrawal> iterator = this.seamless$pendingWithdrawals.iterator(); iterator.hasNext(); ) {
					PendingNearbyWithdrawal withdrawal = iterator.next();
					if (withdrawal.craftingSlotIndex() < 0 || withdrawal.craftingSlotIndex() >= inputSlots.size()) {
						iterator.remove();
						continue;
					}

					Slot slot = inputSlots.get(withdrawal.craftingSlotIndex());
					ItemStack slotStack = slot.getItem();
					if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, withdrawal.templateStack())) {
						iterator.remove();
						continue;
					}

					int removableCount = Math.min(withdrawal.remainingCount(), slotStack.getCount());
					if (removableCount <= 0) {
						iterator.remove();
						continue;
					}

					ItemStack toReturn = withdrawal.templateStack().copyWithCount(removableCount);
					int insertedCount = this.seamless$insertBackIntoContainer(withdrawal.sourceContainer(), withdrawal.sourceSlot(), toReturn);
					if (insertedCount <= 0) {
						continue;
					}

					if (insertedCount == slotStack.getCount()) {
						slot.set(ItemStack.EMPTY);
					} else {
						slotStack.shrink(insertedCount);
						slot.setChanged();
					}

					withdrawal.setRemainingCount(withdrawal.remainingCount() - insertedCount);
					withdrawal.sourceContainer().setChanged();
					passChanged = true;
					changed = true;
					if (withdrawal.remainingCount() <= 0) {
						iterator.remove();
					}
				}

				this.seamless$cleanupSlotBaselines();
				if (!passChanged) {
					break;
				}
			}
		} finally {
			this.seamless$cancelling = false;
		}

		if (changed && refreshAfter) {
			this.seamless$refreshAfterNearbyTransfer();
		}
	}

	@Unique
	private int seamless$insertBackIntoContainer(Container container, int preferredSlot, ItemStack stack) {
		int inserted = this.seamless$tryInsertIntoSlot(container, preferredSlot, stack);
		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			if (slot != preferredSlot) {
				inserted += this.seamless$tryInsertIntoSlot(container, slot, stack);
			}
		}
		return inserted;
	}

	@Unique
	private int seamless$tryInsertIntoSlot(Container container, int slotIndex, ItemStack stack) {
		if (stack.isEmpty() || slotIndex < 0 || slotIndex >= container.getContainerSize() || !container.canPlaceItem(slotIndex, stack)) {
			return 0;
		}

		ItemStack targetStack = container.getItem(slotIndex);
		if (!targetStack.isEmpty() && !ItemStack.isSameItemSameComponents(targetStack, stack)) {
			return 0;
		}

		int maxCount = Math.min(stack.getMaxStackSize(), container.getMaxStackSize(stack));
		if (maxCount <= 0) {
			return 0;
		}

		if (targetStack.isEmpty()) {
			int inserted = Math.min(stack.getCount(), maxCount);
			container.setItem(slotIndex, stack.copyWithCount(inserted));
			stack.shrink(inserted);
			return inserted;
		}

		int inserted = Math.min(stack.getCount(), maxCount - targetStack.getCount());
		if (inserted <= 0) {
			return 0;
		}

		targetStack.grow(inserted);
		stack.shrink(inserted);
		return inserted;
	}

	@Unique
	private void seamless$cleanupSlotBaselines() {
		this.seamless$slotBaselineCounts.entrySet().removeIf(entry ->
				this.seamless$pendingWithdrawals.stream().noneMatch(withdrawal -> withdrawal.craftingSlotIndex() == entry.getKey()));
	}

	@Unique
	private void seamless$refreshAfterNearbyTransfer() {
		Container craftSlots = ((CraftingGridAccess)this).seamless$getCraftSlots();
		craftSlots.setChanged();
		this.slotsChanged(craftSlots);
		((CraftingMenu)(Object)this).broadcastChanges();
		if (this.player instanceof ServerPlayer serverPlayer) {
			NearbyItemsSync.sendNearbyItems(serverPlayer);
		}
	}
}
