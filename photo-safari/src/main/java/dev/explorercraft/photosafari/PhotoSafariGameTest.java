package dev.explorercraft.photosafari;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.CameraItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
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

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
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

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
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
        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);

        PhotoSafari.handlePhotograph(player, new PhotographPayload(List.of(bear.getId())));

        if (!player.getAttachedOrCreate(PhotoSafari.PHOTOGRAPHED).contains(id)) {
            throw helper.assertionException("photographing " + id + " was not credited");
        }

        helper.succeed();
    }

    /// The species-count milestone: 10 distinct species photographed in one go earns exactly
    /// one Eye of Ender.
    @GameTest
    public void tenDistinctSpeciesGrantsAnEyeOfEnder(GameTestHelper helper) {
        EntityType<?>[] types = {
                EntityTypes.COW, EntityTypes.PIG, EntityTypes.SHEEP, EntityTypes.CHICKEN, EntityTypes.RABBIT,
                EntityTypes.WOLF, EntityTypes.CAT, EntityTypes.FOX, EntityTypes.PANDA, EntityTypes.HORSE,
        };
        int[] xs = {0, 1, 2, 3, 4, 0, 1, 2, 3, 4};
        int[] zs = {3, 3, 3, 3, 3, 4, 4, 4, 4, 4};

        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            ids.add(helper.spawn(types[i], new BlockPos(xs[i], 2, zs[i])).getId());
        }

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);

        PhotoSafari.handlePhotograph(player, new PhotographPayload(ids));

        Set<Identifier> seen = player.getAttachedOrCreate(PhotoSafari.PHOTOGRAPHED);
        if (seen.size() != 10) {
            throw helper.assertionException("expected 10 distinct species recorded, got " + seen.size());
        }

        if (player.getInventory().countItem(Items.ENDER_EYE) != 1) {
            throw helper.assertionException("10 distinct species should have granted exactly one Eye of Ender");
        }

        helper.succeed();
    }

    /// Loot mode grants loot, never advancement credit or the species-milestone reward — the
    /// two systems are unrelated on purpose.
    @GameTest
    public void lootModeNeverCreditsSpeciesOrTheMilestoneReward(GameTestHelper helper) {
        EntityType<?>[] types = {
                EntityTypes.COW, EntityTypes.PIG, EntityTypes.SHEEP, EntityTypes.CHICKEN, EntityTypes.RABBIT,
                EntityTypes.WOLF, EntityTypes.CAT, EntityTypes.FOX, EntityTypes.PANDA, EntityTypes.HORSE,
        };
        int[] xs = {0, 1, 2, 3, 4, 0, 1, 2, 3, 4};
        int[] zs = {3, 3, 3, 3, 3, 4, 4, 4, 4, 4};

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);

        for (int i = 0; i < types.length; i++) {
            Entity mob = helper.spawn(types[i], new BlockPos(xs[i], 2, zs[i]));
            giveActiveCamera(player);
            PhotoSafari.handleLoot(player, new LootPayload(List.of(mob.getId())));
        }

        if (!player.getAttachedOrCreate(PhotoSafari.PHOTOGRAPHED).isEmpty()) {
            throw helper.assertionException("loot mode should never touch the photographed species set");
        }

        if (player.getInventory().countItem(Items.ENDER_EYE) != 0) {
            throw helper.assertionException("loot mode should never grant the species-milestone reward");
        }

        helper.succeed();
    }

    /// Loot mode: the mob survives, its drops land in the inventory, and the camera goes
    /// through the same deactivate-on-trigger motion as a real photo.
    @GameTest
    public void lootModeGrantsMobDropsWithoutKillingIt(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);
        giveActiveCamera(player);

        PhotoSafari.handleLoot(player, new LootPayload(List.of(cow.getId())));

        if (cow.isRemoved()) {
            throw helper.assertionException("looting a mob should not kill it");
        }

        if (player.getInventory().countItem(Items.BEEF) == 0) {
            throw helper.assertionException("looting the cow put no beef in the inventory");
        }

        if (CameraItem.isActive(player.getItemInHand(InteractionHand.MAIN_HAND))) {
            throw helper.assertionException("camera should deactivate after a loot trigger, same as a photo");
        }

        helper.succeed();
    }

    /// The 100-mob cooldown ring: the same mob instance is blocked on the very next trigger.
    @GameTest
    public void lootModeBlocksTheSameMobUntilCooldownClears(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);

        giveActiveCamera(player);
        PhotoSafari.handleLoot(player, new LootPayload(List.of(cow.getId())));
        int afterFirst = player.getInventory().countItem(Items.BEEF);

        giveActiveCamera(player);
        PhotoSafari.handleLoot(player, new LootPayload(List.of(cow.getId())));
        int afterSecond = player.getInventory().countItem(Items.BEEF);

        if (afterSecond != afterFirst) {
            throw helper.assertionException("looting the same mob twice in a row should be blocked by the cooldown");
        }

        if (!player.getAttachedOrCreate(PhotoSafari.RECENTLY_LOOTED).contains(cow.getUUID())) {
            throw helper.assertionException("looted mob was not recorded in the cooldown list");
        }

        helper.succeed();
    }

    /// Two fresh mobs in one frame: only the first one gets looted, not both.
    @GameTest
    public void lootModeOnlyLootsTheFirstUncooledMobInFrame(GameTestHelper helper) {
        Cow first = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));
        Cow second = helper.spawn(EntityTypes.COW, new BlockPos(3, 2, 4));

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);
        giveActiveCamera(player);

        PhotoSafari.handleLoot(player, new LootPayload(List.of(first.getId(), second.getId())));

        var recentlyLooted = player.getAttachedOrCreate(PhotoSafari.RECENTLY_LOOTED);
        boolean firstLooted = recentlyLooted.contains(first.getUUID());
        boolean secondLooted = recentlyLooted.contains(second.getUUID());

        if (firstLooted == secondLooted) {
            throw helper.assertionException(
                    "exactly one of the two framed mobs should have been looted, got first=" + firstLooted
                            + " second=" + secondLooted);
        }

        helper.succeed();
    }

    /// Worn equipment normally drops behind a protected method with no consumer hook, so it
    /// gets vacuumed out of the world instead: this checks it actually reaches the inventory
    /// and nothing is left lying around.
    @GameTest
    public void lootModeAlsoVacuumsWornEquipment(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 4));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setGuaranteedDrop(EquipmentSlot.HEAD);

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
        player.snapTo(eyeAt(helper, 2, 1, 1), 0.0f, 0.0f);
        giveActiveCamera(player);

        PhotoSafari.handleLoot(player, new LootPayload(List.of(zombie.getId())));

        if (player.getInventory().countItem(Items.IRON_HELMET) == 0) {
            throw helper.assertionException("worn equipment was not vacuumed into the inventory");
        }

        if (!helper.getLevel().getEntitiesOfClass(ItemEntity.class, zombie.getBoundingBox().inflate(2.0)).isEmpty()) {
            throw helper.assertionException("a dropped item was left behind in the world");
        }

        helper.succeed();
    }

    private static void giveActiveCamera(ServerPlayer player) {
        ItemStack camera = new ItemStack(Camerapture.CAMERA);
        CameraItem.setActive(camera, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, camera);
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

        ServerPlayer player = mockPlayer(helper, GameType.CREATIVE);
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), PhotoSafari.MOD_ID + " camera 3");

        if (player.getInventory().getItem(0).isEmpty()) {
            throw helper.assertionException("command handed over nothing");
        }

        helper.succeed();
    }

    /// A mock player with a working (embedded) network connection, unlike
    /// {@code makeMockServerPlayer}: real code paths send it packets — chat and overlay
    /// messages, cooldown and container syncs, ride teleports — and a player with a null
    /// connection throws on every one of them.
    /// ponytail: makeMockServerPlayerInLevel is deprecated for removal with nothing to
    /// replace it, and it is still the only helper that hands back a usable player. When it
    /// goes, build one here instead: a ServerPlayer plus a ServerGamePacketListenerImpl over
    /// a Connection with an EmbeddedChannel, registered through PlayerList.placeNewPlayer —
    /// which is all this method does today.
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(gameType);
        return player;
    }
}
