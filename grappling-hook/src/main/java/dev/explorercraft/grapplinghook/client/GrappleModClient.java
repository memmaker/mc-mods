package dev.explorercraft.grapplinghook.client;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.client.network.ClientNetworkReceivers;
import dev.explorercraft.grapplinghook.client.physics.ClientPhysicsControllerTracker;
import dev.explorercraft.grapplinghook.client.physics.controller.AirFrictionPhysicsController;
import dev.explorercraft.grapplinghook.client.physics.controller.ForcefieldPhysicsController;
import dev.explorercraft.grapplinghook.client.render.entity.GrapplinghookEntityRenderer;
import dev.explorercraft.grapplinghook.client.render.item.GrappleItemModelProperties;
import dev.explorercraft.grapplinghook.config.GrappleModClientConfig;
import dev.explorercraft.grapplinghook.content.entity.grapplinghook.GrapplinghookEntity;
import dev.explorercraft.grapplinghook.content.registry.internal.ModEntities;
import dev.explorercraft.grapplinghook.content.registry.internal.ModItems;
import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.customization.type.BooleanProperty;
import dev.explorercraft.grapplinghook.util.GrappleModUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static dev.explorercraft.grapplinghook.content.registry.CustomizationProperties.*;

@Environment(EnvType.CLIENT)
public class GrappleModClient implements ClientModInitializer {

    private static GrappleModClient clientInstance;

    private ClientPhysicsControllerTracker clientPhysicsControllerTracker;


    @Override
    public void onInitializeClient() {
        GrappleModClient.clientInstance = this;

        try {
            this.initConfig();
        } catch (Exception e) {
            GrappleMod.LOGGER.info(e);
        }

        EntityRendererRegistry.register(ModEntities.GRAPPLE_HOOK.get(), new GrapplehookEntityRenderFactory());

        ClientNetworkReceivers.registerAll();

        ModKeys.registerAll();
        GrappleItemModelProperties.registerAll();

        this.clientPhysicsControllerTracker = new ClientPhysicsControllerTracker();
    }

    public static GrappleModClient get() {
        return GrappleModClient.clientInstance;
    }

    public void initConfig() {
        GrappleModClientConfig.HANDLER.defaults().saveDefaults();
        GrappleModClientConfig.HANDLER.load();
    }


    public void startRocket(Player player, HookCustomization custom) {
        this.getClientControllerManager().startRocket(player, custom);
    }

    public void resetLauncherTime(int playerId) {
        this.getClientControllerManager().resetLauncherTime(playerId);
    }

    public void launchPlayer(Player player) {
        this.getClientControllerManager().launchPlayer(player);
    }

    public void updateRocketRegen(double rocketActiveTime, double rocketRefuelRatio) {
        this.getClientControllerManager().updateRocketRegen(rocketActiveTime, rocketRefuelRatio);
    }

    public double getRocketFunctioning() {
        return this.getClientControllerManager().getRocketFunctioning();
    }

    public long getTimeSinceLastRopeJump(Level world) {
        return world.getGameTime() - ClientPhysicsControllerTracker.prevRopeJumpTime;
    }

    public void resetRopeJumpTime(Level world) {
        ClientPhysicsControllerTracker.prevRopeJumpTime = world.getGameTime();
    }

    public boolean isMovingSlowly(Entity entity) {
        if (entity instanceof LocalPlayer player) {
            return player.isMovingSlowly();
        }

        return false;
    }

    public void playSound(Identifier loc, float volume) {
        Player player = Minecraft.getInstance().player;
        if(player == null) return;

        SimpleSoundInstance sound = new SimpleSoundInstance(
                loc, SoundSource.PLAYERS, volume, 1.0F, RandomSource.create(),
                false, 0,
                SoundInstance.Attenuation.NONE,
                player.getX(), player.getY(), player.getZ(),
                false
        );

        Minecraft.getInstance()
                .getSoundManager()
                .play(sound);
    }

    private static int propertyEquipOverride(ItemStack stack, BooleanProperty property) {
        HookCustomization volume = ModItems.GRAPPLING_HOOK.get().getCustomizationsOrDefault(stack);
        return volume.get(property) ? 1 : 0;
    }


    public ClientPhysicsControllerTracker getClientControllerManager() {
        return this.clientPhysicsControllerTracker;
    }

    private static class GrapplehookEntityRenderFactory implements EntityRendererProvider<GrapplinghookEntity> {

        @Override
        @NotNull
        public EntityRenderer<GrapplinghookEntity, ?> create(Context manager) {
            return new GrapplinghookEntityRenderer<>(manager, ModItems.GRAPPLING_HOOK.get());
        }

    }
}
