package dev.explorercraft.nofall;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/// Hurts two identical animals with the same fall, one barefoot and one in plush boots, and
/// checks only the barefoot one loses health. Guards the attribute wiring end to end: a typo
/// in the modifier, the slot group or the material's attributes shows up here.
///
/// Cows stand in for the player because the boots work through an equipment attribute, which
/// every LivingEntity resolves the same way, and a spawned mob really ticks inside the test.
public class NoFallGameTest {
    private static final double FATAL_ISH_FALL = 20.0;

    @GameTest
    public void plushBootsRemoveFallDamage(GameTestHelper helper) {
        LivingEntity barefoot = helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 1));
        LivingEntity shod = helper.spawn(EntityTypes.COW, new BlockPos(3, 2, 3));
        shod.setItemSlot(EquipmentSlot.FEET, new ItemStack(NoFall.PLUSH_BOOTS));

        helper.startSequence()
                // Equipment attribute modifiers are applied on the wearer's next tick.
                .thenIdle(2)
                .thenExecute(() -> {
                    double multiplier = shod.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
                    if (multiplier != 0.0) {
                        throw helper.assertionException(
                                "boots left the fall damage multiplier at " + multiplier);
                    }

                    float barefootHealth = barefoot.getHealth();
                    float shodHealth = shod.getHealth();

                    DamageSource fall = barefoot.damageSources().fall();
                    barefoot.causeFallDamage(FATAL_ISH_FALL, 1.0F, fall);
                    shod.causeFallDamage(FATAL_ISH_FALL, 1.0F, fall);

                    if (barefoot.getHealth() >= barefootHealth) {
                        throw helper.assertionException("a barefoot cow survived a "
                                + FATAL_ISH_FALL + " block fall unhurt, so the test proves nothing");
                    }

                    if (shod.getHealth() != shodHealth) {
                        throw helper.assertionException("plush boots let through "
                                + (shodHealth - shod.getHealth()) + " fall damage");
                    }
                })
                .thenSucceed();
    }

    @GameTest
    public void woolCraftsIntoBootsOfThatColour(GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            ItemStack wool = new ItemStack(Items.WOOL.pick(color));
            ItemStack empty = ItemStack.EMPTY;
            ItemStack crafted = craft(helper, 3, 2, List.of(wool, empty, wool, wool, empty, wool));

            if (!crafted.is(NoFall.PLUSH_BOOTS)) {
                throw helper.assertionException(color + " wool in a boot shape crafted " + crafted);
            }

            int dyed = DyedItemColor.getOrDefault(crafted, -1);
            if (dyed != color.getTextureDiffuseColor()) {
                throw helper.assertionException(color + " wool made boots coloured "
                        + Integer.toHexString(dyed));
            }
        }

        helper.succeed();
    }

    @GameTest
    public void bootsTakeDye(GameTestHelper helper) {
        ItemStack dyed = craft(helper, 2, 1,
                List.of(new ItemStack(NoFall.PLUSH_BOOTS), new ItemStack(Items.DYE.pick(DyeColor.LIME))));

        if (!dyed.is(NoFall.PLUSH_BOOTS)) {
            throw helper.assertionException("boots plus a dye crafted " + dyed);
        }

        int color = DyedItemColor.getOrDefault(dyed, -1);
        if (color != DyeColor.LIME.getTextureDiffuseColor()) {
            throw helper.assertionException("lime dye left the boots at " + Integer.toHexString(color));
        }

        helper.succeed();
    }

    /// Runs the grid through the server's own recipe lookup, so a typo in a recipe file shows up
    /// as nothing crafted rather than as a mod that quietly ships an uncraftable item.
    private static ItemStack craft(GameTestHelper helper, int width, int height, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(width, height, grid);
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> helper.assertionException("no recipe matched the grid"));
        return recipe.value().assemble(input);
    }

    /// The debug command has to actually be there, and actually hand over plush boots.
    @GameTest
    public void debugCommandGivesBoots(GameTestHelper helper) {
        var server = helper.getLevel().getServer();

        if (server.getCommands().getDispatcher().getRoot().getChild(NoFall.MOD_ID) == null) {
            throw helper.assertionException("/" + NoFall.MOD_ID + " is not registered");
        }

        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(player), NoFall.MOD_ID + " give");

        ItemStack given = player.getInventory().getItem(0);

        if (given.getItem() != NoFall.PLUSH_BOOTS) {
            throw helper.assertionException("command gave " + given + " instead of plush boots");
        }

        helper.succeed();
    }

}
