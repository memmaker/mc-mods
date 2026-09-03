package dev.explorercraft.photosafari;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.explorercraft.photosafari.mixin.MobDropAccessor;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.CameraItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PhotoSafari implements ModInitializer {
    public static final String MOD_ID = "photosafari";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /// Species this player has on film. Survives death, saved with the player.
    public static final AttachmentType<Set<Identifier>> PHOTOGRAPHED = AttachmentRegistry.create(
            id("photographed"),
            builder -> builder
                    .persistent(Identifier.CODEC.listOf().xmap(LinkedHashSet::new, List::copyOf))
                    .initializer(LinkedHashSet::new)
                    .copyOnDeath());

    public static SpeciesPhotographedTrigger photographedTrigger;

    /// Every this many distinct species photographed (photograph mode only — loot mode
    /// never touches PHOTOGRAPHED) earns one Eye of Ender.
    public static final int SPECIES_MILESTONE_INTERVAL = 10;

    /// The 100 mobs (by UUID) each player most recently looted in peaceful loot mode, oldest
    /// first. Looting the same mob again means cycling 99 others through this first. Synced
    /// to its owner only, so the client can colour the loot-mode outline red/green.
    public static final int RECENT_LOOT_CAP = 100;
    public static final AttachmentType<List<UUID>> RECENTLY_LOOTED = AttachmentRegistry.create(
            id("recently_looted"),
            builder -> builder
                    .persistent(UUIDUtil.CODEC.listOf())
                    .initializer(List::of)
                    .copyOnDeath()
                    .syncWith(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(RECENT_LOOT_CAP)),
                            AttachmentSyncPredicate.targetOnly()));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PhotoSafariConfig.load();

        photographedTrigger = Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                id("species_photographed"), new SpeciesPhotographedTrigger());

        PayloadTypeRegistry.serverboundPlay().register(PhotographPayload.TYPE, PhotographPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PhotographPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handlePhotograph(context.player(), payload)));

        PayloadTypeRegistry.serverboundPlay().register(LootPayload.TYPE, LootPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(LootPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleLoot(context.player(), payload)));

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
        List<Entity> newlyPhotographed = new ArrayList<>();

        for (int entityId : payload.entityIds()) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || !PhotoScan.isWildlife(entity)) {
                continue;
            }

            // The client decides what made it onto the picture, so never trust it: verify here.
            if (!PhotoScan.isPhotographedLenient(level, eye, look, entity)) {
                continue;
            }

            if (seen.add(EntityType.getKey(entity.getType()))) {
                newlyPhotographed.add(entity);
            }
        }

        // Persist before any notification: a chat/overlay message is best-effort and must
        // never be able to cost the player credit for what was already photographed.
        if (seen.size() != before) {
            player.setAttached(PHOTOGRAPHED, seen);

            int milestonesCrossed = seen.size() / SPECIES_MILESTONE_INTERVAL - before / SPECIES_MILESTONE_INTERVAL;
            if (milestonesCrossed > 0) {
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.ENDER_EYE, milestonesCrossed), false);
                player.sendSystemMessage(Component.translatable("text.photosafari.milestone_reward", seen.size())
                        .withStyle(ChatFormatting.AQUA));
            }
        }

        int runningCount = before;
        for (Entity entity : newlyPhotographed) {
            runningCount++;
            player.sendOverlayMessage(Component.translatable("text.photosafari.new_species",
                    entity.getType().getDescription()).withStyle(ChatFormatting.GREEN));
            photographedTrigger.trigger(player, runningCount, EntityType.getKey(entity.getType()));
        }

        // Fired on every photo, so count-based advancements are re-checked even without a new species.
        photographedTrigger.trigger(player, seen.size(), null);
    }

    /// Loot mode: same in-frame detection as a photograph, but instead of a picture the
    /// first framed mob not currently on cooldown has its death loot go straight into the
    /// player's inventory — one mob per trigger, same as one photo per trigger. Never trust
    /// the client's entity list, and never touch it without the server-side config gate.
    public static void handleLoot(ServerPlayer player, LootPayload payload) {
        if (!PhotoSafariConfig.peacefulLoot) {
            return;
        }

        CameraItem.HeldCamera camera = CameraItem.find(player, true);
        if (camera == null) {
            return;
        }

        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        DamageSource source = level.damageSources().playerAttack(player);

        List<UUID> recentlyLooted = new ArrayList<>(player.getAttachedOrCreate(RECENTLY_LOOTED));
        boolean anyInFrame = false;
        boolean looted = false;

        for (int entityId : payload.entityIds()) {
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof Mob mob) || !PhotoScan.isWildlife(mob)) {
                continue;
            }

            if (!PhotoScan.isPhotographedLenient(level, eye, look, mob)) {
                continue;
            }

            anyInFrame = true;
            if (recentlyLooted.contains(mob.getUUID())) {
                continue;
            }

            grantLoot(level, player, mob, source);
            recentlyLooted.add(mob.getUUID());
            looted = true;
            break;
        }

        // Nothing real was in frame: the trigger press is a no-op, not even the camera motions.
        if (!anyInFrame) {
            return;
        }

        if (looted) {
            while (recentlyLooted.size() > RECENT_LOOT_CAP) {
                recentlyLooted.remove(0);
            }
            player.setAttached(RECENTLY_LOOTED, recentlyLooted);
        }

        // Same shutter, cooldown and swing as a real photo, minus the paper: nothing was saved.
        level.playSound(null, player, Camerapture.CAMERA_SHUTTER, SoundSource.PLAYERS, 1.0f, 1.0f);
        CameraItem.setActive(camera.stack(), false);
        player.getCooldowns().addCooldown(camera.stack(), 60);
        player.swing(camera.hand(), true);
    }

    private static void grantLoot(ServerLevel level, ServerPlayer player, Mob mob, DamageSource source) {
        // A single trigger can grant several stacks across several mobs; skip the per-stack
        // sync packet placeItemBackInInventory defaults to, the player's own inventory syncs
        // every tick regardless.
        Optional<ResourceKey<LootTable>> lootTable = mob.getLootTable();
        if (lootTable.isPresent()) {
            mob.dropFromLootTable(level, source, true, lootTable.get(),
                    stack -> player.getInventory().placeItemBackInInventory(stack, false));
        }

        // Worn equipment (and any mob-specific bonus drops) only exist behind the protected
        // dropCustomDeathLoot, which has no consumer to redirect. Let it spawn for real, same
        // tick, then vacuum whatever appeared before the world ever renders it.
        AABB dropBox = mob.getBoundingBox().inflate(1.0);
        Set<Integer> before = level.getEntitiesOfClass(ItemEntity.class, dropBox).stream()
                .map(Entity::getId).collect(Collectors.toSet());
        ((MobDropAccessor) mob).photosafari$dropCustomDeathLoot(level, source, true);
        for (ItemEntity dropped : level.getEntitiesOfClass(ItemEntity.class, dropBox,
                item -> !before.contains(item.getId()))) {
            player.getInventory().placeItemBackInInventory(dropped.getItem(), false);
            dropped.discard();
        }

        if (mob.shouldDropExperience()) {
            ExperienceOrb.award(level, mob.position(), mob.getExperienceReward(level, player));
        }
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
