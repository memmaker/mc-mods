package dev.explorercraft.stealthandalert;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/// The two things that must hold or the whole mod is a lie: a mob sees what is in front of it,
/// and stops seeing it the moment something solid is in the way.
public class StealthGameTest {
    @GameTest
    public void playerInFrontIsSeen(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 1));
        ServerPlayer player = playerAt(helper, 2, 2, 5);
        zombie.getLookControl().setLookAt(player);
        zombie.setYRot(zombie.getYHeadRot());

        if (!Perception.hasLineOfSight(zombie, player, 1.0)) {
            throw helper.assertionException("zombie could not see a player standing right in front of it");
        }
        helper.succeed();
    }

    @GameTest
    public void playerBehindAWallIsNotSeen(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 1));
        ServerPlayer player = playerAt(helper, 2, 2, 5);
        zombie.getLookControl().setLookAt(player);
        zombie.setYRot(zombie.getYHeadRot());

        for (int x = 0; x < 5; x++) {
            for (int y = 1; y < 5; y++) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
            }
        }

        if (Perception.hasLineOfSight(zombie, player, 1.0)) {
            throw helper.assertionException("zombie saw a player through a stone wall");
        }
        helper.succeed();
    }

    @GameTest
    public void playerBehindTheMobIsNotSeen(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 5));
        ServerPlayer player = playerAt(helper, 2, 2, 1);
        zombie.setYRot(0.0F);
        zombie.setYHeadRot(0.0F);
        zombie.setXRot(0.0F);

        if (Perception.hasLineOfSight(zombie, player, 1.0)) {
            throw helper.assertionException("zombie saw a player standing behind its back");
        }
        helper.succeed();
    }

    /// A visible player fills the awareness bar and eventually gets locked onto.
    @GameTest
    public void awarenessFillsUpToTracking(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 1));
        ServerPlayer player = playerAt(helper, 2, 2, 5);

        float level = 0.0F;
        int reaction = StealthConfig.REACTION_TICKS;
        int state = AlertData.UNTRACKED;

        for (int tick = 0; tick < 200 && state != AlertData.TRACKING; tick++) {
            StealthEngine.IndividualResult result = StealthEngine.updateIndividual(
                    player, zombie, AlertData.IDLE, level, reaction, state, 0, true, 1.0);
            level = result.level();
            reaction = result.reaction();
            state = result.pState();
        }

        if (state != AlertData.TRACKING) {
            throw helper.assertionException("awareness stalled at " + level + " instead of locking on");
        }

        // Out of sight, the same bar has to drain again.
        StealthEngine.IndividualResult lost = StealthEngine.updateIndividual(
                player, zombie, AlertData.FIGHTING, 100.0F, 0, AlertData.AWARE, 0, false, 1.0);
        if (lost.level() >= 100.0F) {
            throw helper.assertionException("awareness did not decay once the player was out of sight");
        }

        helper.succeed();
    }

    /// A noise in the open reaches a mob; the same noise through stone does not.
    @GameTest
    public void noiseCarriesButWallsEatIt(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 1));
        ServerPlayer player = playerAt(helper, 2, 2, 5);

        Acoustics.emit(helper.getLevel(), player.position(), player, 50.0, 9.0, AlertSoundData.LOW);
        double inTheOpen = zombie.getAttachedOrCreate(StealthAndAlert.SOUND).score();
        if (inTheOpen <= 0) {
            throw helper.assertionException("zombie heard nothing from four blocks away in the open");
        }

        zombie.setAttached(StealthAndAlert.SOUND, AlertSoundData.NONE);
        for (int x = 0; x < 5; x++) {
            for (int y = 1; y < 5; y++) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
            }
        }

        Acoustics.emit(helper.getLevel(), player.position(), player, 50.0, 9.0, AlertSoundData.LOW);
        double throughWall = zombie.getAttachedOrCreate(StealthAndAlert.SOUND).score();
        if (throughWall >= inTheOpen) {
            throw helper.assertionException("stone wall did not muffle the noise: " + throughWall + " vs " + inTheOpen);
        }

        helper.succeed();
    }

    /// A blade in the back of an unaware mob hits far harder than the swing itself.
    @GameTest
    public void assassinationMultipliesDamageFromBehind(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 4));
        zombie.setYRot(0.0F);
        zombie.setYHeadRot(0.0F);

        ServerPlayer player = playerAt(helper, 2, 2, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(StealthItems.DAGGER));

        float before = zombie.getHealth();
        Assassination.tryAfterHit(zombie, player, 3.0F, zombie.getAttachedOrCreate(StealthAndAlert.ALERT));
        float dealt = before - zombie.getHealth();

        if (dealt < 3.0F) {
            throw helper.assertionException("strike from behind added only " + dealt + " damage");
        }

        // Facing the player, the same strike is just a strike.
        Zombie facing = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 4));
        facing.setYRot(180.0F);
        facing.setYHeadRot(180.0F);
        float healthy = facing.getHealth();
        Assassination.tryAfterHit(facing, player, 3.0F, facing.getAttachedOrCreate(StealthAndAlert.ALERT));
        if (facing.getHealth() < healthy) {
            throw helper.assertionException("a mob looking straight at the player was still assassinated");
        }

        helper.succeed();
    }

    /// The symbol has to sit on the mob, so the camera basis has to be the right way round.
    @GameTest
    public void headSymbolsProjectOntoTheMob(GameTestHelper helper) {
        Vec3 camera = new Vec3(0, 64, 0);
        int width = 200;
        int height = 100;

        // Looking straight down +Z at yaw 0: a point dead ahead lands in the middle of the screen.
        ScreenProjection.Point ahead = ScreenProjection.project(camera, 0F, 0F, 70.0, width, height, new Vec3(0, 64, 10));
        if (ahead == null || Math.abs(ahead.x() - 100) > 0.001 || Math.abs(ahead.y() - 50) > 0.001) {
            throw helper.assertionException("point straight ahead did not land in the screen centre: " + ahead);
        }

        ScreenProjection.Point above = ScreenProjection.project(camera, 0F, 0F, 70.0, width, height, new Vec3(0, 66, 10));
        if (above == null || above.y() >= ahead.y()) {
            throw helper.assertionException("a point higher in the world drew lower on the screen");
        }

        // Facing +Z, the camera's right hand points at -X.
        ScreenProjection.Point right = ScreenProjection.project(camera, 0F, 0F, 70.0, width, height, new Vec3(-2, 64, 10));
        if (right == null || right.x() <= ahead.x()) {
            throw helper.assertionException("a point to the camera's right drew to its left");
        }

        if (ScreenProjection.project(camera, 0F, 0F, 70.0, width, height, new Vec3(0, 64, -10)) != null) {
            throw helper.assertionException("a point behind the camera was projected onto the screen");
        }

        helper.succeed();
    }

    private static ServerPlayer playerAt(GameTestHelper helper, int x, int y, int z) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        return player;
    }
}
