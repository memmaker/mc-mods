package dev.explorercraft.crafttracker;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/// The worn Traveler's Backpack, reached by reflection so the mod stays optional: nothing here is
/// compiled against it and every handle is null when it is not installed.
///
/// Same handles as the copy in seamless-crafting; the two mods build separately and share no code.
public final class TravelersBackpackCompat {
    private static @Nullable MethodHandle playerWrapper;
    private static @Nullable MethodHandle isWearingBackpack;
    private static @Nullable MethodHandle wrapperStorage;

    static {
        if (FabricLoader.getInstance().isModLoaded("travelersbackpack")) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                Class<?> wrapper = Class.forName("com.tiviacz.travelersbackpack.inventory.BackpackWrapper");
                Class<?> attachments = Class.forName("com.tiviacz.travelersbackpack.attachment.AttachmentUtils");

                playerWrapper = lookup.unreflect(attachments.getMethod("getBackpackWrapper", Player.class));
                isWearingBackpack = lookup.unreflect(attachments.getMethod("isWearingBackpack", Player.class));
                wrapperStorage = lookup.unreflect(wrapper.getMethod("getStorage"));
            } catch (ReflectiveOperationException | RuntimeException e) {
                CraftTracker.LOGGER.warn("Traveler's Backpack is installed but its storage could not be reached; skipping backpacks", e);
                playerWrapper = null;
            }
        }
    }

    private TravelersBackpackCompat() {
    }

    /// The storage of the backpack the player is wearing, or null if they wear none.
    public static @Nullable Container wornStorage(@Nullable Player player) {
        if (playerWrapper == null || player == null) return null;

        try {
            if (!(boolean) isWearingBackpack.invoke(player)) return null;
            Object wrapper = playerWrapper.invoke(player);
            return wrapper != null && wrapperStorage.invoke(wrapper) instanceof Container container ? container : null;
        } catch (Throwable e) {
            CraftTracker.LOGGER.warn("Could not read the worn Traveler's Backpack", e);
            return null;
        }
    }
}
