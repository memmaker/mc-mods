package com.explorercraft.fxglobals.gametest;

import com.explorercraft.fxglobals.FxGlobals;
import com.explorercraft.fxglobals.FxGlobalsConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
}
