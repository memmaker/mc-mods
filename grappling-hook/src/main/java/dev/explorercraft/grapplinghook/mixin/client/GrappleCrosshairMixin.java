package dev.explorercraft.grapplinghook.mixin.client;

import com.mojang.blaze3d.platform.Window;
import dev.explorercraft.grapplinghook.client.GrappleModClient;
import dev.explorercraft.grapplinghook.content.item.GrapplehookItem;
import dev.explorercraft.grapplinghook.content.registry.internal.ModItems;
import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.explorercraft.grapplinghook.content.registry.CustomizationProperties.*;

@Mixin(Hud.class)
public abstract class GrappleCrosshairMixin {

    @Final @Shadow
    private Minecraft minecraft;

    @Shadow @Final private static Identifier CROSSHAIR_SPRITE;


    // 26.2 renders the HUD from an extracted render state; the crosshair pass is extractCrosshair.
    @Inject(method = "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("TAIL"))
    public void renderModCrosshair(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {

        LocalPlayer player = this.minecraft.player;
        ItemStack grapplehookItemStack = null;

        if (player == null) return;

        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof GrapplehookItem) {
            grapplehookItemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        } else if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof GrapplehookItem) {
            grapplehookItemStack = player.getItemInHand(InteractionHand.OFF_HAND);
        }

        if (grapplehookItemStack != null) {
            HookCustomization custom = ModItems.GRAPPLING_HOOK.get().getCustomizationsOrDefault(grapplehookItemStack);
            double angle = Math.toRadians(custom.get(DOUBLE_HOOK_ANGLE.get()));
            double verticalAngle = Math.toRadians(custom.get(HOOK_THROW_ANGLE.get()));

            if (player.isCrouching()) {
                angle = Math.toRadians(custom.get(DOUBLE_HOOK_ANGLE_ON_SNEAK.get()));
                verticalAngle = Math.toRadians(custom.get(HOOK_THROW_ANGLE_ON_SNEAK.get()));
            }

            if (!custom.get(DOUBLE_HOOK_ATTACHED.get()))
                angle = 0;

            Window resolution = this.minecraft.getWindow();
            int w = resolution.getGuiScaledWidth();
            int h = resolution.getGuiScaledHeight();

            double fov = Math.toRadians(this.minecraft.options.fov().get());
            fov *= player.getFieldOfViewModifier(false, deltaTracker.getGameTimeDeltaPartialTick(true));
            double l = ((double) h/2) / Math.tan(fov/2);

            if (!((verticalAngle == 0) && (!custom.get(DOUBLE_HOOK_ATTACHED.get()) || angle == 0))) {
                int offset = (int) (Math.tan(angle) * l);
                int verticalOffset = (int) (-Math.tan(verticalAngle) * l);

                this.drawCrosshair(guiGraphics, w / 2 + offset, h / 2 + verticalOffset);
                if (angle != 0) {
                    this.drawCrosshair(guiGraphics, w / 2 - offset, h / 2 + verticalOffset);
                }
            }

            if (custom.get(ROCKET_ATTACHED.get()) && custom.get(ROCKET_ANGLE.get()) != 0) {
                int verticalOffset = (int) (-Math.tan(Math.toRadians(custom.get(ROCKET_ANGLE.get()))) * l);
                this.drawCrosshair(guiGraphics, w / 2, h / 2 + verticalOffset);
            }
        }

        double rocketFuel = GrappleModClient.get().getClientControllerManager().rocketFuel;

        if (rocketFuel < 1) {
            Window resolution = this.minecraft.getWindow();
            int w = resolution.getGuiScaledWidth();
            int h = resolution.getGuiScaledHeight();

            int totalbarLength = w / 8;

            this.drawRect(guiGraphics, w / 2 - totalbarLength / 2, h * 3 / 4, totalbarLength, 2, 50, 100);
            this.drawRect(guiGraphics, w / 2 - totalbarLength / 2, h * 3 / 4, (int) (totalbarLength * rocketFuel), 2, 200, 255);
        }
    }


    /** RenderPipelines.CROSSHAIR is the inverted-blend pipeline vanilla's own crosshair uses. */
    @Unique
    private void drawCrosshair(GuiGraphicsExtractor guiGraphics, int x, int y) {
        guiGraphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_SPRITE,
                (int) (x - (15.0F / 2)), (int) (y - (15.0F / 2)), 15, 15);
    }

    @Unique
    private void drawRect(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int g, int a) {
        int argb = (a << 24) | (g << 16) | (g << 8) | g;
        guiGraphics.fill(x, y, x + width, y + height, argb);
    }
}
