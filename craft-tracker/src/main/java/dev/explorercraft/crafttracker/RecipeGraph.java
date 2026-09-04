package dev.explorercraft.crafttracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Turns the recipes the client knows about into the plain item -> recipe table CraftPlan walks.
///
/// The client only ever sees recipes the player has unlocked, so a chain stops early at anything
/// still locked. That is the same set the recipe book shows, which is where the key is pressed.
public final class RecipeGraph {
    private RecipeGraph() {
    }

    /// ponytail: rebuilt on every keypress (a few thousand entries, once per human keystroke).
    /// Cache it against ClientRecipeBook changes if that ever shows up in a profile.
    public static Map<Item, List<CraftPlan.Craft>> build(Minecraft minecraft) {
        Map<Item, List<CraftPlan.Craft>> table = new HashMap<>();
        if (minecraft.player == null || minecraft.level == null) return table;

        ContextMap context = SlotDisplayContext.fromLevel(minecraft.level);
        for (RecipeCollection collection : minecraft.player.getRecipeBook().getCollections()) {
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                add(table, entry, context);
            }
        }
        return table;
    }

    private static void add(Map<Item, List<CraftPlan.Craft>> table, RecipeDisplayEntry entry, ContextMap context) {
        List<ItemStack> results = entry.resultItems(context);
        if (results.isEmpty()) return;
        ItemStack result = results.getFirst();
        if (result.isEmpty()) return;

        Map<Item, Integer> inputs = new LinkedHashMap<>();
        for (SlotDisplay slot : ingredientsOf(entry.display())) {
            Item input = firstItem(slot, context);
            if (input != null) inputs.merge(input, 1, Integer::sum);
        }
        if (inputs.isEmpty()) return;

        table.computeIfAbsent(result.getItem(), item -> new ArrayList<>())
                .add(new CraftPlan.Craft(result.getCount(), inputs));
    }

    /// Recipe types not listed here (brewing, transmute, unknown modded ones) leave their result
    /// looking raw, which is the safe way to be wrong: you get told to gather the item itself.
    private static List<SlotDisplay> ingredientsOf(RecipeDisplay display) {
        return switch (display) {
            case ShapedCraftingRecipeDisplay shaped -> shaped.ingredients();
            case ShapelessCraftingRecipeDisplay shapeless -> shapeless.ingredients();
            case FurnaceRecipeDisplay furnace -> List.of(furnace.ingredient());
            case StonecutterRecipeDisplay stonecutter -> List.of(stonecutter.input());
            case SmithingRecipeDisplay smithing -> List.of(smithing.template(), smithing.base(), smithing.addition());
            default -> List.of();
        };
    }

    /// ponytail: a tag ingredient becomes its first item, so "any plank" turns into oak planks.
    /// Weighting the choice by what the player already carries is the obvious upgrade.
    private static Item firstItem(SlotDisplay slot, ContextMap context) {
        for (ItemStack stack : slot.resolveForStacks(context)) {
            if (!stack.isEmpty()) return stack.getItem();
        }
        return null;
    }
}
