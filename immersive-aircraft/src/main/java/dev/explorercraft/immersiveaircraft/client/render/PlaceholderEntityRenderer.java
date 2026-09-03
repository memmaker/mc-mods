package dev.explorercraft.immersiveaircraft.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * ponytail: stand-in renderer. Upstream draws every aircraft with a bespoke
 * BlockBench-model renderer (client/render/entity/renderer/**, ~2850 lines) built on
 * MultiBufferSource/GuiGraphics/RenderType APIs that this Minecraft version removed
 * in favor of the createRenderState()/submit(SubmitNodeCollector) pipeline.
 *
 * This draws nothing (entity is invisible) but satisfies the renderer contract so
 * every vehicle entity type can spawn and be interacted with without crashing.
 * Porting the real BBModel renderer to the new submit-node pipeline is the single
 * largest remaining item of this port - see unported-client-rendering/.
 */
public class PlaceholderEntityRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {
    public PlaceholderEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
