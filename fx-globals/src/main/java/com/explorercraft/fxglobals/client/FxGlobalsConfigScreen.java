package com.explorercraft.fxglobals.client;

import com.explorercraft.fxglobals.FxGlobals;
import com.explorercraft.fxglobals.FxGlobalsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * The Mod Menu config screen, built from vanilla's own options widgets — no config-screen
 * library, and it looks like every other options page. Both factors are edited as whole
 * percent, which is what a slider can represent exactly.
 */
public class FxGlobalsConfigScreen extends OptionsSubScreen {
	private final OptionInstance<Integer> dayLength = new OptionInstance<>(
			"options.fxglobals.day_length",
			OptionInstance.cachedConstantTooltip(Component.translatable("options.fxglobals.day_length.tooltip")),
			(caption, value) -> Component.translatable("options.fxglobals.percent", caption, value),
			new OptionInstance.IntRange(FxGlobalsConfig.MIN_DAY_LENGTH_PERCENT, FxGlobalsConfig.MAX_DAY_LENGTH_PERCENT),
			FxGlobalsConfig.percent(FxGlobalsConfig.dayLengthFactor),
			value -> {
				FxGlobalsConfig.dayLengthFactor = value / 100.0f;
				// Single-player: the integrated server is already running, so this takes effect now.
				FxGlobals.applyClockRate();
			});

	private final OptionInstance<Integer> hunger = new OptionInstance<>(
			"options.fxglobals.hunger",
			OptionInstance.cachedConstantTooltip(Component.translatable("options.fxglobals.hunger.tooltip")),
			(caption, value) -> Component.translatable("options.fxglobals.percent", caption, value),
			new OptionInstance.IntRange(FxGlobalsConfig.MIN_HUNGER_PERCENT, FxGlobalsConfig.MAX_HUNGER_PERCENT),
			FxGlobalsConfig.percent(FxGlobalsConfig.hungerFactor),
			value -> FxGlobalsConfig.hungerFactor = value / 100.0f);

	private final OptionInstance<Boolean> pickupArrows = OptionInstance.createBoolean(
			"options.fxglobals.pickup_arrows",
			OptionInstance.cachedConstantTooltip(Component.translatable("options.fxglobals.pickup_arrows.tooltip")),
			FxGlobalsConfig.pickupArrows,
			value -> FxGlobalsConfig.pickupArrows = value);

	private final OptionInstance<Boolean> headshots = OptionInstance.createBoolean(
			"options.fxglobals.headshots",
			OptionInstance.cachedConstantTooltip(Component.translatable("options.fxglobals.headshots.tooltip")),
			FxGlobalsConfig.headshots,
			value -> FxGlobalsConfig.headshots = value);

	private final OptionInstance<Boolean> starterGear = OptionInstance.createBoolean(
			"options.fxglobals.starter_gear",
			OptionInstance.cachedConstantTooltip(Component.translatable("options.fxglobals.starter_gear.tooltip")),
			FxGlobalsConfig.starterGear,
			value -> FxGlobalsConfig.starterGear = value);

	public FxGlobalsConfigScreen(Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("options.fxglobals.title"));
	}

	@Override
	protected void addOptions() {
		this.list.addBig(this.dayLength);
		this.list.addBig(this.hunger);
		this.list.addBig(this.pickupArrows);
		this.list.addBig(this.headshots);
		this.list.addBig(this.starterGear);
	}

	@Override
	public void removed() {
		FxGlobalsConfig.save();
		super.removed();
	}
}
