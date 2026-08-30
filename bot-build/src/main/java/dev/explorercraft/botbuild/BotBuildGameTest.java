package dev.explorercraft.botbuild;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BotBuildGameTest {
    @GameTest
    public void planSkipsBlocksThatAreAlreadyThere(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);

        List<BlockPos> targets = BotBuild.plan(helper.getLevel(),
                helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(1, 1, 3)));

        if (targets.size() != 1 || !targets.getFirst().equals(helper.absolutePos(new BlockPos(1, 1, 3)))) {
            throw helper.assertionException("expected only the one empty spot, got " + targets);
        }

        helper.succeed();
    }

    @GameTest
    public void takeOnePullsASingleBlockOutOfAChest(GameTestHelper helper) {
        Container chest = fillChest(helper, new BlockPos(1, 1, 1), 4);

        if (!BotBuild.takeOne(chest, Items.STONE) || chest.countItem(Items.STONE) != 3) {
            throw helper.assertionException("expected 3 stone left, got " + chest.countItem(Items.STONE));
        }
        if (BotBuild.takeOne(chest, Items.DIAMOND)) {
            throw helper.assertionException("took an item the chest never had");
        }

        helper.succeed();
    }

    /// The whole loop: outline two spots, let the bot ferry stone out of the chest, and wait for
    /// real blocks to show up where the ghosts were.
    // Padding keeps the neighbouring test's bots outside this one's 24-block call radius.
    @GameTest(maxTicks = 600, padding = 32)
    public void botBuildsTheOutlineFromANearbyChest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        fillChest(helper, new BlockPos(0, 1, 0), 4);
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        // A wall across the middle: the bot has to path around it, not through it.
        for (int y = 1; y <= 3; y++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(3, y, z), Blocks.STONE);
            }
        }
        BotBuild.spawnBot(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));

        BlockPos first = helper.absolutePos(new BlockPos(6, 1, 6));
        BlockPos second = helper.absolutePos(new BlockPos(6, 1, 4));
        BotBuild.startJob(level, player, first, second);

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 5));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 4));
        });
    }

    /// Every bot in range takes the job, and each one works a ghost of its own.
    @GameTest(maxTicks = 600, padding = 32)
    public void everyBotInRangeGetsAGhostOfItsOwn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        fillChest(helper, new BlockPos(0, 1, 0), 8);
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        for (int i = 0; i < 3; i++) {
            BotBuild.spawnBot(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1 + i, 1, 1))));
        }

        BlockPos site = helper.absolutePos(new BlockPos(6, 1, 6));
        if (BotBuild.claimBots(level, site).size() != 3) {
            throw helper.assertionException("expected all 3 bots to be free, got " + BotBuild.claimBots(level, site).size());
        }

        BotBuild.startJob(level, player, site, helper.absolutePos(new BlockPos(6, 1, 3)));

        if (!BotBuild.claimBots(level, site).isEmpty()) {
            throw helper.assertionException("the job should have taken every bot in range");
        }

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 5));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 4));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 3));
        });
    }

    /// An idle bot goes and eats what is lying around.
    @GameTest(maxTicks = 400, padding = 32)
    public void idleBotEatsDroppedFood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BotBuild.spawnBot(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
        BlockPos drop = helper.absolutePos(new BlockPos(6, 1, 6));
        level.addFreshEntity(new ItemEntity(level, drop.getX() + 0.5, drop.getY(), drop.getZ() + 0.5,
                new ItemStack(Items.BREAD)));

        helper.succeedWhen(() -> {
            if (!level.getEntities(EntityTypes.ITEM, new AABB(drop).inflate(BotBuild.FOOD_RANGE * 2), item -> true).isEmpty()) {
                throw helper.assertionException("the bread is still on the ground");
            }
        });
    }

    private static Container fillChest(GameTestHelper helper, BlockPos relative, int stone) {
        helper.setBlock(relative, Blocks.CHEST);
        Container chest = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        chest.setItem(0, new ItemStack(Items.STONE, stone));
        return chest;
    }
}
