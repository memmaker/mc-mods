package dev.explorercraft.whiteflag;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/// The debug command has to actually be there, and actually hand over a flag that the
/// mob-targeting check then recognises in the hotbar.
public class WhiteFlagGameTest {
    @GameTest
    public void debugCommandGivesFlag(GameTestHelper helper) {
        var server = helper.getLevel().getServer();

        if (server.getCommands().getDispatcher().getRoot().getChild(WhiteFlag.MOD_ID) == null) {
            throw helper.assertionException("/" + WhiteFlag.MOD_ID + " is not registered");
        }

        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), WhiteFlag.MOD_ID);

        ItemStack given = player.getInventory().getItem(0);

        if (given.getItem() != WhiteFlag.WHITE_FLAG) {
            throw helper.assertionException("command gave " + given + " instead of a flag");
        }

        if (!WhiteFlag.carriesFlag(player)) {
            throw helper.assertionException("the flag it gave is not seen in the hotbar");
        }

        helper.succeed();
    }

    /// Bone meal bleaches dyed wool back to white. Vanilla already covers the white-dye half of
    /// this, so only the bone meal variant lives here.
    @GameTest
    public void boneMealBleachesWool(GameTestHelper helper) {
        CraftingInput input = CraftingInput.of(2, 1, List.of(
                new ItemStack(Items.BONE_MEAL), new ItemStack(Items.WOOL.pick(DyeColor.BLACK))));

        ItemStack result = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElseThrow(() -> helper.assertionException("bone meal does not bleach wool"));

        if (result.getItem() != Items.WOOL.white()) {
            throw helper.assertionException("bleaching gave " + result + " instead of white wool");
        }

        // White wool in means nothing to bleach: the recipe must not burn the bone meal.
        CraftingInput alreadyWhite = CraftingInput.of(2, 1, List.of(
                new ItemStack(Items.BONE_MEAL), new ItemStack(Items.WOOL.white())));

        if (helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, alreadyWhite, helper.getLevel()).isPresent()) {
            throw helper.assertionException("white wool should not be bleachable");
        }

        helper.succeed();
    }
}
