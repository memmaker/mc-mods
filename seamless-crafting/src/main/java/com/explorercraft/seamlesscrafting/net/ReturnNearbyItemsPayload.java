package com.explorercraft.seamlesscrafting.net;

import com.explorercraft.seamlesscrafting.SeamlessCrafting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReturnNearbyItemsPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ReturnNearbyItemsPayload> ID = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(SeamlessCrafting.MOD_ID, "return_nearby_items")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, ReturnNearbyItemsPayload> CODEC =
			StreamCodec.unit(new ReturnNearbyItemsPayload());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
