package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.entity.AirshipEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class CargoAirshipEntityRenderer<T extends AirshipEntity> extends AirshipEntityRenderer<T> {
    private static final Identifier ID = Main.locate("cargo_airship");

    protected Identifier getModelId() {
        return ID;
    }

    public CargoAirshipEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.8f;
    }
}
