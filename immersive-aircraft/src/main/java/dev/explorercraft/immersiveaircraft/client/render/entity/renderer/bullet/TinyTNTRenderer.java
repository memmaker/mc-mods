package dev.explorercraft.immersiveaircraft.client.render.entity.renderer.bullet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.explorercraft.immersiveaircraft.entity.bullet.TinyTNT;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

// Ported from upstream's old render()/MultiBufferSource TinyTNTRenderer to the current
// createRenderState()/extractRenderState()/submit(SubmitNodeCollector) deferred pipeline, mirroring
// how vanilla's own TntMinecartRenderer now renders its carried TNT block (see
// TntMinecartRenderer#submitMinecartContents / #submitWhiteSolidBlock). The block model render
// state is resolved once per frame via BlockModelResolver (from EntityRendererProvider.Context),
// same as AbstractMinecartRenderer does for a minecart's displayed block.
public class TinyTNTRenderer extends EntityRenderer<TinyTNT, TinyTNTRenderer.TinyTNTRenderState> {
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;

    public TinyTNTRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.2f;
        this.blockModelResolver = context.getBlockModelResolver();
    }

    public static class TinyTNTRenderState extends EntityRenderState {
        public float fuseRemainingInTicks;
        public final BlockModelRenderState blockModel = new BlockModelRenderState();
    }

    @Override
    public TinyTNTRenderState createRenderState() {
        return new TinyTNTRenderState();
    }

    @Override
    public void extractRenderState(TinyTNT entity, TinyTNTRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.fuseRemainingInTicks = (float) entity.getFuse() - partialTicks + 1.0f;
        blockModelResolver.update(state.blockModel, Blocks.TNT.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
    }

    @Override
    public void submit(TinyTNTRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.5, 0.0);
        if (state.fuseRemainingInTicks < 10.0f) {
            float f = 1.0f - state.fuseRemainingInTicks / 10.0f;
            f = Mth.clamp(f, 0.0f, 1.0f);
            f *= f;
            f *= f;
            float g = 1.0f + f * 0.3f;
            poseStack.scale(g, g, g);
        }
        poseStack.scale(0.375f, 0.375f, 0.375f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
        poseStack.translate(-0.5, -0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        boolean white = ((int) state.fuseRemainingInTicks) / 5 % 2 == 0;
        TntMinecartRenderer.submitWhiteSolidBlock(state.blockModel, poseStack, submitNodeCollector, state.lightCoords, white, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
