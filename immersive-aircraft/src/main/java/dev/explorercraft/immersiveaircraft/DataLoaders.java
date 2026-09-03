package dev.explorercraft.immersiveaircraft;

import dev.explorercraft.immersiveaircraft.cobalt.registration.Registration;
import dev.explorercraft.immersiveaircraft.data.VehicleDataLoader;
import dev.explorercraft.immersiveaircraft.data.UpgradeDataLoader;
import dev.explorercraft.immersiveaircraft.resources.BBModelLoader;

public class DataLoaders {
    public static void bootstrap() {
        // nop
    }

    static {
        Registration.registerDataLoader("aircraft_upgrades", new UpgradeDataLoader());
        Registration.registerDataLoader("aircraft", new VehicleDataLoader());

        Registration.registerResourceLoader("objects_bbmodel", new BBModelLoader());
    }
}
