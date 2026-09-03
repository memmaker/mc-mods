package dev.explorercraft.pistolsilencer.network;

import dev.explorercraft.pistolsilencer.PistolSilencer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * The client's best measurement of a mob's real head box — read straight off its render model by
 * fx-globals, if that mod is present — so the server's hitscan headshot check does not have to
 * guess one from the hitbox. Sent every tick the player is aiming a pistol at a living target.
 */
public record HeadBoxHintPayload(int targetEntityId, Vec3 min, Vec3 max) implements CustomPacketPayload {
    public static final Type<HeadBoxHintPayload> TYPE = new Type<>(PistolSilencer.id("head_box_hint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeadBoxHintPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HeadBoxHintPayload::targetEntityId,
            Vec3.STREAM_CODEC, HeadBoxHintPayload::min,
            Vec3.STREAM_CODEC, HeadBoxHintPayload::max,
            HeadBoxHintPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
