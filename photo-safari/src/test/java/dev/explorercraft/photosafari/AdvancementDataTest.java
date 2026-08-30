package dev.explorercraft.photosafari;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Keeps the shipped advancements pointing at the trigger, with the field names it reads.
/// ponytail: plain JSON checks. Booting the whole game to run the real codec costs more
/// than it catches, the game itself validates the rest on world load.
class AdvancementDataTest {
    private static final Path ADVANCEMENTS = Path.of("src/main/generated/data/photosafari/advancement");
    private static final String TRIGGER = "photosafari:species_photographed";

    @Test
    void everyAdvancementUsesTheTriggerCorrectly() throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(ADVANCEMENTS)) {
            files = stream.filter(path -> path.toString().endsWith(".json")).toList();
        }

        assertTrue(files.size() > 80, "expected the generated species checklist, found " + files.size() + " files");

        for (Path file : files) {
            JsonObject advancement;
            try (Reader reader = Files.newBufferedReader(file)) {
                advancement = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonObject criteria = advancement.getAsJsonObject("criteria");
            assertFalse(criteria.keySet().isEmpty(), file + " has no criteria");

            for (String name : criteria.keySet()) {
                JsonObject criterion = criteria.getAsJsonObject(name);
                if (!TRIGGER.equals(criterion.get("trigger").getAsString())) {
                    continue;
                }

                // Datagen leaves out conditions that are all defaults, which means "any species".
                JsonObject conditions = criterion.getAsJsonObject("conditions");
                if (conditions == null) {
                    continue;
                }

                if (conditions.has("min_species")) {
                    assertTrue(conditions.get("min_species").getAsInt() > 0, file + " has a useless threshold");
                }

                if (conditions.has("species")) {
                    assertTrue(conditions.get("species").getAsString().contains(":"),
                            file + " species is not a namespaced id");
                }
            }
        }
    }
}
