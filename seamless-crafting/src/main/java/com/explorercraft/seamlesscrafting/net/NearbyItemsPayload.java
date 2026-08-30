package com.explorercraft.seamlesscrafting.net;

import com.explorercraft.seamlesscrafting.NearbyInventoryScanner.NearbyItemEntry;
import com.explorercraft.seamlesscrafting.SeamlessCrafting;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record NearbyItemsPayload(List<NearbyItemEntry> entries, List<ItemStack> craftableStacks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<NearbyItemsPayload> ID = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(SeamlessCrafting.MOD_ID, "nearby_items")
	);
	private static final StreamCodec<RegistryFriendlyByteBuf, NearbyItemEntry> ENTRY_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, NearbyItemEntry::stack,
			ByteBufCodecs.VAR_INT, NearbyItemEntry::count,
			NearbyItemEntry::new
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, NearbyItemsPayload> CODEC = StreamCodec.composite(
			ENTRY_CODEC.apply(ByteBufCodecs.list()), NearbyItemsPayload::entries,
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), NearbyItemsPayload::craftableStacks,
			NearbyItemsPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
