package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.NearbyCraftingAccess;
import com.explorercraft.seamlesscrafting.NearbyInventoryScanner;
import com.explorercraft.seamlesscrafting.NearbyInventoryScanner.LevelPos;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Recipe book placement pulls from nearby containers once the player's own inventory runs
 * short, instead of giving up on the recipe.
 */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
	@Shadow
	@Final
	private Inventory inventory;

	@Shadow
	@Final
	private ServerPlaceRecipe.CraftingMenuAccess<?> menu;

	@Inject(method = "moveItemToGrid", at = @At("HEAD"), cancellable = true)
	private void seamless$fillFromNearby(Slot slot, Holder<Item> item, int count, CallbackInfoReturnable<Integer> cir) {
		ItemStack slotStack = slot.getItem();
		int availableInPlayer = this.seamless$countInPlayerInventory(item, slotStack);
		if (availableInPlayer >= count) {
			return;
		}

		AbstractCraftingMenu menu = this.seamless$resolveMenu();
		if (!(menu instanceof NearbyCraftingAccess access)) {
			cir.setReturnValue(-1);
			return;
		}

		LevelPos levelPos = NearbyInventoryScanner.getLevelPos(access.seamless$getLevelAccess());
		if (levelPos == null) {
			cir.setReturnValue(-1);
			return;
		}

		List<Container> containers = NearbyInventoryScanner.findNearbyContainers(
				levelPos.level(), levelPos.pos(), NearbyInventoryScanner.getConfiguredRadius(), this.inventory.player);
		if (availableInPlayer + this.seamless$countInContainers(containers, item, slotStack) < count) {
			cir.setReturnValue(-1);
			return;
		}

		int remaining = this.seamless$takeFromPlayerInventory(item, slotStack, slot, count);
		slotStack = slot.getItem();
		if (remaining <= 0) {
			cir.setReturnValue(0);
			return;
		}

		for (Container container : containers) {
			for (int sourceSlot = 0; sourceSlot < container.getContainerSize(); sourceSlot++) {
				ItemStack stack = container.getItem(sourceSlot);
				if (!this.seamless$matches(stack, item, slotStack)) {
					continue;
				}

				int baselineCount = slotStack.isEmpty() ? 0 : slotStack.getCount();
				ItemStack removed = container.removeItem(sourceSlot, Math.min(remaining, stack.getCount()));
				if (removed.isEmpty()) {
					continue;
				}

				if (slotStack.isEmpty()) {
					slot.set(removed);
					slotStack = removed;
				} else {
					slotStack.grow(removed.getCount());
					slot.setChanged();
				}

				access.seamless$recordNearbyWithdrawal(
						container, sourceSlot, this.seamless$getCraftingSlotIndex(menu, slot), removed, removed.getCount(), baselineCount);
				container.setChanged();
				remaining -= removed.getCount();
				if (remaining <= 0) {
					cir.setReturnValue(0);
					return;
				}
			}
		}

		cir.setReturnValue(remaining == count ? -1 : remaining);
	}

	@Inject(method = "clearGrid", at = @At("HEAD"))
	private void seamless$returnNearbyInputsToOrigin(CallbackInfo ci) {
		if (this.seamless$resolveMenu() instanceof NearbyCraftingAccess access) {
			access.seamless$prepareNearbyWithdrawalsForAutofill();
		}
	}

	@Unique
	private boolean seamless$matches(ItemStack stack, Holder<Item> item, ItemStack slotStack) {
		if (stack.isEmpty() || stack.getItem() != item.value() || !Inventory.isUsableForCrafting(stack)) {
			return false;
		}
		return slotStack.isEmpty() || ItemStack.isSameItemSameComponents(slotStack, stack);
	}

	@Unique
	private int seamless$countInContainers(List<Container> containers, Holder<Item> item, ItemStack slotStack) {
		int total = 0;
		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (this.seamless$matches(stack, item, slotStack)) {
					total += stack.getCount();
				}
			}
		}
		return total;
	}

	@Unique
	private int seamless$countInPlayerInventory(Holder<Item> item, ItemStack slotStack) {
		int total = 0;
		for (ItemStack stack : this.inventory.getNonEquipmentItems()) {
			if (this.seamless$matches(stack, item, slotStack)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	@Unique
	private int seamless$takeFromPlayerInventory(Holder<Item> item, ItemStack slotStack, Slot slot, int remaining) {
		int stillNeeded = remaining;
		for (int index = 0; index < this.inventory.getNonEquipmentItems().size() && stillNeeded > 0; index++) {
			ItemStack stack = this.inventory.getItem(index);
			if (!this.seamless$matches(stack, item, slotStack)) {
				continue;
			}

			ItemStack removed = this.inventory.removeItem(index, Math.min(stillNeeded, stack.getCount()));
			if (removed.isEmpty()) {
				continue;
			}

			if (slotStack.isEmpty()) {
				slot.set(removed);
				slotStack = removed;
			} else {
				slotStack.grow(removed.getCount());
				slot.setChanged();
			}

			stillNeeded -= removed.getCount();
		}

		return stillNeeded;
	}

	/** The menu access is an inner class of the menu, so the menu itself has to be dug out. */
	@Unique
	@Nullable
	private AbstractCraftingMenu seamless$resolveMenu() {
		if (this.menu instanceof AbstractCraftingMenu craftingMenu) {
			return craftingMenu;
		}

		Object menuAccess = this.menu;
		if (menuAccess == null) {
			return null;
		}

		for (Field field : menuAccess.getClass().getDeclaredFields()) {
			if (AbstractCraftingMenu.class.isAssignableFrom(field.getType())) {
				field.setAccessible(true);
				try {
					return (AbstractCraftingMenu)field.get(menuAccess);
				} catch (IllegalAccessException ignored) {
					return null;
				}
			}
		}

		return null;
	}

	@Unique
	private int seamless$getCraftingSlotIndex(AbstractCraftingMenu menu, Slot slot) {
		List<Slot> inputSlots = menu.getInputGridSlots();
		for (int index = 0; index < inputSlots.size(); index++) {
			if (inputSlots.get(index) == slot) {
				return index;
			}
		}
		return -1;
	}
}
