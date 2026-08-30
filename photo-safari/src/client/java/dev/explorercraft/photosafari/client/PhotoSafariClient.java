package dev.explorercraft.photosafari.client;

import dev.explorercraft.photosafari.PhotoScan;
import dev.explorercraft.photosafari.PhotographPayload;
import me.chrr.camerapture.fabric.event.ClientTakePictureCallback;
import me.chrr.camerapture.picture.PictureTaker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PhotoSafariClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Fires the moment the shutter is pressed, one frame before the screenshot is grabbed.
        // ponytail: close enough, the view barely moves in a frame.
        ClientTakePictureCallback.EVENT.register(() -> {
            reportSubjects();
            return InteractionResult.PASS;
        });
    }

    private static void reportSubjects() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // The viewfinder zoom narrows the FOV, so the photo frames tighter than the screen.
        double fov = minecraft.options.fov().get() * PictureTaker.getInstance().getFovModifier();
        double aspect = (double) minecraft.getWindow().getWidth() / (double) minecraft.getWindow().getHeight();
        Vec3 eye = minecraft.player.getEyePosition(1.0f);
        Vec3 look = minecraft.player.getViewVector(1.0f);

        List<Integer> subjects = new ArrayList<>();
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

        if (!subjects.isEmpty()) {
            ClientPlayNetworking.send(new PhotographPayload(subjects));
        }
    }
}
