package dev.explorercraft.immersiveaircraft.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.item.upgrade.VehicleUpgrade;
import dev.explorercraft.immersiveaircraft.item.upgrade.VehicleUpgradeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import java.util.Map;

public class UpgradeDataLoader extends DataLoader {
    public UpgradeDataLoader() {
        super("aircraft_upgrades");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsonMap, ResourceManager manager, ProfilerFiller profiler) {
        // Clear existing upgrade values
        VehicleUpgradeRegistry.INSTANCE.reset();

        jsonMap.forEach((identifier, jsonElement) -> {
            try {
                if (BuiltInRegistries.ITEM.containsKey(identifier)) {
                    Item item = BuiltInRegistries.ITEM.getValue(identifier);
                    VehicleUpgrade upgrade = getAircraftUpgrade(jsonElement.getAsJsonObject());
                    VehicleUpgradeRegistry.INSTANCE.setUpgrade(item, upgrade);
                } else {
                    Main.LOGGER.error("There is no item {} to make it an upgrade!", identifier);
                }
            } catch (IllegalArgumentException | JsonParseException exception) {
                Main.LOGGER.error("Parsing error on aircraft upgrade {}: {}", identifier, exception.getMessage());
            }
        });
    }
}