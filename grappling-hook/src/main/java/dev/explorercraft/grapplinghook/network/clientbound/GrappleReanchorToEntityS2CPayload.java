package dev.explorercraft.grapplinghook.network.clientbound;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.network.S2CPayload;
import dev.explorercraft.grapplinghook.network.codec.Vec3StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Lightweight "the anchor this hook is attached to changed" notification.
 *
 * <p>Used when a block the hook was anchored to becomes part of a contraption.
 * We swap the hook's anchor target onto the contraption entity <em>without</em>
 * tearing down the physics controller. Sending the full
 * {@link GrappleAttachS2CPayload} instead would trigger the client to rebuild
 * its {@link dev.explorercraft.grapplinghook.client.physics.controller.GrapplingHookPhysicsController},
 * which in turn disables the old controller and fires a
 * {@link dev.explorercraft.grapplinghook.network.serverbound.HaltCustomPhysicsC2SPayload}
 * back — killing the very hook we just reanchored.</p>
 */
public record GrappleReanchorToEntityS2CPayload(int hookId, int newEntityId, Vec3 localOffset) implements S2CPayload {

    public static final Identifier IDENTIFIER = GrappleMod.id("grapple_reanchor_entity");
    public static final CustomPacketPayload.Type<GrappleReanchorToEntityS2CPayload> PAYLOAD_TYPE = new Type<>(IDENTIFIER);

    public static final StreamCodec<RegistryFriendlyByteBuf, GrappleReanchorToEntityS2CPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GrappleReanchorToEntityS2CPayload::hookId,
            ByteBufCodecs.INT, GrappleReanchorToEntityS2CPayload::newEntityId,
            Vec3StreamCodec.INSTANCE, GrappleReanchorToEntityS2CPayload::localOffset,
            GrappleReanchorToEntityS2CPayload::new
    );

    @NotNull
    @Override
    public Type<GrappleReanchorToEntityS2CPayload> type() {
        return PAYLOAD_TYPE;
    }
}
