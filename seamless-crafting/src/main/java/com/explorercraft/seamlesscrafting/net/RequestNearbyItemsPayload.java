package com.explorercraft.seamlesscrafting.net;

import com.explorercraft.seamlesscrafting.SeamlessCrafting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestNearbyItemsPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RequestNearbyItemsPayload> ID = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(SeamlessCrafting.MOD_ID, "request_nearby_items")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestNearbyItemsPayload> CODEC =
			StreamCodec.unit(new RequestNearbyItemsPayload());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
