package dev.explorercraft.pistolsilencer;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public class PistolSilencerGameTest {
    /// The three recipes are hand-written JSON, so a typo in an ingredient ID or a pattern key
    /// would only show up as an item that quietly cannot be crafted. Running the real grids
    /// through the server's own recipe lookup is what catches that.
    @GameTest
    public void pistolIsCraftable(GameTestHelper helper) {
        ItemStack result = craft(helper, 2, 2, List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.REDSTONE), new ItemStack(Items.FLINT)));

        if (!result.is(PistolSilencer.PISTOL)) {
            throw helper.assertionException("the pistol grid crafted " + result);
        }
        helper.succeed();
    }

    @GameTest
    public void silencerIsCraftable(GameTestHelper helper) {
        ItemStack result = craft(helper, 3, 2, List.of(
                new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.IRON_NUGGET),
                new ItemStack(Items.WOOL.white()), new ItemStack(Items.WOOL.white()), new ItemStack(Items.WOOL.white())));

        if (!result.is(PistolSilencer.SILENCER)) {
            throw helper.assertionException("the silencer grid crafted " + result);
        }
        helper.succeed();
    }

    @GameTest
    public void ammoIsCraftableEightAtATime(GameTestHelper helper) {
        ItemStack result = craft(helper, 3, 1, List.of(
                new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.GUNPOWDER)));

        if (!result.is(PistolSilencer.PISTOL_AMMO) || result.getCount() != 8) {
            throw helper.assertionException("the ammo grid crafted " + result);
        }
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, int width, int height, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(width, height, grid);
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> helper.assertionException("no recipe matched the grid"));
        return recipe.value().assemble(input);
    }
}
