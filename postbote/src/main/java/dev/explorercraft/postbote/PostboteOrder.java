package dev.explorercraft.postbote;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * Where a delivery compass points and what it pays out when it gets there. {@code villager} is
 * the specific entity currently being tracked, kept live by {@link Postbote}'s tick job; empty
 * until one is found near {@code destination}, or again after the tracked one dies.
 */
public record PostboteOrder(GlobalPos destination, int reward, Optional<UUID> villager) {
    public static final Codec<PostboteOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("destination").forGetter(PostboteOrder::destination),
            Codec.INT.fieldOf("reward").forGetter(PostboteOrder::reward),
            UUIDUtil.CODEC.optionalFieldOf("villager").forGetter(PostboteOrder::villager)
    ).apply(instance, PostboteOrder::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PostboteOrder> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, PostboteOrder::destination,
            ByteBufCodecs.VAR_INT, PostboteOrder::reward,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), PostboteOrder::villager,
            PostboteOrder::new);
}
