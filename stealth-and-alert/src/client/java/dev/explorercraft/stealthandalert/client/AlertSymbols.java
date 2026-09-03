package dev.explorercraft.stealthandalert.client;

import dev.explorercraft.stealthandalert.AlertData;
import dev.explorercraft.stealthandalert.ScreenProjection;
import dev.explorercraft.stealthandalert.StealthAndAlert;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/// The "?" and "!" that float over a mob's head while it wonders about you.
///
/// Drawn in screen space rather than as a world quad: the alert state is already synced to the
/// client, so projecting the head position onto the HUD gets the same picture for a fraction of
/// the work a custom render layer would cost.
public final class AlertSymbols {
    private AlertSymbols() {
    }

    private static final Identifier BACKGROUND = StealthAndAlert.id("textures/gui/alert_symbol_background.png");
    private static final Identifier QUESTION = StealthAndAlert.id("textures/gui/alert_symbol_question.png");
    private static final Identifier EXCLAMATION = StealthAndAlert.id("textures/gui/alert_symbol_exclamation.png");

    private static final double MAX_DISTANCE = 48.0;
    private static final int BASE_SIZE = 22;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int AMBER = 0xFFFFCC00;
    private static final int SHADE = 0x80000000;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.isSpectator()) return;

        Camera camera = minecraft.gameRenderer.mainCamera();
        Vec3 eye = camera.position();
        // The camera moves between ticks; use the same partial tick for the mob or the symbol lags behind it.
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof Mob mob)) continue;
            if (mob.distanceToSqr(eye) > MAX_DISTANCE * MAX_DISTANCE) continue;

            AlertData data = mob.getAttachedOrCreate(StealthAndAlert.ALERT);
            if (data.state() == AlertData.IDLE) continue;

            Vec3 head = mob.getPosition(partialTick).add(0, mob.getBbHeight() + 0.6, 0);
            ScreenProjection.Point point = ScreenProjection.project(eye, camera.xRot(), camera.yRot(),
                    camera.getFov(), graphics.guiWidth(), graphics.guiHeight(), head);
            if (point == null) continue;
            if (occluded(minecraft, eye, head)) continue;

            int x = (int) point.x();
            int y = (int) point.y();
            double depth = point.depth();

            int size = (int) Math.max(10, BASE_SIZE * (1.0 - depth / MAX_DISTANCE * 0.6));
            draw(graphics, BACKGROUND, x, y, size, SHADE);

            if (data.state() == AlertData.FIGHTING) {
                draw(graphics, EXCLAMATION, x, y, size, AMBER);
            } else {
                draw(graphics, QUESTION, x, y, size, data.state() == AlertData.SEARCHING ? AMBER : WHITE);
            }
        }
    }

    /// Nothing solid may sit between the camera and the mob's head.
    private static boolean occluded(Minecraft minecraft, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player);
        return minecraft.level.clip(context).getType() != HitResult.Type.MISS;
    }

    /// Squeezes the whole 128px texture into `size` pixels by lying about the texture dimensions.
    private static void draw(GuiGraphicsExtractor graphics, Identifier texture, int centerX, int centerY, int size, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                centerX - size / 2, centerY - size / 2, 0.0F, 0.0F, size, size, size, size, color);
    }
}
