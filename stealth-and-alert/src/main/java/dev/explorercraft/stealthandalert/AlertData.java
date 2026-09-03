package dev.explorercraft.stealthandalert;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/// Everything one mob knows about the players hunting it, or hiding from it.
/// Synced to every client tracking the mob so the HUD can draw it.
public record AlertData(
        int state,
        Map<UUID, Float> targetAwareness,
        Map<UUID, Integer> targetStates,
        Map<UUID, Integer> targetReactionTicks,
        Map<UUID, Integer> targetMemoryTicks,
        Optional<Vec3> lastKnownPos,
        Optional<UUID> primaryTarget,
        int stateChangeTicks,
        int patienceTicks,
        boolean canSeeAnyone,
        boolean willFighting) {

    /// Mob-wide alert state.
    public static final int IDLE = 0;
    public static final int SUSPICIOUS = 1;
    public static final int SEARCHING = 2;
    public static final int FIGHTING = 3;

    /// How far the mob has got with one particular player.
    public static final int UNTRACKED = 0;
    public static final int AWARE = 1;
    public static final int TRACKING = 2;

    public static final Codec<AlertData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("state").forGetter(AlertData::state),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.FLOAT).optionalFieldOf("target_awareness", Map.of()).forGetter(AlertData::targetAwareness),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("target_states", Map.of()).forGetter(AlertData::targetStates),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("target_reaction_ticks", Map.of()).forGetter(AlertData::targetReactionTicks),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("target_memory_ticks", Map.of()).forGetter(AlertData::targetMemoryTicks),
                    Vec3.CODEC.optionalFieldOf("last_known_pos").forGetter(AlertData::lastKnownPos),
                    UUIDUtil.STRING_CODEC.optionalFieldOf("primary_target").forGetter(AlertData::primaryTarget),
                    Codec.INT.optionalFieldOf("state_change_ticks", 0).forGetter(AlertData::stateChangeTicks),
                    Codec.INT.optionalFieldOf("patience_ticks", StealthConfig.PATIENCE_TICKS).forGetter(AlertData::patienceTicks),
                    Codec.BOOL.optionalFieldOf("can_see_anyone", false).forGetter(AlertData::canSeeAnyone),
                    Codec.BOOL.optionalFieldOf("will_fighting", false).forGetter(AlertData::willFighting)
            ).apply(instance, AlertData::new)
    );

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new
    );

    private static final StreamCodec<ByteBuf, Map<UUID, Float>> FLOAT_MAP =
            ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.FLOAT);

    private static final StreamCodec<ByteBuf, Map<UUID, Integer>> INT_MAP =
            ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.VAR_INT);

    public static final StreamCodec<ByteBuf, AlertData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.state());
                FLOAT_MAP.encode(buf, data.targetAwareness());
                INT_MAP.encode(buf, data.targetStates());
                INT_MAP.encode(buf, data.targetReactionTicks());
                INT_MAP.encode(buf, data.targetMemoryTicks());
                ByteBufCodecs.optional(VEC3).encode(buf, data.lastKnownPos());
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).encode(buf, data.primaryTarget());
                ByteBufCodecs.VAR_INT.encode(buf, data.stateChangeTicks());
                ByteBufCodecs.VAR_INT.encode(buf, data.patienceTicks());
                ByteBufCodecs.BOOL.encode(buf, data.canSeeAnyone());
                ByteBufCodecs.BOOL.encode(buf, data.willFighting());
            },
            buf -> new AlertData(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    FLOAT_MAP.decode(buf),
                    INT_MAP.decode(buf),
                    INT_MAP.decode(buf),
                    INT_MAP.decode(buf),
                    ByteBufCodecs.optional(VEC3).decode(buf),
                    ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );

    public static AlertData createDefault() {
        return new AlertData(IDLE, Map.of(), Map.of(), Map.of(), Map.of(), Optional.empty(), Optional.empty(),
                0, StealthConfig.PATIENCE_TICKS, false, false);
    }

    public int stateOf(UUID player) {
        return targetStates.getOrDefault(player, UNTRACKED);
    }
}
