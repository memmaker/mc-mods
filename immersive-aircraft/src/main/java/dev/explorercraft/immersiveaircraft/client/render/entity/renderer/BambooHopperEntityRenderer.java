package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import dev.explorercraft.immersiveaircraft.entity.AircraftEntity;
import dev.explorercraft.immersiveaircraft.entity.BambooHopperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class BambooHopperEntityRenderer<T extends BambooHopperEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("bamboo_hopper");

    protected Identifier getModelId() {
        return ID;
    }

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<>();

    public BambooHopperEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 2.0f;
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }
}
