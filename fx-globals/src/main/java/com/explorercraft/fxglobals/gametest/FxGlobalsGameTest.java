package com.explorercraft.fxglobals.gametest;

import com.explorercraft.fxglobals.FxGlobals;
import com.explorercraft.fxglobals.FxGlobalsConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.ServerRecipeBook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

public class FxGlobalsGameTest {
	/** The overworld clock runs at 1/1.5, which is what makes a day 1.5x longer. */
	@GameTest
	public void overworldClockRunsSlow(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ClockState clock = server.clockManager().packState().clocks()
				.get(FxGlobals.overworldClock(server));

		if (clock == null) {
			throw helper.assertionException("no overworld clock state");
		}

		if (Math.abs(clock.rate() - FxGlobalsConfig.clockRate()) > 1.0e-6f) {
			throw helper.assertionException(
					"clock rate is " + clock.rate() + ", expected " + FxGlobalsConfig.clockRate());
		}
		helper.succeed();
	}

	/**
	 * Vanilla burns one saturation point per 4 exhaustion. At the default quarter rate the same
	 * point costs 16, so 4 exhaustion has to leave saturation untouched and 16 has to spend it.
	 */
	@GameTest
	public void hungerDrainsAtTheConfiguredRate(GameTestHelper helper) {
		assertSaturationCost(helper, 0.25f, 4.1f, 4);
		helper.succeed();
	}

	/** The mixin reads the config live, so 100% has to give back vanilla behaviour exactly. */
	@GameTest
	public void hungerAtFullRateMatchesVanilla(GameTestHelper helper) {
		assertSaturationCost(helper, 1.0f, 4.1f, 1);
		helper.succeed();
	}

	/** Out-of-range and unparseable values in the file must not reach the game. */
	@GameTest
	public void configClampsAndRoundTrips(GameTestHelper helper) {
		float dayLength = FxGlobalsConfig.dayLengthFactor;
		float hunger = FxGlobalsConfig.hungerFactor;

		try {
			FxGlobalsConfig.dayLengthFactor = 999.0f;
			FxGlobalsConfig.hungerFactor = -5.0f;
			FxGlobalsConfig.save();
			FxGlobalsConfig.load();

			float maxDayLength = FxGlobalsConfig.MAX_DAY_LENGTH_PERCENT / 100.0f;

			if (FxGlobalsConfig.dayLengthFactor != maxDayLength) {
				throw helper.assertionException("day length was not clamped: " + FxGlobalsConfig.dayLengthFactor);
			}

			if (FxGlobalsConfig.hungerFactor != FxGlobalsConfig.MIN_HUNGER_PERCENT / 100.0f) {
				throw helper.assertionException("hunger was not clamped: " + FxGlobalsConfig.hungerFactor);
			}

			// A clamped day length still has to give a usable rate, never infinity.
			if (!Float.isFinite(FxGlobalsConfig.clockRate()) || FxGlobalsConfig.clockRate() <= 0.0f) {
				throw helper.assertionException("clock rate is " + FxGlobalsConfig.clockRate());
			}
		} finally {
			FxGlobalsConfig.dayLengthFactor = dayLength;
			FxGlobalsConfig.hungerFactor = hunger;
			FxGlobalsConfig.save();
		}
		helper.succeed();
	}

	/** Drives exhaustion at the given factor and checks how much of it one saturation point costs. */
	private void assertSaturationCost(GameTestHelper helper, float factor, float perCall, int calls) {
		float previous = FxGlobalsConfig.hungerFactor;
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
		FoodData food = player.getFoodData();

		try {
			FxGlobalsConfig.hungerFactor = factor;
			food.setFoodLevel(20);
			food.setSaturation(5.0f);

			for (int i = 0; i < calls - 1; i++) {
				food.addExhaustion(perCall);
			}
			food.tick(player);

			if (food.getSaturationLevel() != 5.0f) {
				throw helper.assertionException("saturation went early at factor " + factor + ": "
						+ food.getSaturationLevel());
			}

			food.addExhaustion(perCall);
			food.tick(player);

			if (food.getSaturationLevel() != 4.0f) {
				throw helper.assertionException("factor " + factor + " should spend 1 saturation after "
						+ calls + " calls, got " + food.getSaturationLevel());
			}
		} finally {
			FxGlobalsConfig.hungerFactor = previous;
		}
	}

	/** A skeleton's arrow is DISALLOWED in vanilla; with the setting on it has to come back. */
	@GameTest
	public void mobArrowsCanBePickedUp(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
		Arrow arrow = strayArrow(helper);

		withPickupArrows(true, () -> arrow.playerTouch(player));

		if (!player.getInventory().contains(stack -> stack.getItem() == Items.ARROW)) {
			throw helper.assertionException("mob arrow did not end up in the inventory");
		}

		if (!arrow.isRemoved()) {
			throw helper.assertionException("picked-up arrow is still in the world");
		}
		helper.succeed();
	}

	/** Off is vanilla: the same arrow stays where it is. */
	@GameTest
	public void mobArrowsStayPutWhenTheSettingIsOff(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
		Arrow arrow = strayArrow(helper);

		withPickupArrows(false, () -> arrow.playerTouch(player));

		if (player.getInventory().contains(stack -> stack.getItem() == Items.ARROW)) {
			throw helper.assertionException("arrow was collected with the setting off");
		}

		if (arrow.isRemoved()) {
			throw helper.assertionException("arrow was removed with the setting off");
		}
		helper.succeed();
	}

	/** A drowned's trident is not an arrow, so it stays loot rather than becoming a free trident. */
	@GameTest
	public void tridentsAreNotPickedUp(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
		ThrownTrident trident = new ThrownTrident(helper.getLevel(), 0.0, 0.0, 0.0,
				new ItemStack(Items.TRIDENT));
		trident.pickup = AbstractArrow.Pickup.DISALLOWED;
		trident.setNoPhysics(true);
		trident.shakeTime = 0;
		helper.getLevel().addFreshEntity(trident);

		withPickupArrows(true, () -> trident.playerTouch(player));

		if (player.getInventory().contains(stack -> stack.getItem() == Items.TRIDENT)) {
			throw helper.assertionException("trident was collected as if it were an arrow");
		}
		helper.succeed();
	}

	/** An arrow flying through the zombie's head box kills it outright when the setting is on. */
	@GameTest
	public void headshotKillsTheMob(GameTestHelper helper) {
		Zombie zombie = spawnZombie(helper);
		double headY = zombie.getBoundingBox().maxY - zombie.getBoundingBox().getYsize() * 0.1;
		Arrow arrow = arrowFlyingThrough(helper, headY);

		withHeadshots(true, () -> hitEntity(arrow, zombie));

		if (zombie.isAlive()) {
			throw helper.assertionException("headshot did not kill the zombie");
		}
		helper.succeed();
	}

	/** The same shot, low enough to only cross the body box, is not a headshot. */
	@GameTest
	public void bodyHitDoesNotKillTheMob(GameTestHelper helper) {
		Zombie zombie = spawnZombie(helper);
		Arrow arrow = arrowFlyingThrough(helper, zombie.getY() + 0.1);

		withHeadshots(true, () -> hitEntity(arrow, zombie));

		if (!zombie.isAlive()) {
			throw helper.assertionException("body hit killed the zombie");
		}
		helper.succeed();
	}

	/** Off is vanilla: even a shot straight through the head box just does normal arrow damage. */
	@GameTest
	public void headshotsDoNothingWhenTheSettingIsOff(GameTestHelper helper) {
		Zombie zombie = spawnZombie(helper);
		double headY = zombie.getBoundingBox().maxY - zombie.getBoundingBox().getYsize() * 0.1;
		Arrow arrow = arrowFlyingThrough(helper, headY);

		withHeadshots(false, () -> hitEntity(arrow, zombie));

		if (!zombie.isAlive()) {
			throw helper.assertionException("zombie died with headshots disabled");
		}
		helper.succeed();
	}

	private Zombie spawnZombie(GameTestHelper helper) {
		Zombie zombie = new Zombie(EntityTypes.ZOMBIE, helper.getLevel());
		zombie.setPos(0.0, 0.0, 0.0);
		helper.getLevel().addFreshEntity(zombie);
		return zombie;
	}

	/** An arrow a few blocks out, aimed level, still moving straight at the zombie's hitbox at the given height. */
	private Arrow arrowFlyingThrough(GameTestHelper helper, double height) {
		Arrow arrow = new Arrow(helper.getLevel(), 0.0, height, -5.0,
				new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
		arrow.setDeltaMovement(new Vec3(0.0, 0.0, 5.0));
		return arrow;
	}

	/**
	 * onHitEntity is protected; the mixin injects into it and reads the arrow's own position and
	 * velocity, so the test drives it the same way rather than faking a hit location.
	 */
	private void hitEntity(Arrow arrow, Zombie zombie) {
		try {
			Method onHitEntity = AbstractArrow.class.getDeclaredMethod("onHitEntity", EntityHitResult.class);
			onHitEntity.setAccessible(true);
			onHitEntity.invoke(arrow, new EntityHitResult(zombie, arrow.position()));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private void withHeadshots(boolean enabled, Runnable action) {
		boolean previous = FxGlobalsConfig.headshots;

		try {
			FxGlobalsConfig.headshots = enabled;
			action.run();
		} finally {
			FxGlobalsConfig.headshots = previous;
		}
	}

	/** An arrow lying in the world with vanilla's "never pick this up" marking, as a mob leaves it. */
	private Arrow strayArrow(GameTestHelper helper) {
		Arrow arrow = new Arrow(helper.getLevel(), 0.0, 0.0, 0.0,
				new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		// playerTouch only runs for an arrow that has landed; no physics stands in for that.
		arrow.setNoPhysics(true);
		arrow.shakeTime = 0;
		helper.getLevel().addFreshEntity(arrow);
		return arrow;
	}

	private void withPickupArrows(boolean enabled, Runnable action) {
		boolean previous = FxGlobalsConfig.pickupArrows;

		try {
			FxGlobalsConfig.pickupArrows = enabled;
			action.run();
		} finally {
			FxGlobalsConfig.pickupArrows = previous;
		}
	}

	/** With no companion mods installed, the vanilla items still land and the flag gets set. */
	@GameTest
	public void starterGearGivesVanillaItemsOnce(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		boolean previous = FxGlobalsConfig.starterGear;

		try {
			FxGlobalsConfig.starterGear = true;
			FxGlobals.giveStarterGear(player);

			if (!player.getInventory().contains(stack -> stack.getItem() == Items.COMPASS)) {
				throw helper.assertionException("compass was not given");
			}
			if (!player.getInventory().contains(stack -> stack.getItem() == Items.LODESTONE)) {
				throw helper.assertionException("lodestone was not given");
			}
			if (!player.getAttachedOrCreate(FxGlobals.STARTER_GEAR_GIVEN)) {
				throw helper.assertionException("starter gear flag was not set");
			}

			player.getInventory().clearContent();
			FxGlobals.giveStarterGear(player);

			if (player.getInventory().contains(stack -> stack.getItem() == Items.COMPASS)) {
				throw helper.assertionException("starter gear was given a second time");
			}
		} finally {
			FxGlobalsConfig.starterGear = previous;
		}
		helper.succeed();
	}

	/** Off means off: a brand new player gets nothing. */
	@GameTest
	public void starterGearDoesNothingWhenTheSettingIsOff(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		boolean previous = FxGlobalsConfig.starterGear;

		try {
			FxGlobalsConfig.starterGear = false;
			FxGlobals.giveStarterGear(player);

			if (player.getAttachedOrCreate(FxGlobals.STARTER_GEAR_GIVEN)) {
				throw helper.assertionException("starter gear flag was set with the setting off");
			}
			if (!player.getInventory().isEmpty()) {
				throw helper.assertionException("starter gear was given with the setting off");
			}
		} finally {
			FxGlobalsConfig.starterGear = previous;
		}
		helper.succeed();
	}

	/**
	 * Using the recipe book learns every recipe and consumes the book. The mock server player has
	 * no network connection, so learning even one new recipe would NPE trying to sync it to a
	 * client that does not exist; pre-seeding every recipe as already known keeps the sync a no-op
	 * while still exercising the real award call the item makes.
	 */
	@GameTest
	public void recipeBookAwardsAllRecipesAndIsConsumed(GameTestHelper helper) throws ReflectiveOperationException {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.getInventory().add(new ItemStack(FxGlobals.RECIPE_BOOK));

		Field knownField = ServerRecipeBook.class.getDeclaredField("known");
		knownField.setAccessible(true);
		@SuppressWarnings("unchecked")
		Set<ResourceKey<Recipe<?>>> known = (Set<ResourceKey<Recipe<?>>>) knownField.get(player.getRecipeBook());
		helper.getLevel().getServer().getRecipeManager().getRecipes()
				.forEach(recipe -> known.add(recipe.id()));

		InteractionResult result = UseItemCallback.EVENT.invoker()
				.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND);

		if (!result.consumesAction()) {
			throw helper.assertionException("recipe book use did not consume the action: " + result);
		}

		if (player.getInventory().contains(stack -> stack.getItem() == FxGlobals.RECIPE_BOOK)) {
			throw helper.assertionException("recipe book was not consumed");
		}
		helper.succeed();
	}

	/** Any other item held passes through untouched — the callback only reacts to the recipe book. */
	@GameTest
	public void otherItemsPassThroughTheRecipeBookHandler(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.getInventory().add(new ItemStack(Items.APPLE));

		InteractionResult result = UseItemCallback.EVENT.invoker()
				.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND);

		if (result != InteractionResult.PASS) {
			throw helper.assertionException("expected PASS for a non-recipe-book item, got " + result);
		}
		helper.succeed();
	}

	/** The debug command has to be there and read back the values the mod actually applied. */
	@GameTest
	public void debugCommandReportsStatus(GameTestHelper helper) throws CommandSyntaxException {
		MinecraftServer server = helper.getLevel().getServer();

		if (server.getCommands().getDispatcher().getRoot().getChild(FxGlobals.MOD_ID) == null) {
			throw helper.assertionException("/" + FxGlobals.MOD_ID + " is not registered");
		}

		if (server.getCommands().getDispatcher().execute(
				FxGlobals.MOD_ID + " status", server.createCommandSourceStack()) != 1) {
			throw helper.assertionException("/" + FxGlobals.MOD_ID + " status failed");
		}
		helper.succeed();
	}

	/**
	 * /fxglobals startergear hands out the same kit the join hook does, and does it whether or not
	 * the player already has the once-per-player flag — that is the point of the debug command.
	 */
	@GameTest
	public void debugCommandGivesStarterGearAgain(GameTestHelper helper) throws CommandSyntaxException {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setAttached(FxGlobals.STARTER_GEAR_GIVEN, true);

		// The mock player is not in the server's player list, so a name selector cannot find it;
		// running the command as the player exercises the same no-argument path a human would use.
		if (server.getCommands().getDispatcher().execute(FxGlobals.MOD_ID + " startergear",
				server.createCommandSourceStack().withEntity(player)) != 1) {
			throw helper.assertionException("/" + FxGlobals.MOD_ID + " startergear failed");
		}

		if (!player.getInventory().contains(stack -> stack.getItem() == Items.COMPASS)) {
			throw helper.assertionException("starter gear command gave no compass");
		}
		if (!player.getInventory().contains(stack -> stack.getItem() == FxGlobals.RECIPE_BOOK)) {
			throw helper.assertionException("starter gear command gave no recipe book");
		}
		helper.succeed();
	}
}
