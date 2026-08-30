package com.explorercraft.seamlesscrafting.client;

import com.explorercraft.seamlesscrafting.SeamlessCraftingConfig;
import com.explorercraft.seamlesscrafting.SeamlessCraftingConfig.ConfigData;
import com.explorercraft.seamlesscrafting.SeamlessCraftingConfig.LocateTrailParticle;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.util.ARGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/** Built out of vanilla option widgets, so it looks and behaves like the game's own settings. */
public class SeamlessCraftingOptionsScreen extends OptionsSubScreen {
	private static final Codec<LocateTrailParticle> PARTICLE_CODEC =
			Codec.INT.xmap(ordinal -> LocateTrailParticle.values()[ordinal], LocateTrailParticle::ordinal);

	private final ConfigData config = SeamlessCraftingConfig.snapshot();

	private final OptionInstance<Integer> highlightRed = colorChannel("red", ARGB.red(this.config.highlightColor));
	private final OptionInstance<Integer> highlightGreen = colorChannel("green", ARGB.green(this.config.highlightColor));
	private final OptionInstance<Integer> highlightBlue = colorChannel("blue", ARGB.blue(this.config.highlightColor));

	private final OptionInstance<Integer> highlightSeconds = slider(
			"highlight_duration", 1, 60, this.config.highlightDurationTicks / 20, value -> this.config.highlightDurationTicks = value * 20);
	private final OptionInstance<Integer> nearbyRadius = slider(
			"nearby_radius", 1, 64, this.config.nearbyRadius, value -> this.config.nearbyRadius = value);
	private final OptionInstance<Integer> highlightOpacity = slider(
			"highlight_opacity", 5, 100, this.config.highlightOpacityPercent, value -> this.config.highlightOpacityPercent = value);
	private final OptionInstance<Integer> autoRefreshSeconds = slider(
			"auto_refresh", 1, 30, Math.max(1, this.config.autoRefreshTicks / 20), value -> this.config.autoRefreshTicks = value * 20);

	private final OptionInstance<Boolean> showHighlighter = toggle(
			"show_highlighter", this.config.showHighlighter, value -> this.config.showHighlighter = value);
	private final OptionInstance<Boolean> showDistanceLabel = toggle(
			"distance_label", this.config.showDistanceLabel, value -> this.config.showDistanceLabel = value);
	private final OptionInstance<Boolean> snapAim = toggle(
			"snap_aim", this.config.snapAimToChest, value -> this.config.snapAimToChest = value);
	private final OptionInstance<Boolean> locateTrail = toggle(
			"locate_trail", this.config.showLocateTrail, value -> this.config.showLocateTrail = value);
	private final OptionInstance<Boolean> panelOpenByDefault = toggle(
			"panel_open", this.config.nearbyPanelOpenByDefault, value -> this.config.nearbyPanelOpenByDefault = value);

	private final OptionInstance<LocateTrailParticle> trailParticle = new OptionInstance<>(
			"options.seamlesscrafting.trail_particle",
			OptionInstance.noTooltip(),
			(caption, value) -> Component.translatable("options.seamlesscrafting.trail_particle." + value.name().toLowerCase(java.util.Locale.ROOT)),
			new OptionInstance.Enum<>(List.of(LocateTrailParticle.values()), PARTICLE_CODEC),
			this.config.locateTrailParticle,
			value -> this.config.locateTrailParticle = value
	);

	public SeamlessCraftingOptionsScreen(Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("options.seamlesscrafting.title"));
	}

	@Override
	protected void addOptions() {
		OptionsList list = this.list;
		list.addSmall(this.showHighlighter, this.showDistanceLabel);
		list.addSmall(this.snapAim, this.locateTrail);
		list.addSmall(this.panelOpenByDefault, this.trailParticle);
		list.addSmall(this.nearbyRadius, this.autoRefreshSeconds);
		list.addSmall(this.highlightSeconds, this.highlightOpacity);
		list.addSmall(this.highlightRed, this.highlightGreen);
		list.addSmall(this.highlightBlue);
	}

	@Override
	public void onClose() {
		this.config.highlightColor = ARGB.color(0, this.highlightRed.get(), this.highlightGreen.get(), this.highlightBlue.get()) & 0xFFFFFF;
		SeamlessCraftingConfig.update(this.config);
		NearbyItemsClientState.requestUpdate();
		super.onClose();
	}

	private static OptionInstance<Integer> colorChannel(String channel, int initialValue) {
		return new OptionInstance<>(
				"options.seamlesscrafting.highlight_" + channel,
				OptionInstance.noTooltip(),
				(caption, value) -> Component.translatable("options.generic_value", caption, value),
				new OptionInstance.IntRange(0, 255),
				initialValue,
				value -> {
				}
		);
	}

	private static OptionInstance<Integer> slider(String key, int min, int max, int initialValue, java.util.function.IntConsumer onChange) {
		return new OptionInstance<>(
				"options.seamlesscrafting." + key,
				OptionInstance.noTooltip(),
				(caption, value) -> Component.translatable("options.generic_value", caption, value),
				new OptionInstance.IntRange(min, max),
				Math.max(min, Math.min(max, initialValue)),
				onChange::accept
		);
	}

	private static OptionInstance<Boolean> toggle(String key, boolean initialValue, java.util.function.Consumer<Boolean> onChange) {
		return OptionInstance.createBoolean("options.seamlesscrafting." + key, initialValue, onChange::accept);
	}
}
