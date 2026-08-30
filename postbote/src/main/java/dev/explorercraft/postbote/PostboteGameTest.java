package dev.explorercraft.postbote;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Optional;
import java.util.UUID;

/// The debug command has to be there and hand over a satchel; the order guard, distance gate,
/// tracking hand-off and payout are pure server logic that doesn't need a generated village to
/// exercise, so they're tested directly instead of chasing a real locateStructure result.
public class PostboteGameTest {
    @GameTest
    public void debugCommandGivesSatchel(GameTestHelper helper) {
        var server = helper.getLevel().getServer();

        if (server.getCommands().getDispatcher().getRoot().getChild(Postbote.MOD_ID) == null) {
            throw helper.assertionException("/" + Postbote.MOD_ID + " is not registered");
        }

        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), Postbote.MOD_ID);

        ItemStack given = player.getInventory().getItem(0);
        if (given.getItem() != Postbote.SATCHEL) {
            throw helper.assertionException("command gave " + given + " instead of a satchel");
        }

        helper.succeed();
    }

    @GameTest
    public void teleportCommandDropsPlayerWithinRadiusOfTarget(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        BlockPos target = player.blockPosition().offset(40, 0, 40);
        helper.getLevel().getChunk(target);
        giveOrder(player, target, 10, Optional.empty());

        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), Postbote.MOD_ID + " teleport");

        double dx = player.blockPosition().getX() - target.getX();
        double dz = player.blockPosition().getZ() - target.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance > Postbote.TELEPORT_RADIUS + 1) {
            throw helper.assertionException("expected within %.0f blocks of the target, landed %.1f away"
                    .formatted(Postbote.TELEPORT_RADIUS, horizontalDistance));
        }

        helper.succeed();
    }

    @GameTest
    public void teleportCommandFailsWithoutAnActiveOrder(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        BlockPos before = player.blockPosition();

        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), Postbote.MOD_ID + " teleport");

        if (!player.blockPosition().equals(before)) {
            throw helper.assertionException("teleport should not move the player without an active delivery");
        }

        helper.succeed();
    }

    @GameTest
    public void activeOrderBlocksANewOne(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        giveOrder(player, player.blockPosition().offset(2000, 0, 0), 10, Optional.empty());

        if (Postbote.activeOrder(player) == null) {
            throw helper.assertionException("a compass carrying an order should count as an active order");
        }

        if (Postbote.startOrder(helper.getLevel(), player)) {
            throw helper.assertionException("startOrder should refuse while an order is already active");
        }

        long compasses = player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.getItem() == Postbote.COMPASS)
                .count();
        if (compasses != 1) {
            throw helper.assertionException("a blocked startOrder should not hand out a second compass, found " + compasses);
        }

        helper.succeed();
    }

    /// The fallback (no villager assigned yet) completion path only checks the player's own
    /// distance to the destination — it never looks at where the clicked villager actually is —
    /// so any villager will do here.
    @GameTest
    public void deliveryWithinRadiusPaysAndConsumes(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        ItemStack compass = giveOrder(player, player.blockPosition(), 12, Optional.empty());
        Villager anyVillager = helper.spawn(EntityTypes.VILLAGER, BlockPos.ZERO);
        int emeraldsBefore = countEmeralds(player);

        if (!Postbote.completeOrder(helper.getLevel(), player, compass, anyVillager)) {
            throw helper.assertionException("delivery at the destination should succeed");
        }
        if (!compass.isEmpty()) {
            throw helper.assertionException("compass should be consumed on delivery");
        }
        if (countEmeralds(player) != emeraldsBefore + 12) {
            throw helper.assertionException("expected 12 more emeralds, got " + (countEmeralds(player) - emeraldsBefore));
        }

        helper.succeed();
    }

    @GameTest
    public void deliveryFarFromDestinationFails(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        BlockPos farAway = player.blockPosition().offset(10_000, 0, 0);
        ItemStack compass = giveOrder(player, farAway, 12, Optional.empty());
        Villager anyVillager = helper.spawn(EntityTypes.VILLAGER, BlockPos.ZERO);

        if (Postbote.completeOrder(helper.getLevel(), player, compass, anyVillager)) {
            throw helper.assertionException("delivery far from the destination should fail");
        }
        if (compass.isEmpty()) {
            throw helper.assertionException("a failed delivery should not consume the compass");
        }

        helper.succeed();
    }

    @GameTest
    public void deliveryToTrackedVillagerSucceedsRegardlessOfDistance(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        Villager tracked = helper.spawn(EntityTypes.VILLAGER, BlockPos.ZERO);
        BlockPos farAway = player.blockPosition().offset(10_000, 0, 0);
        ItemStack compass = giveOrder(player, farAway, 12, Optional.of(tracked.getUUID()));
        int emeraldsBefore = countEmeralds(player);

        if (!Postbote.completeOrder(helper.getLevel(), player, compass, tracked)) {
            throw helper.assertionException("delivery to the tracked villager should succeed even far from the fallback point");
        }
        if (countEmeralds(player) != emeraldsBefore + 12) {
            throw helper.assertionException("expected 12 more emeralds, got " + (countEmeralds(player) - emeraldsBefore));
        }

        helper.succeed();
    }

    @GameTest
    public void deliveryToWrongVillagerFailsWhenOneIsTracked(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        Villager tracked = helper.spawn(EntityTypes.VILLAGER, BlockPos.ZERO);
        Villager bystander = helper.spawn(EntityTypes.VILLAGER, BlockPos.ZERO);
        ItemStack compass = giveOrder(player, player.blockPosition(), 12, Optional.of(tracked.getUUID()));

        if (Postbote.completeOrder(helper.getLevel(), player, compass, bystander)) {
            throw helper.assertionException("only the tracked villager should complete the delivery once one is assigned");
        }
        if (compass.isEmpty()) {
            throw helper.assertionException("a failed delivery should not consume the compass");
        }

        helper.succeed();
    }

    /// Unlike the tests above, {@code nearestAliveVillager} genuinely needs real relative
    /// distances between entities, so this one spawns them with GameTest's own structure-relative
    /// {@code helper.spawn(EntityType, BlockPos)} — the coordinates it hands back are guaranteed
    /// already loaded, unlike an arbitrary point out in the world.
    @GameTest
    public void trackingFindsAndFollowsNearestAliveVillager(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        Villager near = helper.spawn(EntityTypes.VILLAGER, new BlockPos(2, 1, 1));
        helper.spawn(EntityTypes.VILLAGER, new BlockPos(6, 1, 1));
        ItemStack compass = giveOrder(player, anchor, 10, Optional.empty());

        Postbote.updateTracking(helper.getLevel().getServer(), compass);

        PostboteOrder updated = compass.get(Postbote.ORDER);
        if (updated == null || updated.villager().isEmpty() || !updated.villager().get().equals(near.getUUID())) {
            throw helper.assertionException("tracking should pick the nearest villager to the destination");
        }

        helper.succeed();
    }

    @GameTest
    public void trackingFallsBackToNearestAliveWhenTrackedVillagerIsGone(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        Villager replacement = helper.spawn(EntityTypes.VILLAGER, new BlockPos(3, 1, 1));
        UUID goneVillager = UUID.randomUUID();
        ItemStack compass = giveOrder(player, anchor, 10, Optional.of(goneVillager));

        Postbote.updateTracking(helper.getLevel().getServer(), compass);

        PostboteOrder updated = compass.get(Postbote.ORDER);
        if (updated == null || updated.villager().isEmpty() || !updated.villager().get().equals(replacement.getUUID())) {
            throw helper.assertionException("tracking should re-home onto the nearest alive villager once the old one is gone");
        }

        helper.succeed();
    }

    /**
     * Regression test for the bug where right-clicking a villager with the satchel just opened
     * the trade screen: {@code Item#interactLivingEntity} never got a chance to stop it, since
     * {@code Villager#mobInteract} runs independently of it. This drives the actual
     * UseEntityCallback the mod is registered on, the same entry point the real click packet
     * goes through, and checks it returns something other than PASS — anything else cancels
     * vanilla's handling outright (see the mixin this event fires from).
     */
    @GameTest
    public void satchelClickOnVillagerPreemptsTrading(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        player.getInventory().setItem(0, new ItemStack(Postbote.SATCHEL));
        player.getInventory().setSelectedSlot(0);

        Villager villager = helper.spawn(EntityTypes.VILLAGER, BlockPos.ZERO);
        EntityHitResult hit = new EntityHitResult(villager, villager.position());

        InteractionResult result = UseEntityCallback.EVENT.invoker()
                .interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, villager, hit);

        if (result == InteractionResult.PASS) {
            throw helper.assertionException("satchel click on a villager must not fall through to PASS, or the trade screen wins");
        }

        helper.succeed();
    }

    /**
     * Places the compass directly in slot 0 instead of going through {@code player.addItem},
     * which drains the passed-in stack as it merges into inventory and would leave our reference
     * empty afterward.
     */
    private static ItemStack giveOrder(ServerPlayer player, BlockPos destination, int reward, Optional<UUID> villager) {
        ItemStack compass = new ItemStack(Postbote.COMPASS);
        compass.set(Postbote.ORDER, new PostboteOrder(GlobalPos.of(player.level().dimension(), destination), reward, villager));
        player.getInventory().setItem(0, compass);
        return compass;
    }

    private static int countEmeralds(ServerPlayer player) {
        return player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.getItem() == Items.EMERALD)
                .mapToInt(ItemStack::getCount)
                .sum();
    }
}
