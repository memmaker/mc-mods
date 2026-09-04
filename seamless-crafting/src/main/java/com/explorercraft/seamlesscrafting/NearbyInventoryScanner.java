package com.explorercraft.seamlesscrafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * Finds the containers around a crafting table. Walks the loaded chunks' block entity maps
 * rather than every block position in the radius — a radius of 16 is 36k positions but only
 * nine chunks.
 */
public final class NearbyInventoryScanner {
	private static final int MAX_ENTRIES = 512;

	private NearbyInventoryScanner() {
	}

	public static int getConfiguredRadius() {
		return SeamlessCraftingConfig.getNearbyRadius();
	}

	public static List<Container> findNearbyContainers(Level level, BlockPos center, int radius, @Nullable Player player) {
		List<Container> containers = new ArrayList<>();
		forEachContainer(level, center, radius, player, (pos, container) -> containers.add(container));
		return containers;
	}

	public static List<NearbyItemEntry> collectItemCounts(Level level, BlockPos center, int radius, @Nullable Player player) {
		Map<Item, Integer> totals = new HashMap<>();
		for (Container container : findNearbyContainers(level, center, radius, player)) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (!stack.isEmpty() && Inventory.isUsableForCrafting(stack)) {
					totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
				}
			}
		}

		List<NearbyItemEntry> entries = new ArrayList<>();
		for (Map.Entry<Item, Integer> total : totals.entrySet()) {
			entries.add(new NearbyItemEntry(new ItemStack(total.getKey()), total.getValue()));
			if (entries.size() >= MAX_ENTRIES) {
				break;
			}
		}
		return entries;
	}

	public static List<ItemStack> collectCraftableStacks(Level level, BlockPos center, int radius, @Nullable Player player) {
		List<ItemStack> stacks = new ArrayList<>();
		for (Container container : findNearbyContainers(level, center, radius, player)) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.isEmpty() || !Inventory.isUsableForCrafting(stack)) {
					continue;
				}

				stacks.add(stack.copy());
				if (stacks.size() >= MAX_ENTRIES) {
					return stacks;
				}
			}
		}
		return stacks;
	}

	public static List<BlockPos> findContainerPositionsWithItem(Level level, BlockPos center, int radius, @Nullable Player player, Item item) {
		Set<BlockPos> positions = new LinkedHashSet<>();
		forEachContainer(level, center, radius, player, (pos, container) -> {
			if (containerHasItem(container, item)) {
				positions.add(pos);
			}
		});
		return new ArrayList<>(positions);
	}

	@Nullable
	public static LevelPos getLevelPos(ContainerLevelAccess access) {
		return access.evaluate(LevelPos::new).orElse(null);
	}

	public record NearbyItemEntry(ItemStack stack, int count) {
	}

	public record LevelPos(Level level, BlockPos pos) {
	}

	private interface ContainerVisitor {
		void accept(BlockPos pos, Container container);
	}

	private static void forEachContainer(Level level, BlockPos center, int radius, @Nullable Player player, ContainerVisitor visitor) {
		Container worn = TravelersBackpackCompat.wornStorage(player);
		if (worn != null) {
			visitor.accept(player.blockPosition(), worn);
		}

		int minChunkX = (center.getX() - radius) >> 4;
		int maxChunkX = (center.getX() + radius) >> 4;
		int minChunkZ = (center.getZ() - radius) >> 4;
		int maxChunkZ = (center.getZ() + radius) >> 4;

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}

				for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
					BlockPos pos = entry.getKey();
					if (!withinRadius(center, pos, radius)) {
						continue;
					}
					BlockEntity blockEntity = entry.getValue();
					Container container = blockEntity instanceof Container found && !(found instanceof Inventory)
							? found
							: TravelersBackpackCompat.storageOf(blockEntity);
					if (container != null) {
						visitor.accept(pos.immutable(), container);
					}
				}
			}
		}
	}

	private static boolean withinRadius(BlockPos center, BlockPos pos, int radius) {
		return Math.abs(pos.getX() - center.getX()) <= radius
				&& Math.abs(pos.getY() - center.getY()) <= radius
				&& Math.abs(pos.getZ() - center.getZ()) <= radius;
	}

	private static boolean containerHasItem(Container container, Item item) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && stack.getItem() == item && Inventory.isUsableForCrafting(stack)) {
				return true;
			}
		}
		return false;
	}
}
