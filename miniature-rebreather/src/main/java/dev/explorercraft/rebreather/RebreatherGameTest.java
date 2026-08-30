package dev.explorercraft.rebreather;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

/// Drowns two identical animals side by side, one bare-headed and one in a rebreather, and checks
/// that only the bare one loses air. Guards the whole chain: the equippable component putting the
/// item in the head slot, the equipment tick reaching it, and the refill itself.
///
/// Cows stand in for the player because the refill runs off the worn stack, which every
/// LivingEntity ticks the same way. They are spawned without AI so they stay put and the only
/// thing changing across the test is their air.
public class RebreatherGameTest {
    private static final int SUBMERGED_TICKS = 40;

    @GameTest(maxTicks = 100)
    public void rebreatherKeepsAirFullUnderwater(GameTestHelper helper) {
        flood(helper);

        LivingEntity bare = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 1, 2));
        LivingEntity masked = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(5, 1, 5));
        masked.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Rebreather.REBREATHER));

        helper.startSequence()
                .thenIdle(SUBMERGED_TICKS)
                .thenExecute(() -> {
                    if (bare.getAirSupply() >= bare.getMaxAirSupply()) {
                        throw helper.assertionException("a bare-headed cow held its breath for "
                                + SUBMERGED_TICKS + " ticks underwater, so the test proves nothing");
                    }

                    if (masked.getAirSupply() != masked.getMaxAirSupply()) {
                        throw helper.assertionException("the rebreather let the air bar drop to "
                                + masked.getAirSupply() + " of " + masked.getMaxAirSupply());
                    }
                })
                .thenSucceed();
    }

    /// The rebreather only works from the head slot: carried in a pocket it must do nothing.
    @GameTest(maxTicks = 100)
    public void carriedRebreatherDoesNothing(GameTestHelper helper) {
        flood(helper);

        LivingEntity carrying = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 1, 2));
        carrying.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Rebreather.REBREATHER));

        helper.startSequence()
                .thenIdle(SUBMERGED_TICKS)
                .thenExecute(() -> {
                    if (carrying.getAirSupply() >= carrying.getMaxAirSupply()) {
                        throw helper.assertionException(
                                "a rebreather held in hand refilled the air bar anyway");
                    }
                })
                .thenSucceed();
    }

    @GameTest
    public void kelpAndNuggetsCraftIntoARebreather(GameTestHelper helper) {
        ItemStack nugget = new ItemStack(Items.IRON_NUGGET);
        ItemStack kelp = new ItemStack(Items.DRIED_KELP);
        ItemStack leather = new ItemStack(Items.LEATHER);
        ItemStack crafted = craft(helper, 3, 2,
                List.of(nugget, kelp, nugget, ItemStack.EMPTY, leather, ItemStack.EMPTY));

        if (!crafted.is(Rebreather.REBREATHER)) {
            throw helper.assertionException("the rebreather recipe crafted " + crafted);
        }

        helper.succeed();
    }

    /// Fills the bottom of the test box with water, deep enough that a cow standing on the floor
    /// has its eyes under the surface for the whole run.
    private static void flood(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    /// Runs the grid through the server's own recipe lookup, so a typo in the recipe file shows up
    /// as nothing crafted rather than as a mod that quietly ships an uncraftable item.
    private static ItemStack craft(GameTestHelper helper, int width, int height, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(width, height, grid);
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> helper.assertionException("no recipe matched the grid"));
        return recipe.value().assemble(input);
    }

    /// The debug command has to actually be there, and actually hand over a rebreather.
    @GameTest
    public void debugCommandGivesRebreather(GameTestHelper helper) {
        var server = helper.getLevel().getServer();

        if (server.getCommands().getDispatcher().getRoot().getChild(Rebreather.MOD_ID) == null) {
            throw helper.assertionException("/" + Rebreather.MOD_ID + " is not registered");
        }

        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), Rebreather.MOD_ID + " give");

        ItemStack given = player.getInventory().getItem(0);

        if (given.getItem() != Rebreather.REBREATHER) {
            throw helper.assertionException("command gave " + given + " instead of a rebreather");
        }

        helper.succeed();
    }

}
