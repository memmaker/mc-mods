package dev.explorercraft.immersiveaircraft;

import dev.explorercraft.immersiveaircraft.cobalt.network.NetworkHandler;
import dev.explorercraft.immersiveaircraft.cobalt.network.NetworkHandlerImpl;
import dev.explorercraft.immersiveaircraft.cobalt.registration.CobaltFuelRegistryImpl;
import dev.explorercraft.immersiveaircraft.cobalt.registration.RegistrationImpl;
import dev.explorercraft.immersiveaircraft.network.s2c.AircraftDataMessage;
import dev.explorercraft.immersiveaircraft.network.s2c.VehicleUpgradesMessage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;

public final class ImmersiveAircraftMod implements ModInitializer {
    static {
        Main.MOD_LOADER = "fabric";

        new RegistrationImpl();
        new NetworkHandlerImpl();
        new CobaltFuelRegistryImpl();
    }

    @Override
    public void onInitialize() {
        dev.explorercraft.immersiveaircraft.util.ItemTagCompat.bootstrap();

        Items.bootstrap();
        Sounds.bootstrap();
        Entities.bootstrap();
        WeaponRegistry.bootstrap();
        DataLoaders.bootstrap();

        Messages.loadMessages();

        CreativeModeTab group = FabricCreativeModeTab.builder()
                .title(ItemGroups.getDisplayName())
                .icon(ItemGroups::getIcon)
                .displayItems((enabledFeatures, entries) -> entries.acceptAll(Items.getSortedItems()))
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Main.locate("group"), group);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(ImmersiveAircraftMod::onSyncDatapack);
    }

    private static void onSyncDatapack(ServerPlayer player, boolean joined) {
        NetworkHandler.sendToPlayer(new VehicleUpgradesMessage(), player);
        NetworkHandler.sendToPlayer(new AircraftDataMessage(), player);
    }
}
