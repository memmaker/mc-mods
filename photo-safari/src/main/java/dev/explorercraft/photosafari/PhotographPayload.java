package dev.explorercraft.photosafari;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/// Entities the client believes ended up on the photo. The server re-checks every one of them.
public record PhotographPayload(List<Integer> entityIds) implements CustomPacketPayload {
    public static final int MAX_ENTITIES = 256;

    public static final Type<PhotographPayload> TYPE = new Type<>(PhotoSafari.id("photograph"));

    public static final StreamCodec<ByteBuf, PhotographPayload> CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list(MAX_ENTITIES))
            .map(PhotographPayload::new, PhotographPayload::entityIds);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
