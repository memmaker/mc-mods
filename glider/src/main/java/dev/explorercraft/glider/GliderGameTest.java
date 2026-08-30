package dev.explorercraft.glider;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;

/// The debug command has to actually be there, and actually hand over a glider.
public class GliderGameTest {
    @GameTest
    public void debugCommandGivesGlider(GameTestHelper helper) {
        var server = helper.getLevel().getServer();

        if (server.getCommands().getDispatcher().getRoot().getChild(GliderMod.MOD_ID) == null) {
            throw helper.assertionException("/" + GliderMod.MOD_ID + " is not registered");
        }

        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), GliderMod.MOD_ID + " give");

        ItemStack given = player.getInventory().getItem(0);

        if (given.getItem() != GliderMod.GLIDER) {
            throw helper.assertionException("command gave " + given + " instead of a glider");
        }

        helper.succeed();
    }
}
