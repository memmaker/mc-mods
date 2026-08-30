package com.explorercraft.fxglobals;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.jetbrains.annotations.Nullable;

/**
 * World-wide pacing tweaks: longer days, slower hunger. No items, no recipes — the two
 * numbers in {@link FxGlobalsConfig} are the whole mod.
 */
public class FxGlobals implements ModInitializer {
	public static final String MOD_ID = "fxglobals";

	/** Held so a config change can re-apply the clock rate without a world reload. */
	@Nullable
	private static MinecraftServer server;

	@Override
	public void onInitialize() {
		FxGlobalsConfig.load();

		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
			server = startedServer;
			applyClockRate();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(stoppedServer -> server = null);
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommands(dispatcher));
	}

	/**
	 * Writes the configured rate onto the vanilla overworld clock — the same knob {@code /time rate}
	 * uses, so sleeping, daylight sensors and the in-game clock stay consistent. Safe to call any
	 * time; without a running server there is nothing to write to yet.
	 */
	public static void applyClockRate() {
		MinecraftServer running = server;

		if (running != null) {
			running.clockManager().setRate(overworldClock(running), FxGlobalsConfig.clockRate());
		}
	}

	/** /fxglobals status — the live values, so a tweak that did not take shows up straight away. */
	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> status(context.getSource()))
				.then(Commands.literal("status").executes(context -> status(context.getSource()))));
	}

	private static int status(CommandSourceStack source) {
		MinecraftServer running = source.getServer();
		ClockState clock = running.clockManager().packState().clocks().get(overworldClock(running));
		String text = "dayLength=%.2fx clockRate=%.4f hunger=%.2fx pickupArrows=%s clockTicks=%d".formatted(
				FxGlobalsConfig.dayLengthFactor, clock == null ? Float.NaN : clock.rate(),
				FxGlobalsConfig.hungerFactor, FxGlobalsConfig.pickupArrows,
				running.clockManager().getTotalTicks(overworldClock(running)));

		source.sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	public static Holder<WorldClock> overworldClock(MinecraftServer server) {
		return server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
	}
}
