package dev.explorercraft.grapplinghook.content.registry.internal;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.content.recipe.smithing.HookUpgradeSmithingRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipeSerializers {

    public static final RecipeSerializer<HookUpgradeSmithingRecipe> HOOK_UPGRADE = new RecipeSerializer<>(HookUpgradeSmithingRecipe.CODEC, HookUpgradeSmithingRecipe.STREAM_CODEC);

    public static void registerAll() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, GrappleMod.id("hook_upgrade"), HOOK_UPGRADE);
    }

    private ModRecipeSerializers() {}
}
