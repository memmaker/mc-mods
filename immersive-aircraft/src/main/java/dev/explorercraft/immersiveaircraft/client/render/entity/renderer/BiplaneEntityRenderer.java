package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import dev.explorercraft.immersiveaircraft.entity.AircraftEntity;
import dev.explorercraft.immersiveaircraft.entity.BiplaneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class BiplaneEntityRenderer<T extends BiplaneEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("biplane");

    protected Identifier getModelId() {
        return ID;
    }

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<T>()
            .add("banners", this::renderBanners);

    public BiplaneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0.8f;
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }
}
