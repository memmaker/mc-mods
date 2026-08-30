package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.net.NearbyItemsSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Crafting consumes nearby items, so the panel counts need a refresh afterwards. */
@Mixin(ResultSlot.class)
public class ResultSlotMixin {
	@Inject(method = "onTake", at = @At("TAIL"))
	private void seamless$refreshNearbyCounts(Player player, ItemStack stack, CallbackInfo ci) {
		if (player instanceof ServerPlayer serverPlayer) {
			NearbyItemsSync.sendNearbyItems(serverPlayer);
		}
	}
}
