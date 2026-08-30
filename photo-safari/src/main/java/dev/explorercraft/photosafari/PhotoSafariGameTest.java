package dev.explorercraft.photosafari;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/// End to end checks in a real world: a mob in the open counts, the same mob behind a
/// wall does not, and a verified photo grants the advancement.
public class PhotoSafariGameTest {
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final double FOV = 70.0;
    private static final double ASPECT = 16.0 / 9.0;

    @GameTest
    public void mobInTheOpenIsPhotographed(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));
        Vec3 eye = eyeAt(helper, 2, 2, 1);

        if (!PhotoScan.isPhotographed(helper.getLevel(), eye, SOUTH, FOV, ASPECT, cow)) {
            throw helper.assertionException("cow standing in the open was not photographed");
        }

        helper.succeed();
    }

    @GameTest
    public void mobBehindAWallIsNot(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));
        for (int x = 0; x < 5; x++) {
            for (int y = 1; y < 5; y++) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
            }
        }

        Vec3 eye = eyeAt(helper, 2, 2, 1);
        if (PhotoScan.isPhotographed(helper.getLevel(), eye, SOUTH, FOV, ASPECT, cow)) {
            throw helper.assertionException("cow hidden behind a stone wall was still photographed");
        }

        helper.succeed();
    }

    @GameTest
    public void mobBehindTheCameraIsNot(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 1));
        Vec3 eye = eyeAt(helper, 2, 2, 4);

        if (PhotoScan.isPhotographed(helper.getLevel(), eye, SOUTH, FOV, ASPECT, cow)) {
            throw helper.assertionException("cow behind the camera was photographed");
        }

        helper.succeed();
    }

    /// The whole server side: an untrusted client report turns into a tracked species
    /// and an earned advancement.
    @GameTest
    public void photographGrantsTheSpeciesAdvancement(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);

        PhotoSafari.handlePhotograph(player, new PhotographPayload(List.of(cow.getId())));

        Set<Identifier> seen = player.getAttachedOrCreate(PhotoSafari.PHOTOGRAPHED);
        if (!seen.contains(Identifier.withDefaultNamespace("cow"))) {
            throw helper.assertionException("cow was not recorded, got " + seen);
        }

        var advancement = helper.getLevel().getServer().getAdvancements()
                .get(PhotoSafari.id("species/minecraft/cow"));
        if (advancement == null) {
            throw helper.assertionException("the generated cow advancement is missing");
        }

        if (!player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            throw helper.assertionException("photographing a cow did not grant its advancement");
        }

        helper.succeed();
    }

    /// A client claiming it photographed something it cannot see gets nothing.
    @GameTest
    public void clientReportOfAHiddenMobIsRejected(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));
        for (int x = 0; x < 5; x++) {
            for (int y = 1; y < 5; y++) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
            }
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);

        PhotoSafari.handlePhotograph(player, new PhotographPayload(List.of(cow.getId())));

        Set<Identifier> seen = player.getAttachedOrCreate(PhotoSafari.PHOTOGRAPHED);
        if (!seen.isEmpty()) {
            throw helper.assertionException("server credited a mob the player could not see: " + seen);
        }

        helper.succeed();
    }

    /// Other mods name their spawn eggs their own way (Alex's Mobs writes
    /// spawn_egg_grizzly_bear), so check the runtime lookup against a real one when it is
    /// installed. Without the mod there is nothing to check.
    @GameTest
    public void moddedMobsAreRecognisedAndCredited(GameTestHelper helper) {
        Identifier id = Identifier.parse("alexsmobs:grizzly_bear");
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            helper.succeed();
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (!PhotoScan.isWildlifeType(type)) {
            throw helper.assertionException("modded mob " + id + " was not recognised as wildlife");
        }

        Entity bear = helper.spawn(type, new BlockPos(2, 2, 4));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);

        PhotoSafari.handlePhotograph(player, new PhotographPayload(List.of(bear.getId())));

        if (!player.getAttachedOrCreate(PhotoSafari.PHOTOGRAPHED).contains(id)) {
            throw helper.assertionException("photographing " + id + " was not credited");
        }

        helper.succeed();
    }

    private static Vec3 eyeAt(GameTestHelper helper, int x, int y, int z) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /// The debug commands have to actually be there, and actually hand over a camera.
    @GameTest
    public void debugCommandGivesCamera(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var root = server.getCommands().getDispatcher().getRoot().getChild(PhotoSafari.MOD_ID);

        if (root == null) {
            throw helper.assertionException("/" + PhotoSafari.MOD_ID + " is not registered");
        }

        for (String sub : List.of("camera", "progress", "reset")) {
            if (root.getChild(sub) == null) {
                throw helper.assertionException("/" + PhotoSafari.MOD_ID + " " + sub + " is missing");
            }
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), PhotoSafari.MOD_ID + " camera 3");

        if (player.getInventory().getItem(0).isEmpty()) {
            throw helper.assertionException("command handed over nothing");
        }

        helper.succeed();
    }

}
