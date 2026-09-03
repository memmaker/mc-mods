package dev.explorercraft.immersiveaircraft.entity.inventory;

import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.cobalt.network.NetworkHandler;
import dev.explorercraft.immersiveaircraft.entity.InventoryVehicleEntity;
import dev.explorercraft.immersiveaircraft.network.c2s.RequestInventory;
import dev.explorercraft.immersiveaircraft.network.s2c.InventoryUpdateMessage;
import dev.explorercraft.immersiveaircraft.screen.VehicleScreenHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class SparseSimpleInventory extends SimpleContainer {
    private final NonNullList<ItemStack> tracked;
    private boolean inventoryRequested = false;

    public SparseSimpleInventory(int size) {
        super(size);

        tracked = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    // ponytail: ItemStack.save(CompoundTag)/ItemStack.of(CompoundTag) were removed - ItemStack is
    // now only (de)serialized through its Codec, which needs registry access for its data
    // components. Threaded through from the owning entity (Entity.registryAccess()).
    public ListTag writeNbt(ListTag nbtList, HolderLookup.Provider registries) {
        for (int i = 0; i < this.getContainerSize(); ++i) {
            if (this.getItem(i).isEmpty()) continue;
            CompoundTag nbtCompound = new CompoundTag();
            nbtCompound.putByte("Slot", (byte) i);
            nbtCompound.store("Item", ItemStack.CODEC, registries.createSerializationContext(NbtOps.INSTANCE), this.getItem(i));
            nbtList.add(nbtCompound);
        }
        return nbtList;
    }

    public void readNbt(ListTag nbtList, HolderLookup.Provider registries) {
        this.clearContent();
        for (int i = 0; i < nbtList.size(); ++i) {
            CompoundTag nbtCompound = nbtList.getCompoundOrEmpty(i);
            int slot = nbtCompound.getByteOr("Slot", (byte) 0) & 0xFF;
            ItemStack itemStack = nbtCompound.read("Item", ItemStack.CODEC, registries.createSerializationContext(NbtOps.INSTANCE)).orElse(ItemStack.EMPTY);
            if (itemStack.isEmpty()) continue;
            if (slot > this.getContainerSize()) {
                Main.LOGGER.warn("Inventory slot out of bound, {} has been discarded!", itemStack);
                continue;
            }
            this.setItem(slot, itemStack);
        }
    }

    public void tick(InventoryVehicleEntity entity) {
        if (entity.level().isClientSide()) {
            // Sync initial inventory
            if (!inventoryRequested) {
                NetworkHandler.sendToServer(new RequestInventory(entity.getId()));
                inventoryRequested = true;
            }
        } else {
            // Sync changed slots
            int lastSyncIndex = entity.getInventoryDescription().getLastSyncIndex();
            if (lastSyncIndex == 0) return;
            int index = entity.tickCount % lastSyncIndex;
            ItemStack stack = getItem(index);
            ItemStack trackedStack = tracked.get(index);
            if (!ItemStack.isSameItem(stack, trackedStack)) {
                tracked.set(index, stack.copy());
                entity.level().players().forEach(p -> {
                    if (!(p.containerMenu instanceof VehicleScreenHandler vehicleScreenHandler && vehicleScreenHandler.getVehicle() == entity)) {
                        NetworkHandler.sendToPlayer(new InventoryUpdateMessage(entity.getId(), index, stack), (ServerPlayer) p);
                    }
                });
            }
        }
    }
}
