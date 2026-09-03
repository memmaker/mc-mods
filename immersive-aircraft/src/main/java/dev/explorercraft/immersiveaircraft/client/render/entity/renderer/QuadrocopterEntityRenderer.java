package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import dev.explorercraft.immersiveaircraft.entity.AircraftEntity;
import dev.explorercraft.immersiveaircraft.entity.QuadrocopterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

import java.util.Random;

public class QuadrocopterEntityRenderer<T extends QuadrocopterEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("quadrocopter");

    protected Identifier getModelId() {
        return ID;
    }

    private final Random random = new Random();

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<T>()
            .add(
                    "engine",
                    (entity, yaw, tickDelta, matrixStack) -> {
                        double p = entity.enginePower.getSmooth() / 128.0;
                        matrixStack.translate((random.nextDouble() - 0.5) * p, (random.nextDouble() - 0.5) * p, (random.nextDouble() - 0.5) * p);
                    },
                    (model, object, submitNodeCollector, entity, matrixStack, light, time, modelPartRenderer) -> {
                        String engine = "engine_" + (entity.enginePower.getSmooth() > 0.01 ? entity.tickCount % 2 : 0);
                        renderOptionalObject(engine, model, submitNodeCollector, entity, matrixStack, light, time);
                    }
            );

    public QuadrocopterEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0.8f;
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }
}
