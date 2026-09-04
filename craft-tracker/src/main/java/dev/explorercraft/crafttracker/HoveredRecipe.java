package dev.explorercraft.crafttracker;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/// What the recipe book is showing under the cursor, updated while the book draws itself.
public final class HoveredRecipe {
    private static Item hovered;

    private HoveredRecipe() {
    }

    public static void set(Item item) {
        hovered = item;
    }

    /// Only the button that claimed the hover clears it, so button order while drawing does not
    /// matter.
    public static void clearIf(Item item) {
        if (hovered == item) hovered = null;
    }

    public static void clear() {
        hovered = null;
    }

    public static @Nullable Item get() {
        return hovered;
    }
}
