package dev.explorercraft.photosafari;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/// Camera behaviour toggles, in config/photosafari.properties. Hand-edited files and the
/// Mod Menu screen both land here.
public final class PhotoSafariConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhotoSafariConfig.class);
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("photosafari.properties");

    public static final boolean DEFAULT_PEACEFUL_LOOT = true;

    /// Whether the camera's loot mode is reachable at all. Off leaves only photograph mode,
    /// and is also enforced server-side so a modified client can't re-enable it.
    public static boolean peacefulLoot = DEFAULT_PEACEFUL_LOOT;

    private PhotoSafariConfig() {
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

        peacefulLoot = readBoolean(properties, "peaceful_loot", DEFAULT_PEACEFUL_LOOT);

        // Writes the file on first run.
        save();
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("peaceful_loot", Boolean.toString(peacefulLoot));

        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                properties.store(writer, "Photo Safari");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", FILE, e);
        }
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
}
