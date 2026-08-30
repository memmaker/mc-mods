package com.explorercraft.seamlesscrafting.net;

import com.explorercraft.seamlesscrafting.SeamlessCrafting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record NearbyHighlightRequestPayload(ItemStack stack) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<NearbyHighlightRequestPayload> ID = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(SeamlessCrafting.MOD_ID, "highlight_request")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, NearbyHighlightRequestPayload> CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, NearbyHighlightRequestPayload::stack,
			NearbyHighlightRequestPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
