package com.explorercraft.seamlesscrafting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class SeamlessCraftingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(SeamlessCrafting.MOD_ID + ".json");
	private static ConfigData data = ConfigData.defaults();

	private SeamlessCraftingConfig() {
	}

	public static void load() {
		ConfigData loaded = null;
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				loaded = GSON.fromJson(reader, ConfigData.class);
			} catch (IOException | JsonSyntaxException ignored) {
				loaded = null;
			}
		}

		data = sanitize(loaded);
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public static ConfigData snapshot() {
		return data.copy();
	}

	public static void update(ConfigData updated) {
		data = sanitize(updated);
		save();
	}

	public static int getHighlightColor() {
		return data.highlightColor;
	}

	public static int getHighlightDurationTicks() {
		return data.highlightDurationTicks;
	}

	public static int getNearbyRadius() {
		return data.nearbyRadius;
	}

	public static float getHighlightOpacity() {
		return data.highlightOpacityPercent / 100.0f;
	}

	public static boolean isDistanceLabelEnabled() {
		return data.showDistanceLabel;
	}

	public static boolean isHighlightEnabled() {
		return data.showHighlighter;
	}

	public static boolean isSnapAimEnabled() {
		return data.snapAimToChest;
	}

	public static boolean isLocateTrailEnabled() {
		return data.showLocateTrail;
	}

	public static LocateTrailParticle getLocateTrailParticle() {
		return data.locateTrailParticle;
	}

	public static boolean isNearbyPanelOpenByDefault() {
		return data.nearbyPanelOpenByDefault;
	}

	public static int getAutoRefreshTicks() {
		return data.autoRefreshTicks;
	}

	private static ConfigData sanitize(ConfigData source) {
		ConfigData sanitized = source == null ? ConfigData.defaults() : source.copy();
		if (sanitized.locateTrailParticle == null) {
			sanitized.locateTrailParticle = LocateTrailParticle.CLOUD;
		}
		sanitized.highlightColor = sanitized.highlightColor & 0xFFFFFF;
		sanitized.highlightDurationTicks = clamp(sanitized.highlightDurationTicks, 10, 20 * 60);
		sanitized.nearbyRadius = clamp(sanitized.nearbyRadius, 1, 64);
		sanitized.highlightOpacityPercent = clamp(sanitized.highlightOpacityPercent, 5, 100);
		sanitized.autoRefreshTicks = clamp(sanitized.autoRefreshTicks, 5, 20 * 30);
		return sanitized;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public static final class ConfigData {
		public int highlightColor;
		public int highlightDurationTicks;
		public int nearbyRadius;
		public int highlightOpacityPercent;
		public boolean showHighlighter;
		public boolean showDistanceLabel;
		public boolean snapAimToChest;
		public boolean showLocateTrail;
		public LocateTrailParticle locateTrailParticle;
		public boolean nearbyPanelOpenByDefault;
		public int autoRefreshTicks;

		public static ConfigData defaults() {
			ConfigData defaults = new ConfigData();
			defaults.highlightColor = 0xFFD700;
			defaults.highlightDurationTicks = 100;
			defaults.nearbyRadius = 16;
			defaults.highlightOpacityPercent = 35;
			defaults.showHighlighter = true;
			defaults.showDistanceLabel = true;
			defaults.snapAimToChest = false;
			defaults.showLocateTrail = true;
			defaults.locateTrailParticle = LocateTrailParticle.CLOUD;
			defaults.nearbyPanelOpenByDefault = true;
			defaults.autoRefreshTicks = 20;
			return defaults;
		}

		public ConfigData copy() {
			ConfigData copy = new ConfigData();
			copy.highlightColor = this.highlightColor;
			copy.highlightDurationTicks = this.highlightDurationTicks;
			copy.nearbyRadius = this.nearbyRadius;
			copy.highlightOpacityPercent = this.highlightOpacityPercent;
			copy.showHighlighter = this.showHighlighter;
			copy.showDistanceLabel = this.showDistanceLabel;
			copy.snapAimToChest = this.snapAimToChest;
			copy.showLocateTrail = this.showLocateTrail;
			copy.locateTrailParticle = this.locateTrailParticle;
			copy.nearbyPanelOpenByDefault = this.nearbyPanelOpenByDefault;
			copy.autoRefreshTicks = this.autoRefreshTicks;
			return copy;
		}
	}

	public enum LocateTrailParticle {
		CLOUD,
		SMOKE,
		END_ROD
	}
}
