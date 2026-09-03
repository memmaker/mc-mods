package dev.explorercraft.immersiveaircraft.util;

import dev.explorercraft.immersiveaircraft.Main;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * ponytail: ItemStack.getOrCreateTag()/hasTag()/getTag() were removed - raw NBT-on-itemstack was
 * replaced by the DataComponent system. Rather than redesign the mod's item<->entity NBT transfer
 * (vehicle name/inventory/upgrades round-tripped through a CompoundTag) around typed components,
 * this stores that same CompoundTag verbatim in one custom component and keeps the old call
 * pattern everywhere else. Revisit with a proper typed component if this needs cross-mod NBT compat.
 */
public final class ItemTagCompat {
    public static final DataComponentType<CompoundTag> VEHICLE_DATA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Main.locate("vehicle_data"),
            DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.TRUSTED_COMPOUND_TAG)
                    .build()
    );

    private ItemTagCompat() {
    }

    /** Forces this class's static registration to run at mod init, before the registry freezes. */
    public static void bootstrap() {
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.has(VEHICLE_DATA);
    }

    public static CompoundTag getTag(ItemStack stack) {
        return stack.get(VEHICLE_DATA);
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag tag = stack.get(VEHICLE_DATA);
        if (tag == null) {
            tag = new CompoundTag();
            stack.set(VEHICLE_DATA, tag);
        }
        return tag;
    }
}
