package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.BBModelRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import dev.explorercraft.immersiveaircraft.resources.BBModelLoader;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBAnimationVariables;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBModel;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBObject;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public abstract class VehicleEntityRenderer<T extends VehicleEntity> extends EntityRenderer<T, VehicleRenderState<T>> {
    public VehicleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    protected abstract ModelPartRenderHandler<T> getModel(T entity);

    protected abstract Identifier getModelId();

    @Override
    public VehicleRenderState<T> createRenderState() {
        return new VehicleRenderState<>();
    }

    @Override
    public void extractRenderState(T entity, VehicleRenderState<T> state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entity = entity;
        state.tickDelta = partialTicks;
    }

    @Override
    public void submit(VehicleRenderState<T> state, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        T entity = state.entity;
        float tickDelta = state.tickDelta;
        float yaw = entity.getYRot(tickDelta);
        int light = state.lightCoords;

        matrixStack.pushPose();

        // Rotation
        matrixStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        matrixStack.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(tickDelta)));
        matrixStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll(tickDelta)));

        // Render model, weapons, etc.
        renderLocal(entity, yaw, tickDelta, matrixStack, submitNodeCollector, light);

        matrixStack.popPose();

        // Rendered in the un-rotated (world-aligned) pose, e.g. trails - matches upstream's use
        // of the pre-rotation "peek" pose rather than the vehicle-local one.
        renderWorldAligned(entity, tickDelta, matrixStack, submitNodeCollector, light);

        super.submit(state, matrixStack, submitNodeCollector, camera);
    }

    protected void renderWorldAligned(T entity, float tickDelta, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light) {
        // no-op by default; overridden by AircraftEntityRenderer to render trails.
    }

    public void renderLocal(T entity, float yaw, float tickDelta, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light) {
        // Wobble
        float h = (float) entity.getDamageWobbleTicks() - tickDelta;
        float j = entity.getDamageWobbleStrength() - tickDelta;
        if (j < 0.0f) {
            j = 0.0f;
        }
        if (h > 0.0f) {
            matrixStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(h) * h * j / 10.0f * (float) entity.getDamageWobbleSide()));
        }

        // Updated variables
        float time = (entity.level().getGameTime() % 24000 + tickDelta) / 20.0f;
        BBAnimationVariables.set("time", time);
        entity.setAnimationVariables(tickDelta);

        // Render model
        BBModel bbModel = BBModelLoader.MODELS.get(getModelId());
        if (bbModel != null) {
            float health = entity.getHealth();
            float r = health * 0.6f + 0.4f;
            float g = health * 0.4f + 0.6f;
            float b = health * 0.4f + 0.6f;
            BBModelRenderer.renderModel(bbModel, matrixStack, submitNodeCollector, light, time, entity, getModel(entity), r, g, b, 1.0f);
        }
    }

    public void renderOptionalObject(String name, BBModel model, SubmitNodeCollector submitNodeCollector, T entity, PoseStack matrixStack, int light, float time) {
        renderOptionalObject(name, model, submitNodeCollector, entity, matrixStack, light, time, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void renderOptionalObject(String name, BBModel model, SubmitNodeCollector submitNodeCollector, T entity, PoseStack matrixStack, int light, float time, float red, float green, float blue, float alpha) {
        BBObject object = model.objectsByName.get(name);
        if (object != null) {
            BBModelRenderer.renderObject(model, object, matrixStack, submitNodeCollector, light, time, entity, null, red, green, blue, alpha);
        }
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        if (!entity.shouldRender(x, y, z)) {
            return false;
        }
        AABB box = entity.getBoundingBox().inflate(getCullingBoundingBoxInflation());
        return frustum.isVisible(box);
    }

    protected double getCullingBoundingBoxInflation() {
        return 1.0;
    }
}
