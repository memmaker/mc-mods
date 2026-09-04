package dev.explorercraft.grapplinghook;

import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.entity.grapplinghook.GrapplinghookEntity;
import dev.explorercraft.grapplinghook.content.recipe.smithing.HookUpgradeSmithingRecipe;
import dev.explorercraft.grapplinghook.content.registry.CustomizationProperties;
import dev.explorercraft.grapplinghook.content.registry.internal.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

/// The port's smoke test: everything registers, a hook entity spawns, and a smithing upgrade
/// still writes its customization onto the stack.
public class GrappleModGameTest {

    @GameTest
    public void itemsAndEntityAreRegistered(GameTestHelper helper) {
        if (!BuiltInRegistries.ITEM.containsKey(GrappleMod.id("grappling_hook"))) {
            throw helper.assertionException("grappling_hook item is not registered");
        }
        if (!BuiltInRegistries.ITEM.containsKey(GrappleMod.id("long_fall_boots"))) {
            throw helper.assertionException("long_fall_boots item is not registered");
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(GrappleMod.id("grapplehook"))) {
            throw helper.assertionException("grapplehook entity type is not registered");
        }

        GrapplinghookEntity hook = new GrapplinghookEntity(
                dev.explorercraft.grapplinghook.content.registry.internal.ModEntities.GRAPPLE_HOOK.get(),
                helper.getLevel());
        if (!hook.getType().equals(
                dev.explorercraft.grapplinghook.content.registry.internal.ModEntities.GRAPPLE_HOOK.get())) {
            throw helper.assertionException("hook entity did not take its own entity type");
        }

        helper.succeed();
    }

    /// The upgrade recipes are the mod's whole progression, and they went through the biggest
    /// rewrite in the port (SmithingTransformRecipe lost the fixed-stack result).
    @GameTest
    public void smithingUpgradeAppliesCustomization(GameTestHelper helper) {
        HookUpgradeSmithingRecipe found = null;
        for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof HookUpgradeSmithingRecipe upgrade
                    && holder.id().identifier().equals(GrappleMod.id("smithing/hook_motor"))) {
                found = upgrade;
                break;
            }
        }
        if (found == null) throw helper.assertionException("smithing/hook_motor recipe did not load");

        ItemStack base = ModItems.GRAPPLING_HOOK.get().getDefaultInstance();
        ItemStack result = found.assemble(new SmithingRecipeInput(
                ModItems.BASE_UPGRADE.get().getDefaultInstance(),
                base,
                found.additionIngredientRaw().items().iterator().next().value().getDefaultInstance()));

        HookCustomization custom = ModItems.GRAPPLING_HOOK.get().getCustomizationsOrDefault(result);
        if (!Boolean.TRUE.equals(custom.get(CustomizationProperties.MOTOR_ATTACHED.get()))) {
            throw helper.assertionException("motor upgrade did not apply to the hook");
        }

        helper.succeed();
    }
}
