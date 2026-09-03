package dev.explorercraft.photosafari;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/// Entities the client believes ended up in frame while the camera was in loot mode. The
/// server re-checks every one of them, same as {@link PhotographPayload}.
public record LootPayload(List<Integer> entityIds) implements CustomPacketPayload {
    public static final int MAX_ENTITIES = 256;

    public static final Type<LootPayload> TYPE = new Type<>(PhotoSafari.id("loot"));

    public static final StreamCodec<ByteBuf, LootPayload> CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list(MAX_ENTITIES))
            .map(LootPayload::new, LootPayload::entityIds);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
