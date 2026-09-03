package dev.explorercraft.pistolsilencer.client;

import dev.explorercraft.pistolsilencer.PistolSilencer;
import dev.explorercraft.pistolsilencer.item.PistolItem;
import dev.explorercraft.pistolsilencer.network.HeadBoxHintPayload;
import dev.explorercraft.pistolsilencer.network.ReloadPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class PistolSilencerClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(PistolSilencer.id("pistol"));
    private static final KeyMapping RELOAD_KEY = new KeyMapping("key.pistolsilencer.reload",
            GLFW.GLFW_KEY_R, CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(RELOAD_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) {
                return;
            }
            boolean holdingPistol = player.getMainHandItem().getItem() instanceof PistolItem;
            while (RELOAD_KEY.consumeClick()) {
                if (holdingPistol) {
                    ClientPlayNetworking.send(new ReloadPayload());
                }
            }
            if (holdingPistol) {
                sendHeadBoxHint(player);
            }
        });
    }

    /**
     * Tells the server the real head box of whatever the player is currently aiming a pistol at,
     * read straight off fx-globals' render-model tracking (if that mod is loaded) rather than a
     * server-side guess from the hitbox. Mirrors the server's own aim ray, minus the random
     * spread it rolls independently — close enough to identify the same target.
     */
    private static void sendHeadBoxHint(LocalPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getViewVector(1.0F).scale(PistolItem.RANGE));
        EntityHitResult hit = PistolItem.raycastEntity(player, start, end);

        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }

        AABB headBox = FxGlobalsHeadBoxClient.get(target.getId());

        if (headBox != null) {
            ClientPlayNetworking.send(new HeadBoxHintPayload(target.getId(),
                    new Vec3(headBox.minX, headBox.minY, headBox.minZ),
                    new Vec3(headBox.maxX, headBox.maxY, headBox.maxZ)));
        }
    }
}
