package dev.explorercraft.crafttracker;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CraftTracker {
    public static final String MOD_ID = "crafttracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final CraftQueue QUEUE = new CraftQueue();

    private CraftTracker() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
