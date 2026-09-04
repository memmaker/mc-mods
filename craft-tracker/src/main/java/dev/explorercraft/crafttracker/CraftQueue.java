package dev.explorercraft.crafttracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/// The tracked list: item -> how many the player wants. Insertion ordered, client side only.
///
/// ponytail: one file for all worlds, not one per save. Split it when someone actually keeps
/// parallel worlds and complains that the lists bleed into each other.
public final class CraftQueue {
    private static final Gson GSON = new Gson();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("crafttracker.json");

    private final Map<Item, Integer> wanted = new LinkedHashMap<>();

    public Map<Item, Integer> entries() {
        return wanted;
    }

    /// Adds to the wanted count, removing the entry once it drops to zero or below.
    public void add(Item item, int amount) {
        int total = wanted.getOrDefault(item, 0) + amount;
        if (total <= 0) {
            wanted.remove(item);
        } else {
            wanted.put(item, total);
        }
    }

    public void remove(Item item) {
        wanted.remove(item);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        wanted.forEach((item, count) -> json.add(BuiltInRegistries.ITEM.getKey(item).toString(), new JsonPrimitive(count)));
        return json;
    }

    /// Unknown or malformed ids are dropped: the file survives a mod being uninstalled.
    public void fromJson(JsonObject json) {
        wanted.clear();
        for (String key : json.keySet()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
            int count = json.get(key).getAsInt();
            if (count > 0) wanted.put(BuiltInRegistries.ITEM.getValue(id), count);
        }
    }

    public void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            fromJson(GSON.fromJson(reader, JsonObject.class));
        } catch (IOException | RuntimeException e) {
            CraftTracker.LOGGER.warn("Could not read {}, starting with an empty queue", FILE, e);
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(toJson(), writer);
        } catch (IOException e) {
            CraftTracker.LOGGER.warn("Could not write {}", FILE, e);
        }
    }
}
