package dev.explorercraft.stealthandalert.client;

import dev.explorercraft.stealthandalert.StealthAndAlert;
import dev.explorercraft.stealthandalert.StealthConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/// The eye above the crosshair: how exposed the player is right now.
public final class VisibilityBar {
    private VisibilityBar() {
    }

    private static final Identifier BAR = StealthAndAlert.id("textures/gui/visibility_bar.png");
    private static final Identifier EYE = StealthAndAlert.id("textures/gui/visibility_eye.png");
    private static final Identifier SLASH = StealthAndAlert.id("textures/gui/visibility_slash.png");

    private static final int TEXTURE_SIZE = 128;
    private static final int EYE_WIDTH = 38;
    private static final int HALF_BAR = 37;

    /// Eases towards the real value so the bar doesn't twitch on every light-level change.
    private static double displayed = 0;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) return;

        double visibility = StealthAndAlert.visibilityOf(minecraft.player);
        if (!minecraft.isPaused()) {
            displayed = Mth.lerp(0.01, displayed, visibility);
        }

        int x = graphics.guiWidth() / 2 - EYE_WIDTH / 2;
        int y = 30 - TEXTURE_SIZE / 2 + 4;

        boolean hidden = displayed <= StealthConfig.VISIBILITY_THRESHOLD + 0.01;

        if (!hidden) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BAR, x, y,
                    TEXTURE_SIZE / 2f - EYE_WIDTH / 2f, 0, EYE_WIDTH, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

            // The bar grows outwards from the eye in both directions.
            double filled = Math.clamp(displayed - StealthConfig.VISIBILITY_THRESHOLD, 0, 1 - StealthConfig.VISIBILITY_THRESHOLD)
                    / (1 - StealthConfig.VISIBILITY_THRESHOLD);
            int width = visibility >= 0.99 && Math.abs(displayed - 1) < 0.02 ? HALF_BAR : (int) (HALF_BAR * filled);

            graphics.blit(RenderPipelines.GUI_TEXTURED, BAR, x + EYE_WIDTH, y, 8, 0, width, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BAR, x - width, y, 83, 0, width, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, EYE, x + 8, y, 53, 0, 22, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        if (hidden) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SLASH, x + 12, y, 57, 0, 14, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }
}
