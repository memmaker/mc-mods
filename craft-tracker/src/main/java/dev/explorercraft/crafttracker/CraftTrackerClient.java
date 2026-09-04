package dev.explorercraft.crafttracker;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.explorercraft.crafttracker.mixin.ContainerScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

public final class CraftTrackerClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(CraftTracker.id("controls"));

    /// One key for everything. Over a slot or a recipe it tracks; out in the world it toggles the
    /// overlay, and with shift or control it clears the list.
    private static final KeyMapping TRACK = new KeyMapping("key.crafttracker.track",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);

    /// How many are added per press. Shift-clicking a stack of 64 into the list is rarely what
    /// you want, so it stays a small fixed step and repeats are cheap.
    private static final int STEP = 1;

    private static final int LINE_HEIGHT = 18;
    private static final int MARGIN = 4;

    private static boolean visible = true;

    @Override
    public void onInitializeClient() {
        CraftTracker.QUEUE.load();

        KeyMappingHelper.registerKeyMapping(TRACK);

        // Only fires with no screen open; the screen handler below covers the other case.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TRACK.consumeClick()) {
                if (clearModifierDown(client)) {
                    CraftTracker.QUEUE.entries().clear();
                    CraftTracker.QUEUE.save();
                    visible = true;
                    beep(client, CLEARED);
                } else {
                    visible = !visible;
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
            ScreenEvents.remove(screen).register(ignored -> HoveredRecipe.clear());
            ScreenKeyboardEvents.afterKeyPress(screen).register((ignored, keyEvent) -> {
                if (!TRACK.matches(keyEvent)) return;
                boolean shift = (keyEvent.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

                // The recipe book sits on top of the slots, so it gets asked first.
                Item recipe = HoveredRecipe.get();
                if (recipe != null) {
                    if (shift) {
                        beep(client, track(client, recipe, STEP) ? TRACKED : REFUSED);
                    } else {
                        trackRawMaterials(client, recipe);
                    }
                    return;
                }

                Slot slot = ((ContainerScreenAccessor) containerScreen).getHoveredSlot();
                if (slot == null || !slot.hasItem()) return;
                Item item = slot.getItem().getItem();
                if (shift) {
                    CraftTracker.QUEUE.remove(item);
                    CraftTracker.QUEUE.save();
                    beep(client, CLEARED);
                } else {
                    beep(client, track(client, item, STEP) ? TRACKED : REFUSED);
                }
            });
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                CraftTracker.id("queue"), CraftTrackerClient::renderHud);
    }

    private static boolean clearModifierDown(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    /// How many entries fit down the left edge of the screen. Anything past that would be drawn
    /// off the bottom, so it is refused at the point it would be added.
    private static int capacity(Minecraft minecraft) {
        return Math.max(1, (minecraft.getWindow().getGuiScaledHeight() - 2 * MARGIN) / LINE_HEIGHT);
    }

    /// The overlay is hidden behind an open screen, so every press answers with a click: high
    /// for tracked, low for a list that is already as long as the screen allows.
    private static final float TRACKED = 1.6f;
    private static final float REFUSED = 0.6f;
    private static final float CLEARED = 1.0f;

    private static void beep(Minecraft minecraft, float pitch) {
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, pitch);
        }
    }

    /// Tracks the item, unless the list is full and this would be a new line.
    private static boolean track(Minecraft minecraft, Item item, int amount) {
        Map<Item, Integer> entries = CraftTracker.QUEUE.entries();
        if (!entries.containsKey(item) && entries.size() >= capacity(minecraft)) return false;

        CraftTracker.QUEUE.add(item, amount);
        CraftTracker.QUEUE.save();
        return true;
    }

    /// Follows the hovered recipe down to things that have to be mined, grown or killed for, and
    /// tracks those instead of the recipe's own ingredients.
    private static void trackRawMaterials(Minecraft minecraft, Item item) {
        Map<Item, List<CraftPlan.Craft>> table = RecipeGraph.build(minecraft);
        Map<Item, Integer> raw = CraftPlan.rawMaterials(item, STEP, CraftPlan.from(table));
        if (raw.isEmpty()) return;

        // A chain can be longer than the screen; what does not fit is dropped rather than drawn
        // where nobody can see it, and the low click says so.
        boolean complete = true;
        for (Map.Entry<Item, Integer> material : raw.entrySet()) {
            complete &= track(minecraft, material.getKey(), material.getValue());
        }
        beep(minecraft, complete ? TRACKED : REFUSED);
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible || minecraft.player == null) return;

        Map<Item, Integer> entries = CraftTracker.QUEUE.entries();
        if (entries.isEmpty()) return;

        int y = MARGIN;
        for (Map.Entry<Item, Integer> entry : entries.entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey());
            int have = count(minecraft, entry.getKey());
            boolean done = have >= entry.getValue();

            graphics.item(stack, MARGIN, y);
            graphics.text(minecraft.font, Component.translatable("hud.crafttracker.entry",
                                    have, entry.getValue(), stack.getHoverName())
                            .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                    MARGIN + 20, y + 5, 0xFFFFFFFF);
            y += LINE_HEIGHT;
        }
    }

    /// What the player is carrying: the inventory, plus a worn Traveler's Backpack when that mod
    /// is installed and its contents have reached the client.
    private static int count(Minecraft minecraft, Item item) {
        int total = 0;
        for (ItemStack stack : minecraft.player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) total += stack.getCount();
        }

        Container backpack = TravelersBackpackCompat.wornStorage(minecraft.player);
        if (backpack != null) {
            for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
                ItemStack stack = backpack.getItem(slot);
                if (stack.is(item)) total += stack.getCount();
            }
        }
        return total;
    }
}
