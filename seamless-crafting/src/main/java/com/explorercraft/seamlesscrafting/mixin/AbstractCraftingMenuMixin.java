package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.CraftingGridAccess;
import com.explorercraft.seamlesscrafting.NearbyCraftingAccess;
import com.explorercraft.seamlesscrafting.NearbyInventoryScanner;
import com.explorercraft.seamlesscrafting.NearbyInventoryScanner.LevelPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server side: the recipe book counts items in nearby containers as available. */
@Mixin(AbstractCraftingMenu.class)
public class AbstractCraftingMenuMixin implements CraftingGridAccess {
	@Shadow
	@Final
	protected CraftingContainer craftSlots;

	@Override
	public CraftingContainer seamless$getCraftSlots() {
		return this.craftSlots;
	}

	@Inject(method = "fillCraftSlotsStackedContents", at = @At("TAIL"))
	private void seamless$addNearbyItems(StackedItemContents contents, CallbackInfo ci) {
		if (!((Object)this instanceof NearbyCraftingAccess access)) {
			return;
		}

		LevelPos levelPos = NearbyInventoryScanner.getLevelPos(access.seamless$getLevelAccess());
		if (levelPos == null || levelPos.level().isClientSide()) {
			return;
		}

		for (Container container : NearbyInventoryScanner.findNearbyContainers(
				levelPos.level(), levelPos.pos(), NearbyInventoryScanner.getConfiguredRadius())) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (!stack.isEmpty()) {
					contents.accountSimpleStack(stack);
				}
			}
		}
	}
}
