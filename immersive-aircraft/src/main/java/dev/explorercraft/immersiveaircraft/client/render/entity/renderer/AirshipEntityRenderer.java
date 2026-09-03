package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import dev.explorercraft.immersiveaircraft.entity.AircraftEntity;
import dev.explorercraft.immersiveaircraft.entity.AirshipEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class AirshipEntityRenderer<T extends AirshipEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("airship");

    protected Identifier getModelId() {
        return ID;
    }

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<T>()
            .add("banners", this::renderBanners)
            .add("colored", (model, object, submitNodeCollector, entity, matrixStack, light, time, modelPartRenderer) ->
                    renderDyed(model, object, submitNodeCollector, entity, matrixStack, light, time, false, true))
            .add("uncolored", (model, object, submitNodeCollector, entity, matrixStack, light, time, modelPartRenderer) ->
                    renderUndyed(model, object, submitNodeCollector, entity, matrixStack, light, time))
            .add("flag", (model, object, submitNodeCollector, entity, matrixStack, light, time, modelPartRenderer) ->
                    renderSails(object, submitNodeCollector, entity, matrixStack, light, time))
            .add("flag_small", (model, object, submitNodeCollector, entity, matrixStack, light, time, modelPartRenderer) ->
                    renderSails(object, submitNodeCollector, entity, matrixStack, light, time))
            .add("flag_front", (model, object, submitNodeCollector, entity, matrixStack, light, time, modelPartRenderer) ->
                    renderSails(object, submitNodeCollector, entity, matrixStack, light, time));


    public AirshipEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.8f;
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }
}
