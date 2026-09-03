package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.entity.misc.Trail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class TrailRenderer {
    private static final Identifier identifier = Main.locate("textures/entity/trail.png");
    private static final RenderType RENDER_TYPE = RenderTypes.beaconBeam(identifier, true);

    public static void render(Trail trail, SubmitNodeCollector submitNodeCollector, PoseStack matrixStack) {
        if (trail.nullEntries >= trail.size || trail.entries == 0) {
            return;
        }

        int light = 15728640;
        Vec3 pos = Minecraft.getInstance().gameRenderer.mainCamera().position();

        submitNodeCollector.submitCustomGeometry(matrixStack, RENDER_TYPE, (pose, lineVertexConsumer) -> {
            Matrix3f matrix = pose.normal();
            for (int i = 1; i < Math.min(trail.entries, trail.size); i++) {
                int pre = ((i + trail.lastIndex - 1) % trail.size) * 7;
                int index = ((i + trail.lastIndex) % trail.size) * 7;

                int a1 = (int) ((1.0f - ((float) i) / trail.size * 255) * trail.buffer[pre + 6]);
                int a2 = i == (trail.size - 1) ? 0 : (int) ((1.0f - ((float) i + 1) / trail.size * 255) * trail.buffer[index + 6]);

                vertex(trail, lineVertexConsumer, matrix, 0, 0, pre, pos, a1, light);
                vertex(trail, lineVertexConsumer, matrix, 0, 1, pre + 3, pos, a1, light);
                vertex(trail, lineVertexConsumer, matrix, 1, 1, index + 3, pos, a2, light);
                vertex(trail, lineVertexConsumer, matrix, 1, 0, index, pos, a2, light);

                // ponytail: anti-culling double-wind, kept from upstream as-is
                vertex(trail, lineVertexConsumer, matrix, 1, 0, index, pos, a2, light);
                vertex(trail, lineVertexConsumer, matrix, 1, 1, index + 3, pos, a2, light);
                vertex(trail, lineVertexConsumer, matrix, 0, 1, pre + 3, pos, a1, light);
                vertex(trail, lineVertexConsumer, matrix, 0, 0, pre, pos, a1, light);
            }
        });
    }

    private static void vertex(Trail trail, VertexConsumer lineVertexConsumer, Matrix3f matrix, float u, float v, int index, Vec3 pos, int a, int light) {
        Vector3f p = new Vector3f((float) (trail.buffer[index] - pos.x), (float) (trail.buffer[index + 1] - pos.y), (float) (trail.buffer[index + 2] - pos.z));
        matrix.transform(p);
        int gray = (int) (trail.gray * 255.0f);
        int alpha = Mth.clamp(a, 0, 255);
        int color = ARGB.color(alpha, gray, gray, gray);
        lineVertexConsumer.addVertex(p.x, p.y, p.z, color, u, v, OverlayTexture.NO_OVERLAY, light, 1, 0, 0);
    }
}
