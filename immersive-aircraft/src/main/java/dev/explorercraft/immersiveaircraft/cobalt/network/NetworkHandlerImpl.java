package dev.explorercraft.immersiveaircraft.cobalt.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Fabric implementation of the platform-agnostic {@link NetworkHandler}, backed by
 * Minecraft's modern {@link CustomPacketPayload} networking API (raw byte-buf channels
 * were removed after 1.20.4).
 */
public class NetworkHandlerImpl extends NetworkHandler.Impl {
    private final Map<Class<?>, CustomPacketPayload.Type<MessagePayload>> types = new HashMap<>();
    private final Map<Class<?>, Function<FriendlyByteBuf, ? extends Message>> constructors = new HashMap<>();

    private record MessagePayload(Message message,
                                   CustomPacketPayload.Type<MessagePayload> payloadType) implements CustomPacketPayload {
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return payloadType;
        }
    }

    private Identifier createMessageIdentifier(String namespace, Class<?> msg) {
        return Identifier.fromNamespaceAndPath(namespace, msg.getSimpleName().toLowerCase(Locale.ROOT));
    }

    @Override
    public <T extends Message> void registerMessage(String namespace, Class<T> msg, Function<FriendlyByteBuf, T> constructor) {
        Identifier id = createMessageIdentifier(namespace, msg);
        CustomPacketPayload.Type<MessagePayload> type = new CustomPacketPayload.Type<>(id);
        types.put(msg, type);
        constructors.put(msg, constructor);

        StreamCodec<RegistryFriendlyByteBuf, MessagePayload> codec = StreamCodec.ofMember(
                (payload, buf) -> payload.message().encode(buf),
                buf -> new MessagePayload(constructor.apply(buf), type)
        );

        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        PayloadTypeRegistry.clientboundPlay().register(type, codec);

        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                context.server().execute(() -> payload.message().receive(context.player())));

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientProxy.register(type);
        }
    }

    @SuppressWarnings("unchecked")
    private MessagePayload wrap(Message msg) {
        CustomPacketPayload.Type<MessagePayload> type = (CustomPacketPayload.Type<MessagePayload>) types.get(msg.getClass());
        Objects.requireNonNull(type, "Used unregistered message: " + msg.getClass());
        return new MessagePayload(msg, type);
    }

    @Override
    public void sendToServer(Message msg) {
        ClientPlayNetworking.send(wrap(msg));
    }

    @Override
    public void sendToPlayer(Message msg, ServerPlayer e) {
        ServerPlayNetworking.send(e, wrap(msg));
    }

    @Override
    public void sendToTrackingPlayers(Message msg, Entity origin) {
        MessagePayload payload = wrap(msg);
        for (ServerPlayer player : PlayerLookup.tracking(origin)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    // Fabric's client APIs are not side-agnostic; keep them out of eager-loaded server classes.
    private static final class ClientProxy {
        private ClientProxy() {
        }

        static void register(CustomPacketPayload.Type<MessagePayload> type) {
            ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                    context.client().execute(() -> payload.message().receive(context.player())));
        }
    }
}
