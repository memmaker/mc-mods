package dev.explorercraft.immersiveaircraft.network;

import dev.explorercraft.immersiveaircraft.client.gui.VehicleScreen;
import dev.explorercraft.immersiveaircraft.entity.InventoryVehicleEntity;
import dev.explorercraft.immersiveaircraft.network.s2c.FireResponse;
import dev.explorercraft.immersiveaircraft.network.s2c.InventoryUpdateMessage;
import dev.explorercraft.immersiveaircraft.network.s2c.OpenGuiRequest;
import dev.explorercraft.immersiveaircraft.screen.VehicleScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public class ClientNetworkManager implements NetworkManager {
    @Override
    public void handleOpenGuiRequest(OpenGuiRequest message) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null) {
            InventoryVehicleEntity vehicle = (InventoryVehicleEntity) client.level.getEntity(message.getVehicle());
            if (vehicle != null) {
                VehicleScreenHandler handler = (VehicleScreenHandler) vehicle.createMenu(message.getSyncId(), client.player.getInventory(), client.player);
                if (handler != null) {
                    VehicleScreen screen = new VehicleScreen(handler, client.player.getInventory(), vehicle.getDisplayName());
                    client.player.containerMenu = screen.getMenu();
                    client.gui.setScreen(screen);
                }
            }
        }
    }

    @Override
    public void handleInventoryUpdate(InventoryUpdateMessage message) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null) {
            InventoryVehicleEntity vehicle = (InventoryVehicleEntity) client.level.getEntity(message.getVehicle());
            if (vehicle != null) {
                vehicle.getInventory().setItem(message.getIndex(), message.getStack());
            }
        }
    }

    @Override
    public void handleFire(FireResponse fireResponse) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level != null) {
            // Particles
            RandomSource random = level.getRandom();
            double r = 0.1;
            for (int t = 0; t < 2; ++t) {
                for (int i = 0; i < 5; ++i) {
                    level.addParticle(t == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE,
                            fireResponse.x, fireResponse.y, fireResponse.z,
                            fireResponse.vx + (random.nextDouble() - 0.5) * r,
                            fireResponse.vy + (random.nextDouble() - 0.5) * r,
                            fireResponse.vz + (random.nextDouble() - 0.5) * r
                    );
                }
            }
        }
    }
}
