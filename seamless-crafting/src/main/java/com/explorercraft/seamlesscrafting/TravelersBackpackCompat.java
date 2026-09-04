package com.explorercraft.seamlesscrafting;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traveler's Backpack storage, reached by reflection. The mod stays optional: nothing here is
 * compiled against it, and every handle is null when it is not installed. Its backpack block
 * entity is not a {@link Container} itself, but the wrapper it holds hands out one.
 */
public final class TravelersBackpackCompat {
	private static final Logger LOGGER = LoggerFactory.getLogger(TravelersBackpackCompat.class);

	@Nullable
	private static Class<?> blockEntityClass;
	@Nullable
	private static MethodHandle blockEntityWrapper;
	@Nullable
	private static MethodHandle playerWrapper;
	@Nullable
	private static MethodHandle isWearingBackpack;
	@Nullable
	private static MethodHandle wrapperStorage;

	static {
		if (FabricLoader.getInstance().isModLoaded("travelersbackpack")) {
			try {
				MethodHandles.Lookup lookup = MethodHandles.publicLookup();
				Class<?> blockEntity = Class.forName("com.tiviacz.travelersbackpack.blockentity.BackpackBlockEntity");
				Class<?> wrapper = Class.forName("com.tiviacz.travelersbackpack.inventory.BackpackWrapper");
				Class<?> attachments = Class.forName("com.tiviacz.travelersbackpack.attachment.AttachmentUtils");

				blockEntityWrapper = lookup.unreflect(blockEntity.getMethod("getWrapper"));
				playerWrapper = lookup.unreflect(attachments.getMethod("getBackpackWrapper", Player.class));
				isWearingBackpack = lookup.unreflect(attachments.getMethod("isWearingBackpack", Player.class));
				wrapperStorage = lookup.unreflect(wrapper.getMethod("getStorage"));
				blockEntityClass = blockEntity;
			} catch (ReflectiveOperationException | RuntimeException e) {
				LOGGER.warn("Traveler's Backpack is installed but its storage could not be reached; skipping backpacks", e);
				blockEntityClass = null;
			}
		}
	}

	private TravelersBackpackCompat() {
	}

	/** The storage of a placed backpack block, or null for any other block entity. */
	@Nullable
	public static Container storageOf(BlockEntity blockEntity) {
		if (blockEntityClass == null || !blockEntityClass.isInstance(blockEntity)) {
			return null;
		}

		try {
			return storageOfWrapper(blockEntityWrapper.invoke(blockEntity));
		} catch (Throwable e) {
			LOGGER.warn("Could not read a Traveler's Backpack block", e);
			return null;
		}
	}

	/** The storage of the backpack the player is wearing, or null if they wear none. */
	@Nullable
	public static Container wornStorage(@Nullable Player player) {
		if (blockEntityClass == null || player == null) {
			return null;
		}

		try {
			if (!(boolean)isWearingBackpack.invoke(player)) {
				return null;
			}
			return storageOfWrapper(playerWrapper.invoke(player));
		} catch (Throwable e) {
			LOGGER.warn("Could not read the worn Traveler's Backpack", e);
			return null;
		}
	}

	@Nullable
	private static Container storageOfWrapper(@Nullable Object wrapper) throws Throwable {
		return wrapper != null && wrapperStorage.invoke(wrapper) instanceof Container container ? container : null;
	}
}
