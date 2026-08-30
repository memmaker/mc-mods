package com.explorercraft.seamlesscrafting.gametest;

import com.explorercraft.seamlesscrafting.CraftingGridAccess;
import com.explorercraft.seamlesscrafting.NearbyCraftingAccess;
import com.explorercraft.seamlesscrafting.NearbyInventoryScanner;
import com.explorercraft.seamlesscrafting.SeamlessCrafting;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

public class SeamlessCraftingGameTest {
	/** The scanner has to see into a chest that is inside the radius, and count what is in it. */
	@GameTest
	public void findsItemsInNearbyChest(GameTestHelper helper) {
		BlockPos chestPos = placeChestWithPlanks(helper, new BlockPos(1, 1, 1), 12);
		BlockPos center = helper.absolutePos(new BlockPos(0, 1, 0));

		List<NearbyInventoryScanner.NearbyItemEntry> entries =
				NearbyInventoryScanner.collectItemCounts(helper.getLevel(), center, 8);

		int planks = entries.stream()
				.filter(entry -> entry.stack().is(Items.OAK_PLANKS))
				.mapToInt(NearbyInventoryScanner.NearbyItemEntry::count)
				.sum();
		if (planks != 12) {
			throw helper.assertionException("expected 12 planks nearby, found " + planks);
		}

		List<BlockPos> positions = NearbyInventoryScanner.findContainerPositionsWithItem(
				helper.getLevel(), center, 8, Items.OAK_PLANKS);
		if (!positions.contains(chestPos)) {
			throw helper.assertionException("chest at " + chestPos + " was not located, got " + positions);
		}
		helper.succeed();
	}

	/** A chest beyond the radius must stay invisible, or crafting would reach across the world. */
	@GameTest
	public void ignoresChestsOutsideTheRadius(GameTestHelper helper) {
		placeChestWithPlanks(helper, new BlockPos(1, 1, 1), 12);
		BlockPos center = helper.absolutePos(new BlockPos(0, 1, 0));

		List<Container> containers = NearbyInventoryScanner.findNearbyContainers(helper.getLevel(), center, 0);
		if (!containers.isEmpty()) {
			throw helper.assertionException("radius 0 still found " + containers.size() + " containers");
		}
		helper.succeed();
	}

	/** The menu mixins have to apply, or nothing pulls from a chest at all. */
	@GameTest
	public void craftingMenuTracksNearbyWithdrawals(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CraftingMenu menu = new CraftingMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0))));

		if (!(menu instanceof NearbyCraftingAccess access)) {
			throw helper.assertionException("CraftingMenu does not implement NearbyCraftingAccess");
		}
		if (!(menu instanceof CraftingGridAccess grid) || grid.seamless$getCraftSlots() == null) {
			throw helper.assertionException("AbstractCraftingMenu does not expose its craft slots");
		}
		if (NearbyInventoryScanner.getLevelPos(access.seamless$getLevelAccess()) == null) {
			throw helper.assertionException("the menu did not expose its level access");
		}

		// Cancelling with nothing withdrawn has to be a no-op rather than a crash.
		access.seamless$cancelNearbyWithdrawals();
		helper.succeed();
	}

	/**
	 * The whole point of the mod: clicking a recipe in the book has to fill the grid from a
	 * nearby chest when the player carries none of the ingredients.
	 */
	@GameTest
	public void recipeBookPlacementPullsFromNearbyChest(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getInventory().clearContent();

		BlockPos chestPos = placeChestWithPlanks(helper, new BlockPos(1, 1, 1), 42);
		CraftingMenu menu = new CraftingMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0))));
		player.containerMenu = menu;

		RecipeHolder<?> recipe = helper.getLevel().getServer().getRecipeManager()
				.byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("crafting_table")))
				.orElseThrow(() -> helper.assertionException("no crafting table recipe"));
		menu.handlePlacement(false, false, recipe, helper.getLevel(), player.getInventory());

		int placed = menu.getInputGridSlots().stream().mapToInt(slot -> slot.getItem().getCount()).sum();
		if (placed != 4) {
			throw helper.assertionException("expected 4 planks in the grid, found " + placed);
		}

		Container chest = (Container)helper.getLevel().getBlockEntity(chestPos);
		if (chest.getItem(0).getCount() != 38) {
			throw helper.assertionException("chest should be down to 38 planks, has " + chest.getItem(0).getCount());
		}

		// Cancelling has to put every borrowed plank back where it came from.
		((NearbyCraftingAccess)menu).seamless$cancelNearbyWithdrawals();
		if (chest.getItem(0).getCount() != 42) {
			throw helper.assertionException("cancel left the chest at " + chest.getItem(0).getCount() + " planks");
		}
		helper.succeed();
	}

	/** The debug command has to be registered so the mod can be checked in game. */
	@GameTest
	public void debugCommandIsRegistered(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		if (server.getCommands().getDispatcher().getRoot().getChild(SeamlessCrafting.MOD_ID) == null) {
			throw helper.assertionException("/" + SeamlessCrafting.MOD_ID + " is not registered");
		}
		helper.succeed();
	}

	private static BlockPos placeChestWithPlanks(GameTestHelper helper, BlockPos relativePos, int count) {
		helper.setBlock(relativePos, Blocks.CHEST);
		BlockPos chestPos = helper.absolutePos(relativePos);
		if (!(helper.getLevel().getBlockEntity(chestPos) instanceof Container container)) {
			throw helper.assertionException("no chest at " + chestPos);
		}

		container.setItem(0, new ItemStack(Items.OAK_PLANKS, count));
		container.setChanged();
		return chestPos;
	}
}
