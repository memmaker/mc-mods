package dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBModel;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBObject;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.Entity;

public record ModelPartRenderer<T extends Entity>(
        String id,
        ModelPartRenderer.AnimationConsumer<T> animationConsumer,
        ModelPartRenderer.RenderConsumer<T> renderConsumer
) {
    public interface AnimationConsumer<T> {
        void run(T entity, float yaw, float time, PoseStack matrixStack);
    }

    public interface RenderConsumer<T extends Entity> {
        void run(BBModel model, BBObject object, SubmitNodeCollector submitNodeCollector, T entity, PoseStack matrixStack, int light, float time, ModelPartRenderHandler<T> modelPartRenderer);
    }
}
