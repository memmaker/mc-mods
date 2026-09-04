package dev.explorercraft.grapplinghook.network.codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Shared stream codec for {@link Vec3} values. Replaces three identical xyz-decomposition
 * blocks that previously lived inline in {@code GrappleAttachS2CPayload.GrappleAttachTarget.EntityOffset},
 * {@code GrappleReanchorToEntityS2CPayload}, and {@code GrappleReanchorToBlockS2CPayload}.
 */
public final class Vec3StreamCodec {

    public static final StreamCodec<RegistryFriendlyByteBuf, Vec3> INSTANCE = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, v -> v.x,
            ByteBufCodecs.DOUBLE, v -> v.y,
            ByteBufCodecs.DOUBLE, v -> v.z,
            Vec3::new
    );

    /** 26.2's ByteBufCodecs.VECTOR3F reads the immutable Vector3fc view; payloads here carry Vector3f. */
    public static final StreamCodec<io.netty.buffer.ByteBuf, Vector3f> VECTOR3F =
            ByteBufCodecs.VECTOR3F.map(Vector3f::new, v -> v);

    private Vec3StreamCodec() {}
}
