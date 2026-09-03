package dev.explorercraft.pistolsilencer.network;

import dev.explorercraft.pistolsilencer.PistolSilencer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells the server whether the client is aiming down sights, purely so recoil can be reduced while aiming. */
public record AimPayload(boolean aiming) implements CustomPacketPayload {
    public static final Type<AimPayload> TYPE = new Type<>(PistolSilencer.id("aim"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AimPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, AimPayload::aiming, AimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
