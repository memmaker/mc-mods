package dev.explorercraft.immersiveaircraft.network.s2c;

import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.cobalt.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InventoryUpdateMessage extends Message {
    private final int vehicle;
    private final int index;
    private final ItemStack stack;

    public InventoryUpdateMessage(int id, int index, ItemStack stack) {
        this.vehicle = id;
        this.index = index;
        this.stack = stack;
    }

    // ponytail: ItemStack.save(CompoundTag)/ItemStack.of(CompoundTag) were removed with the old
    // NBT item-save format; ItemStack is now (de)serialized via its own StreamCodec, which needs
    // a RegistryFriendlyByteBuf. Every message in this mod is actually sent over one (see
    // NetworkHandlerImpl), so the cast is safe.
    public InventoryUpdateMessage(FriendlyByteBuf b) {
        vehicle = b.readInt();
        index = b.readInt();
        stack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) b);
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeInt(vehicle);
        b.writeInt(index);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) b, stack);
    }

    @Override
    public void receive(Player e) {
        Main.networkManager.handleInventoryUpdate(this);
    }

    public int getVehicle() {
        return vehicle;
    }

    public int getIndex() {
        return index;
    }

    public ItemStack getStack() {
        return stack;
    }

}
