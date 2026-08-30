package dev.explorercraft.photosafari;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.chrr.camerapture.Camerapture;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PhotoSafari implements ModInitializer {
    public static final String MOD_ID = "photosafari";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /// Species this player has on film. Survives death, saved with the player.
    public static final AttachmentType<Set<Identifier>> PHOTOGRAPHED = AttachmentRegistry
            .<Set<Identifier>>builder()
            .persistent(Identifier.CODEC.listOf().xmap(LinkedHashSet::new, List::copyOf))
            .initializer(LinkedHashSet::new)
            .copyOnDeath()
            .buildAndRegister(id("photographed"));

    public static SpeciesPhotographedTrigger photographedTrigger;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        photographedTrigger = Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                id("species_photographed"), new SpeciesPhotographedTrigger());

        PayloadTypeRegistry.serverboundPlay().register(PhotographPayload.TYPE, PhotographPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PhotographPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handlePhotograph(context.player(), payload)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("photosafari")
                        .then(Commands.literal("camera")
                                .executes(ctx -> giveCamera(ctx.getSource().getPlayerOrException(), 1))
                                .then(Commands.argument("paper", IntegerArgumentType.integer(0, 64))
                                        .executes(ctx -> giveCamera(ctx.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(ctx, "paper")))))
                        .then(Commands.literal("progress")
                                .executes(ctx -> showProgress(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("reset")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> resetProgress(ctx.getSource().getPlayerOrException())))));
    }

    public static void handlePhotograph(ServerPlayer player, PhotographPayload payload) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        Set<Identifier> seen = new LinkedHashSet<>(player.getAttachedOrCreate(PHOTOGRAPHED));
        int before = seen.size();

        for (int entityId : payload.entityIds()) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || !PhotoScan.isWildlife(entity)) {
                continue;
            }

            // The client decides what made it onto the picture, so never trust it: verify here.
            if (!PhotoScan.isPhotographedLenient(level, eye, look, entity)) {
                continue;
            }

            Identifier species = EntityType.getKey(entity.getType());
            if (seen.add(species)) {
                player.sendOverlayMessage(Component.translatable("text.photosafari.new_species",
                        entity.getType().getDescription()).withStyle(ChatFormatting.GREEN));
                photographedTrigger.trigger(player, seen.size(), species);
            }
        }

        if (seen.size() != before) {
            player.setAttached(PHOTOGRAPHED, seen);
        }

        // Fired on every photo, so count-based advancements are re-checked even without a new species.
        photographedTrigger.trigger(player, seen.size(), null);
    }

    private static int giveCamera(ServerPlayer player, int paper) {
        player.getInventory().placeItemBackInInventory(new ItemStack(Camerapture.CAMERA));
        if (paper > 0) {
            player.getInventory().placeItemBackInInventory(new ItemStack(net.minecraft.world.item.Items.PAPER, paper));
        }

        player.sendSystemMessage(Component.translatable("text.photosafari.camera_given"));
        return 1;
    }

    private static int showProgress(ServerPlayer player) {
        Set<Identifier> seen = player.getAttachedOrCreate(PHOTOGRAPHED);
        player.sendSystemMessage(Component.translatable("text.photosafari.progress",
                seen.size(), countSpecies()));
        return seen.size();
    }

    private static int resetProgress(ServerPlayer player) {
        player.setAttached(PHOTOGRAPHED, new LinkedHashSet<>());
        player.sendSystemMessage(Component.translatable("text.photosafari.reset"));
        return 1;
    }

    /// All species that count, including the ones other mods add.

    public static int countSpecies() {
        int count = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (PhotoScan.isWildlifeType(type)) {
                count++;
            }
        }

        return count;
    }
}
