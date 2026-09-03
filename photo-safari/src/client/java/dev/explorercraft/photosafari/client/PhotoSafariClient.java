package dev.explorercraft.photosafari.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.explorercraft.photosafari.LootPayload;
import dev.explorercraft.photosafari.PhotoSafari;
import dev.explorercraft.photosafari.PhotoSafariConfig;
import dev.explorercraft.photosafari.PhotoScan;
import dev.explorercraft.photosafari.PhotographPayload;
import me.chrr.camerapture.fabric.event.ClientTakePictureCallback;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PhotoSafariClient implements ClientModInitializer {
    private enum Mode {
        PHOTOGRAPH, LOOT
    }

    /// ChatFormatting.GREEN / ChatFormatting.RED, made opaque.
    private static final int OUTLINE_LOOTABLE = 0xFF55FF55;
    private static final int OUTLINE_ON_COOLDOWN = 0xFFFF5555;
    private static final float OUTLINE_WIDTH = 2.0f;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(PhotoSafari.id("main"));
    private static final KeyMapping CYCLE_MODE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.photosafari.cycle_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));

    /// Which mode the camera used last, for the lifetime of the client: raising the camera
    /// again always continues where the player left off instead of resetting to photograph.
    private static Mode mode = Mode.PHOTOGRAPH;

    @Override
    public void onInitializeClient() {
        // Fires the moment the shutter is pressed, one frame before the screenshot is grabbed.
        // ponytail: close enough, the view barely moves in a frame.
        ClientTakePictureCallback.EVENT.register(() ->
                mode == Mode.LOOT && PhotoSafariConfig.peacefulLoot ? tryLoot() : reportSubjects());

        ClientTickEvents.END_CLIENT_TICK.register(PhotoSafariClient::handleTick);
        LevelRenderEvents.COLLECT_SUBMITS.register(PhotoSafariClient::renderLootOutlines);
    }

    private static void handleTick(Minecraft client) {
        if (!PhotoSafariConfig.peacefulLoot) {
            mode = Mode.PHOTOGRAPH;
        }

        while (CYCLE_MODE.consumeClick()) {
            if (client.player == null || !PhotoSafariConfig.peacefulLoot
                    || CameraItem.find(client.player, true) == null) {
                continue;
            }

            mode = mode == Mode.PHOTOGRAPH ? Mode.LOOT : Mode.PHOTOGRAPH;
            client.gui.hud.setOverlayMessage(Component.translatable(
                    mode == Mode.LOOT ? "text.photosafari.mode_loot" : "text.photosafari.mode_photograph"), false);
        }
    }

    private static InteractionResult reportSubjects() {
        List<Integer> subjects = collectSubjects();
        if (!subjects.isEmpty()) {
            ClientPlayNetworking.send(new PhotographPayload(subjects));
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult tryLoot() {
        List<Integer> subjects = collectSubjects();
        if (!subjects.isEmpty()) {
            ClientPlayNetworking.send(new LootPayload(subjects));
        }

        // Loot mode never takes a real picture: no paper spent, nothing saved.
        return InteractionResult.FAIL;
    }

    private static List<Integer> collectSubjects() {
        Minecraft minecraft = Minecraft.getInstance();
        List<Integer> subjects = new ArrayList<>();
        if (minecraft.player == null || minecraft.level == null) {
            return subjects;
        }

        // The viewfinder zoom narrows the FOV, so the photo frames tighter than the screen.
        double fov = minecraft.options.fov().get() * PictureTaker.getInstance().getFovModifier();
        double aspect = (double) minecraft.getWindow().getWidth() / (double) minecraft.getWindow().getHeight();
        Vec3 eye = minecraft.player.getEyePosition(1.0f);
        Vec3 look = minecraft.player.getViewVector(1.0f);

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity == minecraft.player || !PhotoScan.isWildlife(entity)) {
                continue;
            }

            if (PhotoScan.isPhotographed(minecraft.level, eye, look, fov, aspect, entity)) {
                subjects.add(entity.getId());
                if (subjects.size() >= PhotographPayload.MAX_ENTITIES) {
                    break;
                }
            }
        }

        return subjects;
    }

    /// Wireframes every mob currently framed for loot, green if it can still be looted and
    /// red if it's on cooldown. Reuses the exact same frame/occlusion check as the actual
    /// loot trigger, so what's outlined is exactly what pressing the trigger would act on.
    private static void renderLootOutlines(LevelRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (mode != Mode.LOOT || minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (CameraItem.find(minecraft.player, true) == null) {
            return;
        }

        double fov = minecraft.options.fov().get() * PictureTaker.getInstance().getFovModifier();
        double aspect = (double) minecraft.getWindow().getWidth() / (double) minecraft.getWindow().getHeight();
        Vec3 eye = minecraft.player.getEyePosition(1.0f);
        Vec3 look = minecraft.player.getViewVector(1.0f);
        List<UUID> recentlyLooted = minecraft.player.getAttachedOrCreate(PhotoSafari.RECENTLY_LOOTED);

        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity == minecraft.player || !PhotoScan.isWildlife(entity)) {
                continue;
            }

            if (!PhotoScan.isPhotographed(minecraft.level, eye, look, fov, aspect, entity)) {
                continue;
            }

            int color = recentlyLooted.contains(entity.getUUID()) ? OUTLINE_ON_COOLDOWN : OUTLINE_LOOTABLE;
            AABB box = entity.getBoundingBox();
            VoxelShape shape = Shapes.create(box);

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            collector.submitShapeOutline(poseStack, shape, RenderTypes.lines(), color, OUTLINE_WIDTH, false);
            poseStack.popPose();
        }
    }
}
