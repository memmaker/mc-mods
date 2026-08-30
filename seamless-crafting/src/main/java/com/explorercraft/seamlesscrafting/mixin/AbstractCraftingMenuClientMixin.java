package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.client.NearbyItemsClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client side: the recipe book marks recipes craftable from what the panel last reported. */
@Mixin(AbstractCraftingMenu.class)
public class AbstractCraftingMenuClientMixin {
	@Inject(method = "fillCraftSlotsStackedContents", at = @At("TAIL"))
	private void seamless$addNearbyClientItems(StackedItemContents contents, CallbackInfo ci) {
		if ((Object)this instanceof InventoryMenu || !Minecraft.getInstance().isSameThread()) {
			return;
		}

		for (ItemStack stack : NearbyItemsClientState.getCraftableStacks()) {
			if (!stack.isEmpty()) {
				contents.accountSimpleStack(stack);
			}
		}
	}
}
