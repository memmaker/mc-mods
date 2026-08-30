package com.explorercraft.seamlesscrafting.net;

import com.explorercraft.seamlesscrafting.NearbyCraftingAccess;
import com.explorercraft.seamlesscrafting.NearbyInventoryScanner;
import com.explorercraft.seamlesscrafting.NearbyInventoryScanner.LevelPos;
import com.explorercraft.seamlesscrafting.NearbyInventoryScanner.NearbyItemEntry;
import com.explorercraft.seamlesscrafting.SeamlessCrafting;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class NearbyItemsSync {
	private NearbyItemsSync() {
	}

	public static void sendNearbyItems(ServerPlayer player) {
		if (!SeamlessCrafting.hasCraftingGrid(player)) {
			return;
		}

		LevelPos levelPos = getLevelPos(player);
		if (levelPos == null) {
			return;
		}

		int radius = NearbyInventoryScanner.getConfiguredRadius();
		List<NearbyItemEntry> entries = NearbyInventoryScanner.collectItemCounts(levelPos.level(), levelPos.pos(), radius);
		List<ItemStack> craftableStacks = NearbyInventoryScanner.collectCraftableStacks(levelPos.level(), levelPos.pos(), radius);
		ServerPlayNetworking.send(player, new NearbyItemsPayload(entries, craftableStacks));
	}

	@Nullable
	public static List<BlockPos> findHighlightPositions(ServerPlayer player, ItemStack stack) {
		LevelPos levelPos = getLevelPos(player);
		if (levelPos == null) {
			return null;
		}

		return NearbyInventoryScanner.findContainerPositionsWithItem(
				levelPos.level(),
				levelPos.pos(),
				NearbyInventoryScanner.getConfiguredRadius(),
				stack.getItem()
		);
	}

	@Nullable
	private static LevelPos getLevelPos(ServerPlayer player) {
		if (player.containerMenu instanceof NearbyCraftingAccess access) {
			ContainerLevelAccess levelAccess = access.seamless$getLevelAccess();
			LevelPos levelPos = NearbyInventoryScanner.getLevelPos(levelAccess);
			if (levelPos != null) {
				return levelPos;
			}
		}

		if (player.containerMenu instanceof InventoryMenu) {
			return new LevelPos(player.level(), player.blockPosition());
		}

		return null;
	}
}
