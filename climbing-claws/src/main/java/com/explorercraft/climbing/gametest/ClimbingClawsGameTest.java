package com.explorercraft.climbing.gametest;

import java.util.List;
import java.util.Map;

import com.explorercraft.climbing.ClimbingClaws;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

public class ClimbingClawsGameTest {
	private static final Map<Item, Integer> EXPECTED_TINTS = Map.of(
			Items.IRON_INGOT, 0xD8D8D8,
			Items.COPPER_INGOT, 0xE0794B,
			Items.GOLD_INGOT, 0xF5D145,
			Items.NETHERITE_INGOT, 0x6E5B57);

	/** Every metal crafts, in both triangle orientations, and carries its own tint. */
	@GameTest
	public void everyMetalCraftsTintedClaws(GameTestHelper helper) {
		EXPECTED_TINTS.forEach((ingot, tint) -> {
			ItemStack ingotStack = new ItemStack(ingot);
			ItemStack empty = ItemStack.EMPTY;

			assertCrafts(helper, CraftingInput.of(2, 3, List.of(
					ingotStack, empty,
					empty, ingotStack,
					ingotStack, empty)), tint, ingot + " vertical");

			assertCrafts(helper, CraftingInput.of(3, 2, List.of(
					empty, ingotStack, empty,
					ingotStack, empty, ingotStack)), tint, ingot + " upright");
		});
		helper.succeed();
	}

	/** Mixed metals must not craft — otherwise the tint would be arbitrary. */
	@GameTest
	public void mixedMetalsDoNotCraft(GameTestHelper helper) {
		CraftingInput mixed = CraftingInput.of(2, 3, List.of(
				new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
				ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT),
				new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY));

		if (helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, mixed, helper.getLevel()).isPresent()) {
			throw helper.assertionException("mixed metals should not craft claws");
		}
		helper.succeed();
	}

	/** The whole point: claws anywhere in the hotbar turn a faced wall into a ladder. */
	@GameTest
	public void clawsInHotbarMakeWallsClimbable(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayerInLevel();
		player.horizontalCollision = true;
		faceWall(player);

		if (player.onClimbable()) {
			throw helper.assertionException("bare-handed player should not climb");
		}

		player.getInventory().setItem(8, new ItemStack(ClimbingClaws.CLIMBING_CLAWS));

		if (!player.onClimbable()) {
			throw helper.assertionException("claws in the hotbar should make the wall climbable");
		}

		// Same claws, deep in the backpack: no grip.
		player.getInventory().setItem(8, ItemStack.EMPTY);
		player.getInventory().setItem(20, new ItemStack(ClimbingClaws.CLIMBING_CLAWS));

		if (player.onClimbable()) {
			throw helper.assertionException("claws outside the hotbar should not make the wall climbable");
		}

		helper.succeed();
	}

	/** Turning away from the wall, or looking along it, drops the grip. */
	@GameTest
	public void clawsNeedThePlayerToFaceTheWall(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayerInLevel();
		player.horizontalCollision = true;
		player.getInventory().setItem(0, new ItemStack(ClimbingClaws.CLIMBING_CLAWS));
		faceWall(player);

		if (!player.onClimbable()) {
			throw helper.assertionException("facing the wall should climb");
		}

		player.setYRot(player.getYRot() + 180.0F);
		if (player.onClimbable()) {
			throw helper.assertionException("back to the wall should not climb");
		}

		player.setYRot(player.getYRot() + 90.0F);
		if (player.onClimbable()) {
			throw helper.assertionException("looking sideways along the wall should not climb");
		}

		helper.succeed();
	}

	/** Topping out: only a sliver of wall left beside the feet still has to grip. */
	@GameTest
	public void clawsGripTheLastSliverOfWall(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayerInLevel();
		player.horizontalCollision = true;
		player.getInventory().setItem(0, new ItemStack(ClimbingClaws.CLIMBING_CLAWS));
		faceWall(player);

		BlockPos wall = player.blockPosition().south();
		helper.getLevel().removeBlock(wall.above(), false);
		player.snapTo(player.getX(), wall.getY() + 0.9, player.getZ(), player.getYRot(), 0.0F);

		if (!player.onClimbable()) {
			throw helper.assertionException("wall sliver at the feet should still grip");
		}
		helper.succeed();
	}

	/**
	 * Two blocks of wall south of the player, the player pressed against them (hitbox half-width
	 * off the face, as vanilla collision leaves them) and looking south.
	 */
	private void faceWall(Player player) {
		BlockPos wall = player.blockPosition().south();
		for (BlockPos pos : List.of(wall, wall.above())) {
			player.level().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		player.snapTo(player.getX(), player.getY(), wall.getZ() - player.getBbWidth() / 2.0,
				Direction.SOUTH.toYRot(), 0.0F);
	}

	/** No wall to grip means no climbing, claws or not. */
	@GameTest
	public void clawsDoNotClimbThinAir(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayerInLevel();
		player.getInventory().setItem(0, new ItemStack(ClimbingClaws.CLIMBING_CLAWS));
		faceWall(player);
		player.horizontalCollision = false;

		if (player.onClimbable()) {
			throw helper.assertionException("claws should need a wall to grip");
		}
		helper.succeed();
	}

	/** The debug command has to actually be there, and actually hand over claws. */
	@GameTest
	public void debugCommandGivesClaws(GameTestHelper helper) {
		var server = helper.getLevel().getServer();

		if (server.getCommands().getDispatcher().getRoot().getChild(ClimbingClaws.MOD_ID) == null) {
			throw helper.assertionException("/" + ClimbingClaws.MOD_ID + " is not registered");
		}

		Player player = helper.makeMockServerPlayerInLevel();
		server.getCommands().performPrefixedCommand(
				server.createCommandSourceStack().withEntity(player), ClimbingClaws.MOD_ID + " copper");

		ItemStack given = player.getInventory().getItem(0);

		if (given.getItem() != ClimbingClaws.CLIMBING_CLAWS) {
			throw helper.assertionException("command gave " + given + " instead of claws");
		}

		if ((DyedItemColor.getOrDefault(given, -1) & 0xFFFFFF) != 0xE0794B) {
			throw helper.assertionException("command ignored the requested metal");
		}

		helper.succeed();
	}

	private void assertCrafts(GameTestHelper helper, CraftingInput input, int expectedTint, String what) {
		ItemStack result = helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.map(holder -> holder.value().assemble(input))
				.orElseThrow(() -> helper.assertionException("no recipe matched for " + what));

		if (result.getItem() != ClimbingClaws.CLIMBING_CLAWS) {
			throw helper.assertionException("wrong result for " + what + ": " + result);
		}

		// getOrDefault hands back opaque ARGB; the recipe only sets RGB.
		int tint = DyedItemColor.getOrDefault(result, -1) & 0xFFFFFF;

		if (tint != expectedTint) {
			throw helper.assertionException(
					"wrong tint for %s: expected %06X, got %06X".formatted(what, expectedTint, tint));
		}
	}
}
