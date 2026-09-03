package dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.*;
import dev.explorercraft.immersiveaircraft.util.Utils;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.joml.Vector3f;

import java.util.List;

/**
 * ponytail: the new MC render pipeline defers geometry submission (submit() builds a list of
 * render nodes, actual drawing happens later on the render thread), so this can no longer grab a
 * persistent VertexConsumer and write into it in place like 1.20.1 did. Bridged via
 * SubmitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> ...): the
 * poseStack/pose snapshot is captured immediately (synchronously) when submitCustomGeometry is
 * called, so the transform math below is unchanged from the original - only the vertex-write
 * calls moved from the old vertex()/color()/uv()/overlayCoords()/uv2()/normal()/endVertex() chain
 * to the new addVertex(pose,..)/setColor/setUv/setOverlay/setLight/setNormal(pose,..) chain.
 */
public class BBModelRenderer {
    public interface RenderTypeProvider {
        RenderType getRenderType(BBFaceContainer container, BBFace face);
    }

    public static final RenderTypeProvider DEFAULT_RENDER_TYPE_PROVIDER = (container, face) ->
            container.enableCulling() ? RenderTypes.entityCutoutCull(face.texture.location) : RenderTypes.entityCutout(face.texture.location);

    public static <T extends VehicleEntity> void renderModel(BBModel model, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, float time, T entity, ModelPartRenderHandler<T> modelPartRenderer, float red, float green, float blue, float alpha) {
        model.root.forEach(object -> renderObject(model, object, matrixStack, submitNodeCollector, light, time, entity, modelPartRenderer, red, green, blue, alpha));
    }

    /**
     * Apply transformations, animations, and callbacks, and render the object.
     */
    public static <T extends VehicleEntity> void renderObject(BBModel model, BBObject object, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, float time, T entity, ModelPartRenderHandler<T> modelPartRenderer, float red, float green, float blue, float alpha) {
        matrixStack.pushPose();
        matrixStack.translate(object.origin.x(), object.origin.y(), object.origin.z());

        // Apply animations
        if (!model.animations.isEmpty()) {
            BBAnimation animation = model.animations.get(0);
            if (animation.hasAnimator(object.uuid)) {
                Vector3f position = animation.sample(object.uuid, BBAnimator.Channel.POSITION, time);
                position.mul(1.0f / 16.0f);
                matrixStack.translate(position.x(), position.y(), position.z());

                Vector3f rotation = animation.sample(object.uuid, BBAnimator.Channel.ROTATION, time);
                rotation.mul(1.0f / 180.0f * (float) Math.PI);
                matrixStack.mulPose(Utils.fromXYZ(rotation));

                Vector3f scale = animation.sample(object.uuid, BBAnimator.Channel.SCALE, time);
                matrixStack.scale(scale.x(), scale.y(), scale.z());
            }
        }

        // Apply object rotation
        matrixStack.mulPose(Utils.fromXYZ(object.rotation));

        // Apply additional, complex animations
        if (object instanceof BBBone bone && modelPartRenderer != null) {
            modelPartRenderer.animate(bone.name, entity, matrixStack, time);
        }

        // The bones origin is only used during transformation
        if (object instanceof BBBone) {
            matrixStack.translate(-object.origin.x(), -object.origin.y(), -object.origin.z());
        }

        // Render the object
        if (modelPartRenderer == null || !modelPartRenderer.render(object.name, model, object, submitNodeCollector, entity, matrixStack, light, time, modelPartRenderer)) {
            renderObjectInner(model, object, matrixStack, submitNodeCollector, light, time, entity, modelPartRenderer, red, green, blue, alpha);
        }

        matrixStack.popPose();
    }

    /**
     * Render the object without applying transformations, animations, or callbacks.
     */
    public static <T extends VehicleEntity> void renderObjectInner(BBModel model, BBObject object, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, float time, T entity, ModelPartRenderHandler<T> modelPartRenderer, float red, float green, float blue, float alpha) {
        if (object instanceof BBFaceContainer cube) {
            renderFaces(cube, matrixStack, submitNodeCollector, light, red, green, blue, alpha, modelPartRenderer == null ? DEFAULT_RENDER_TYPE_PROVIDER : modelPartRenderer.getRenderTypeProvider());
        } else if (object instanceof BBBone bone) {
            boolean shouldRender = bone.visibility;
            if (bone.name.equals("lod0")) {
                shouldRender = entity.isWithinParticleRange();
            } else if (bone.name.equals("lod1")) {
                shouldRender = !entity.isWithinParticleRange();
            }

            if (shouldRender) {
                bone.children.forEach(child -> renderObject(model, child, matrixStack, submitNodeCollector, light, time, entity, modelPartRenderer, red, green, blue, alpha));
            }
        }
    }

    public static void renderFaces(BBFaceContainer cube, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, float red, float green, float blue, float alpha, RenderTypeProvider provider) {
        for (BBFace face : cube.getFaces()) {
            RenderType renderType = provider.getRenderType(cube, face);
            submitNodeCollector.submitCustomGeometry(matrixStack, renderType, (pose, vertexConsumer) -> {
                for (int i = 0; i < 4; i++) {
                    BBFace.BBVertex v = face.vertices[i];
                    vertexConsumer.addVertex(pose, v.x, v.y, v.z)
                            .setColor(red, green, blue, alpha)
                            .setUv(v.u, v.v)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(pose, v.nx, v.ny, v.nz);
                }
            });
        }
    }

    public static void renderBanner(BBFaceContainer cube, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, boolean isBanner, List<Pair<Holder<BannerPattern>, DyeColor>> patterns) {
        matrixStack.pushPose();

        if (cube instanceof BBObject object) {
            matrixStack.translate(object.origin.x(), object.origin.y(), object.origin.z());
        }

        // ponytail: upstream renders banner patterns via the material atlas sprite system
        // (Sheets.getBannerMaterial()/getShieldMaterial(), which needs a MultiBufferSource that
        // no longer exists at submit() time). Banner-on-vehicle decals are cosmetic and rare
        // (only a couple of vehicles expose banner slots) - skipped for now, upgrade by resolving
        // the sprite via SpriteGetter and using submitModel/submitCustomGeometry with it directly.
        matrixStack.popPose();
    }

    public static void renderSailObject(BBMesh cube, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, float time, float red, float green, float blue, float alpha) {
        renderSailObject(cube, matrixStack, submitNodeCollector, light, time, red, green, blue, alpha, 0.025f, 0.0f);
    }

    public static void renderSailObject(BBMesh cube, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, float time, float red, float green, float blue, float alpha, float distanceScale, float baseScale) {
        for (BBFace face : cube.getFaces()) {
            RenderType renderType = RenderTypes.entityCutout(face.texture.location);
            submitNodeCollector.submitCustomGeometry(matrixStack, renderType, (pose, vertexConsumer) -> {
                for (int i = 0; i < 4; i++) {
                    BBFace.BBVertex v = face.vertices[i];
                    float distance = Math.max(Math.max(Math.abs(v.x), Math.abs(v.y)), Math.abs(v.z));
                    double angle = (v.x + v.z + v.y * 0.25) * 4.0f + time * 4.0f;
                    double scale = distanceScale * distance + baseScale;
                    float x = (float) ((Math.cos(angle) + Math.cos(angle * 1.7)) * scale);
                    float z = (float) ((Math.sin(angle) + Math.sin(angle * 1.7)) * scale);

                    vertexConsumer.addVertex(pose, v.x + x, v.y, v.z + z)
                            .setColor(red, green, blue, alpha)
                            .setUv(v.u, v.v)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(pose, v.nx, v.ny, v.nz);
                }
            });
        }
    }
}
