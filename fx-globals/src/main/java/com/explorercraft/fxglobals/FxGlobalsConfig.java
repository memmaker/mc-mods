package com.explorercraft.fxglobals;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The two numbers this mod is made of, in config/fxglobals.properties. Hand-edited files and
 * config screens both land here, so both go through the same clamping.
 */
public final class FxGlobalsConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(FxGlobalsConfig.class);
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fxglobals.properties");

	public static final float DEFAULT_DAY_LENGTH_FACTOR = 1.5f;
	public static final float DEFAULT_HUNGER_FACTOR = 0.25f;
	public static final boolean DEFAULT_PICKUP_ARROWS = true;

	/** Slider and clamp bounds, in percent. A day length of 0 would divide by zero, hence the floor. */
	public static final int MIN_DAY_LENGTH_PERCENT = 25;
	public static final int MAX_DAY_LENGTH_PERCENT = 800;
	public static final int MIN_HUNGER_PERCENT = 0;
	public static final int MAX_HUNGER_PERCENT = 400;

	/** One day-night cycle lasts this many times longer than vanilla. */
	public static float dayLengthFactor = DEFAULT_DAY_LENGTH_FACTOR;

	/** Exhaustion — the only thing that drains the hunger bar — is scaled by this. */
	public static float hungerFactor = DEFAULT_HUNGER_FACTOR;

	/** Whether arrows a mob shot can be picked up, the way a player's own arrows already can. */
	public static boolean pickupArrows = DEFAULT_PICKUP_ARROWS;

	private FxGlobalsConfig() {
	}

	public static void load() {
		Properties properties = new Properties();

		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				properties.load(reader);
			} catch (IOException e) {
				LOGGER.warn("Could not read {}, falling back to defaults", FILE, e);
			}
		}

		dayLengthFactor = read(properties, "day_length_factor", DEFAULT_DAY_LENGTH_FACTOR,
				MIN_DAY_LENGTH_PERCENT, MAX_DAY_LENGTH_PERCENT);
		hungerFactor = read(properties, "hunger_factor", DEFAULT_HUNGER_FACTOR,
				MIN_HUNGER_PERCENT, MAX_HUNGER_PERCENT);
		pickupArrows = readBoolean(properties, "pickup_arrows", DEFAULT_PICKUP_ARROWS);

		// Writes the file on first run, and normalises anything that was out of range.
		save();
	}

	public static void save() {
		Properties properties = new Properties();
		properties.setProperty("day_length_factor", Float.toString(dayLengthFactor));
		properties.setProperty("hunger_factor", Float.toString(hungerFactor));
		properties.setProperty("pickup_arrows", Boolean.toString(pickupArrows));

		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				properties.store(writer, "FX Globals — 1.0 is vanilla for both factors");
			}
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", FILE, e);
		}
	}

	/** The overworld clock advances this much per tick, so a day takes dayLengthFactor as long. */
	public static float clockRate() {
		return 1.0f / dayLengthFactor;
	}

	public static int percent(float factor) {
		return Math.round(factor * 100.0f);
	}

	private static boolean readBoolean(Properties properties, String key, boolean fallback) {
		String raw = properties.getProperty(key);

		if (raw == null) {
			return fallback;
		}

		// Boolean.parseBoolean would silently read a typo as false, which is a config that lies.
		String value = raw.trim();

		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
			return Boolean.parseBoolean(value);
		}

		LOGGER.warn("{} in {} is not true or false ({}), using {}", key, FILE, raw, fallback);
		return fallback;
	}

	private static float read(Properties properties, String key, float fallback, int minPercent, int maxPercent) {
		String raw = properties.getProperty(key);

		if (raw == null) {
			return fallback;
		}

		try {
			return Math.clamp(Float.parseFloat(raw.trim()), minPercent / 100.0f, maxPercent / 100.0f);
		} catch (NumberFormatException e) {
			LOGGER.warn("{} in {} is not a number ({}), using {}", key, FILE, raw, fallback);
			return fallback;
		}
	}
}
