package dev.explorercraft.immersiveaircraft.client.render.entity.renderer.bullet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.entity.bullet.BulletEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class BulletEntityRenderer<T extends BulletEntity> extends EntityRenderer<T, BulletEntityRenderer.BulletRenderState> {
    private static final Identifier TEXTURE = Main.locate("textures/entity/bullet.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    public BulletEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BulletRenderState extends EntityRenderState {
        public float scale;
    }

    @Override
    public BulletRenderState createRenderState() {
        return new BulletRenderState();
    }

    @Override
    public void extractRenderState(T entity, BulletRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
    }

    @Override
    public void submit(BulletRenderState state, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        matrixStack.pushPose();
        matrixStack.scale(state.scale, state.scale, state.scale);
        matrixStack.translate(0.0, 0.5, 0.0);
        matrixStack.mulPose(camera.orientation);
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        int light = state.lightCoords;
        submitNodeCollector.submitCustomGeometry(matrixStack, RENDER_TYPE, (pose, vertexConsumer) -> {
            vertex(vertexConsumer, pose, light, 0.0f, 0.0f, 0.0f, 1.0f);
            vertex(vertexConsumer, pose, light, 1.0f, 0.0f, 1.0f, 1.0f);
            vertex(vertexConsumer, pose, light, 1.0f, 1.0f, 1.0f, 0.0f);
            vertex(vertexConsumer, pose, light, 0.0f, 1.0f, 0.0f, 0.0f);
        });
        matrixStack.popPose();
        super.submit(state, matrixStack, submitNodeCollector, camera);
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, PoseStack.Pose pose, int light, float x, float y, float u, float v) {
        vertexConsumer.addVertex(pose, x - 0.5f, y - 0.5f, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
