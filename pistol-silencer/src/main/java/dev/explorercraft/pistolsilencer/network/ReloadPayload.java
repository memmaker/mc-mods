package dev.explorercraft.pistolsilencer.network;

import dev.explorercraft.pistolsilencer.PistolSilencer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReloadPayload() implements CustomPacketPayload {
    public static final Type<ReloadPayload> TYPE = new Type<>(PistolSilencer.id("reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadPayload> CODEC = StreamCodec.unit(new ReloadPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
