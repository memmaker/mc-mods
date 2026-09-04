package dev.explorercraft.crafttracker;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/// Walks a recipe down to the things you cannot craft: ores, logs, mob drops, anything no known
/// recipe produces. Pure map arithmetic, so it can be tested without a world.
public final class CraftPlan {
    private CraftPlan() {
    }

    /// One way to make `yield` of an item out of `inputs`.
    public record Craft(int yield, Map<Item, Integer> inputs) {
    }

    /// Raw materials needed for `amount` of `target`, keyed by item.
    ///
    /// Overshoot is kept: 5 sticks means 2 crafts of 4, so 2 planks, not 1.25.
    public static Map<Item, Integer> rawMaterials(Item target, int amount, Function<Item, List<Craft>> recipes) {
        Map<Item, Integer> raw = new LinkedHashMap<>();
        expand(target, amount, recipes, new HashSet<>(), raw);
        return raw;
    }

    private static void expand(Item item, int amount, Function<Item, List<Craft>> recipes,
                               Set<Item> path, Map<Item, Integer> raw) {
        Craft craft = amount <= 0 ? null : choose(item, recipes, path);
        if (craft == null) {
            if (amount > 0) raw.merge(item, amount, Integer::sum);
            return;
        }

        int crafts = Math.ceilDiv(amount, craft.yield());
        path.add(item);
        craft.inputs().forEach((input, count) -> expand(input, crafts * count, recipes, path, raw));
        path.remove(item);
    }

    /// Picks the recipe to follow, or null when the item counts as raw.
    private static Craft choose(Item item, Function<Item, List<Craft>> recipes, Set<Item> path) {
        List<Craft> candidates = recipes.apply(item);
        if (candidates == null || candidates.isEmpty()) return null;

        for (Craft craft : candidates) {
            // Following a recipe that needs something already being made would loop forever.
            if (craft.inputs().keySet().stream().anyMatch(path::contains)) continue;
            if (unpacks(item, craft, recipes)) continue;
            return craft;
        }
        // Only loops and unpacking left: gather the item itself.
        return null;
    }

    /// True for the "take a block apart again" half of a pack/unpack pair: nine ingots out of one
    /// iron block is a recipe for iron ingots, but following it would ask for iron blocks.
    private static boolean unpacks(Item item, Craft craft, Function<Item, List<Craft>> recipes) {
        if (craft.yield() <= 1 || craft.inputs().size() != 1) return false;
        Item source = craft.inputs().keySet().iterator().next();
        List<Craft> reverse = recipes.apply(source);
        return reverse != null && reverse.stream().anyMatch(other -> other.inputs().containsKey(item));
    }

    /// Convenience for a fixed recipe table.
    public static Function<Item, List<Craft>> from(Map<Item, List<Craft>> table) {
        Map<Item, List<Craft>> copy = new HashMap<>(table);
        return copy::get;
    }
}
