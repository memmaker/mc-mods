package com.explorercraft.fireproof.gametest;

import java.util.List;

import com.explorercraft.fireproof.FireproofSuit;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/// Burns three identical cows with the same fire and compares what each one lost: bare, half a
/// suit, and the full four pieces. Cows stand in for the player because the suit is read off
/// worn equipment, which every LivingEntity resolves the same way.
///
/// The tests use the `on_fire` damage type on purpose. It is tagged `bypasses_armor`, so the
/// suit's own armour points cannot muddy the numbers and what is left is exactly the mixin's
/// reduction.
public class FireproofSuitGameTest {
	private static final float BURN = 8.0F;

	@GameTest
	public void suitScalesFireDamageByPieceCount(GameTestHelper helper) {
		LivingEntity bare = helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 1));
		LivingEntity half = helper.spawn(EntityTypes.COW, new BlockPos(3, 2, 1));
		LivingEntity full = helper.spawn(EntityTypes.COW, new BlockPos(5, 2, 1));

		half.setItemSlot(EquipmentSlot.HEAD, new ItemStack(FireproofSuit.FIREPROOF_HELMET));
		half.setItemSlot(EquipmentSlot.FEET, new ItemStack(FireproofSuit.FIREPROOF_BOOTS));
		wearFullSuit(full);

		helper.startSequence()
				// Equipment attribute modifiers are applied on the wearer's next tick.
				.thenIdle(2)
				.thenExecute(() -> {
					float bareLost = burn(bare);
					float halfLost = burn(half);
					float fullLost = burn(full);

					if (bareLost <= 0.0F) {
						throw helper.assertionException(
								"a bare cow shrugged off " + BURN + " fire damage, so the test proves nothing");
					}

					// Half the suit has to take off half of whatever the bare cow actually lost,
					// which is the burn minus whatever the world already softened.
					assertClose(helper, "half a suit", halfLost, bareLost * 0.5F);

					if (fullLost != 0.0F) {
						throw helper.assertionException("a full suit let through " + fullLost + " fire damage");
					}
				})
				.thenSucceed();
	}

	/// The full suit refuses the damage rather than taking zero of it, so no hurt animation,
	/// no sound and no hunger drain.
	@GameTest
	public void fullSuitIsInvulnerableToFire(GameTestHelper helper) {
		LivingEntity cow = helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 1));
		wearFullSuit(cow);

		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					DamageSource lava = cow.damageSources().lava();

					if (!cow.isInvulnerableTo(helper.getLevel(), lava)) {
						throw helper.assertionException("a full suit is not invulnerable to lava");
					}

					DamageSource drown = cow.damageSources().drown();

					if (cow.isInvulnerableTo(helper.getLevel(), drown)) {
						throw helper.assertionException("the suit blocked drowning, which is not fire");
					}
				})
				.thenSucceed();
	}

	/// A suit that stopped the damage but left you permanently ablaze would read as a bug, so
	/// each piece also takes a quarter off the burning-time attribute.
	@GameTest
	public void fullSuitNeverCatchesFire(GameTestHelper helper) {
		LivingEntity cow = helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 1));
		wearFullSuit(cow);

		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					double burningTime = cow.getAttributeValue(Attributes.BURNING_TIME);

					if (burningTime != 0.0) {
						throw helper.assertionException("a full suit left burningTime at " + burningTime);
					}

					cow.igniteForSeconds(10.0F);

					if (cow.isOnFire()) {
						throw helper.assertionException("a full suit caught fire anyway");
					}
				})
				.thenSucceed();
	}

	@GameTest
	public void magmaCreamCraftsEveryPiece(GameTestHelper helper) {
		ItemStack magma = new ItemStack(Items.MAGMA_CREAM);
		ItemStack gap = ItemStack.EMPTY;

		assertCrafts(helper, FireproofSuit.FIREPROOF_HELMET, 3, 2,
				List.of(magma, magma, magma, magma, gap, magma));
		assertCrafts(helper, FireproofSuit.FIREPROOF_CHESTPLATE, 3, 3,
				List.of(magma, gap, magma, magma, magma, magma, magma, magma, magma));
		assertCrafts(helper, FireproofSuit.FIREPROOF_LEGGINGS, 3, 3,
				List.of(magma, magma, magma, magma, gap, magma, magma, gap, magma));
		assertCrafts(helper, FireproofSuit.FIREPROOF_BOOTS, 3, 2,
				List.of(magma, gap, magma, magma, gap, magma));

		helper.succeed();
	}

	/// The debug command has to actually be there, and actually hand over all four pieces.
	@GameTest
	public void debugCommandGivesTheWholeSuit(GameTestHelper helper) {
		var server = helper.getLevel().getServer();

		if (server.getCommands().getDispatcher().getRoot().getChild(FireproofSuit.MOD_ID) == null) {
			throw helper.assertionException("/" + FireproofSuit.MOD_ID + " is not registered");
		}

		var player = helper.makeMockServerPlayerInLevel();
		server.getCommands().performPrefixedCommand(
				server.createCommandSourceStack().withEntity(player), FireproofSuit.MOD_ID + " give");

		int slot = 0;
		for (Item piece : FireproofSuit.PIECES.values()) {
			ItemStack given = player.getInventory().getItem(slot++);

			if (given.getItem() != piece) {
				throw helper.assertionException("command gave " + given + " where " + piece + " was expected");
			}
		}

		helper.succeed();
	}

	private static void wearFullSuit(LivingEntity entity) {
		FireproofSuit.PIECES.forEach((slot, piece) -> entity.setItemSlot(slot, new ItemStack(piece)));
	}

	/// Health lost to one dose of fire. `on_fire` bypasses armour, so this is the mixin's work alone.
	private static float burn(LivingEntity entity) {
		float before = entity.getHealth();
		entity.hurtServer((net.minecraft.server.level.ServerLevel) entity.level(),
				entity.damageSources().onFire(), BURN);
		return before - entity.getHealth();
	}

	private static void assertClose(GameTestHelper helper, String what, float actual, float expected) {
		if (Math.abs(actual - expected) > 0.001F) {
			throw helper.assertionException(what + " let through " + actual + ", expected " + expected);
		}
	}

	/// Runs the grid through the server's own recipe lookup, so a typo in a recipe file shows up
	/// as nothing crafted rather than as a mod that quietly ships an uncraftable item.
	private static void assertCrafts(GameTestHelper helper, Item expected, int width, int height,
			List<ItemStack> grid) {
		CraftingInput input = CraftingInput.of(width, height, grid);
		RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
				.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.orElseThrow(() -> helper.assertionException("no recipe matched the grid for " + expected));
		ItemStack crafted = recipe.value().assemble(input);

		if (!crafted.is(expected)) {
			throw helper.assertionException("the grid for " + expected + " crafted " + crafted);
		}
	}
}
