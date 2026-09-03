package dev.explorercraft.immersiveaircraft.entity.inventory.slots;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.explorercraft.immersiveaircraft.entity.InventoryVehicleEntity;
import dev.explorercraft.immersiveaircraft.screen.slot.IngredientSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Ingredient;

public class IngredientSlotDescription extends TooltippedSlotDescription {
    final Ingredient ingredient;
    final int maxStackSize;

    public IngredientSlotDescription(String type, int index, int x, int y, JsonObject json) {
        this(type, index, x, y, json,
                Ingredient.CODEC.parse(JsonOps.INSTANCE, json.get("ingredient")).getOrThrow(),
                GsonHelper.getAsInt(json, "maxStackSize", 64)
        );
    }

    public IngredientSlotDescription(String type, int index, int x, int y, JsonObject json, Ingredient ingredient, int maxStackSize) {
        super(type, index, x, y, json);
        this.ingredient = ingredient;
        this.maxStackSize = maxStackSize;
    }

    public IngredientSlotDescription(String type, FriendlyByteBuf buffer) {
        super(type, buffer);

        this.ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
        this.maxStackSize = buffer.readInt();
    }

    public Slot getSlot(InventoryVehicleEntity vehicle, Container inventory) {
        return new IngredientSlot(ingredient, maxStackSize, inventory, index, x, y);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        super.encode(buffer);

        Ingredient.CONTENTS_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, ingredient);
        buffer.writeInt(maxStackSize);
    }
}