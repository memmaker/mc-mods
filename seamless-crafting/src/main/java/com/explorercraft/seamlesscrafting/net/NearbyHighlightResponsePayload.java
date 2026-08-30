package com.explorercraft.seamlesscrafting.net;

import com.explorercraft.seamlesscrafting.SeamlessCrafting;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NearbyHighlightResponsePayload(List<BlockPos> positions) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<NearbyHighlightResponsePayload> ID = new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(SeamlessCrafting.MOD_ID, "highlight_response")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, NearbyHighlightResponsePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), NearbyHighlightResponsePayload::positions,
			NearbyHighlightResponsePayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
