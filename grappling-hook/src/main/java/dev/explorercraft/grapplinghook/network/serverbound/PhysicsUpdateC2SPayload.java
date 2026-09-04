package dev.explorercraft.grapplinghook.network.serverbound;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.network.C2SPayload;
import dev.explorercraft.grapplinghook.physics.PlayerPhysicsFrame;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record PhysicsUpdateC2SPayload(PlayerPhysicsFrame frame) implements C2SPayload {

    public static final Identifier IDENTIFIER = GrappleMod.id("physics_update");
    public static final CustomPacketPayload.Type<PhysicsUpdateC2SPayload> PAYLOAD_TYPE = new Type<>(IDENTIFIER);

    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsUpdateC2SPayload> STREAM_CODEC = StreamCodec.composite(
            PlayerPhysicsFrame.STREAM_CODEC,
            PhysicsUpdateC2SPayload::frame,
            PhysicsUpdateC2SPayload::new
    );

    public PhysicsUpdateC2SPayload() {
        this(new PlayerPhysicsFrame());
    }

    @NotNull
    @Override
    public Type<PhysicsUpdateC2SPayload> type() {
        return PAYLOAD_TYPE;
    }

    @Override
    public void process(ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> GrappleMod
                .get()
                .getServerPhysicsObserver()
                .receiveNewFrame(ctx.player(), this.frame)
        );
    }

}
