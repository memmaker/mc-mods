package dev.explorercraft.grapplinghook.network.clientbound;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.network.S2CPayload;
import dev.explorercraft.grapplinghook.network.codec.Vec3StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Mirror of {@link GrappleReanchorToEntityS2CPayload} for the entity → static-block direction.
 * Sent when a contraption disassembles and one of its blocks lands at a position close enough
 * to the hook's current anchor to warrant a silent re-anchor rather than detach.
 *
 * <p>Like its sibling, this intentionally avoids rebuilding the physics controller so the
 * existing hook state is preserved end-to-end.</p>
 */
public record GrappleReanchorToBlockS2CPayload(int hookId, BlockPos blockPos, Vec3 hookWorldPos) implements S2CPayload {

    public static final Identifier IDENTIFIER = GrappleMod.id("grapple_reanchor_block");
    public static final CustomPacketPayload.Type<GrappleReanchorToBlockS2CPayload> PAYLOAD_TYPE = new Type<>(IDENTIFIER);

    public static final StreamCodec<RegistryFriendlyByteBuf, GrappleReanchorToBlockS2CPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,        GrappleReanchorToBlockS2CPayload::hookId,
            BlockPos.STREAM_CODEC,    GrappleReanchorToBlockS2CPayload::blockPos,
            Vec3StreamCodec.INSTANCE, GrappleReanchorToBlockS2CPayload::hookWorldPos,
            GrappleReanchorToBlockS2CPayload::new
    );

    @NotNull
    @Override
    public Type<GrappleReanchorToBlockS2CPayload> type() {
        return PAYLOAD_TYPE;
    }
}
