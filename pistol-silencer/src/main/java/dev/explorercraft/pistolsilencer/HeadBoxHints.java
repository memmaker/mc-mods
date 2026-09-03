package dev.explorercraft.pistolsilencer;

import dev.explorercraft.pistolsilencer.network.HeadBoxHintPayload;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The most recent {@link HeadBoxHintPayload} each player's client has sent, for {@code
 * PistolItem}'s hitscan headshot check. A hint is only trusted for a moment: the client sends one
 * every tick it is aiming a pistol at a living target, so a stale one just means the player let
 * go of the trigger or looked away.
 */
public final class HeadBoxHints {
    private record Hint(int entityId, AABB box, long recordedAtMillis) {
    }

    private static final Map<UUID, Hint> HINTS = new ConcurrentHashMap<>();
    private static final long MAX_AGE_MILLIS = 500;

    private HeadBoxHints() {
    }

    public static void record(UUID player, HeadBoxHintPayload payload) {
        AABB box = new AABB(payload.min().x, payload.min().y, payload.min().z,
                payload.max().x, payload.max().y, payload.max().z);
        HINTS.put(player, new Hint(payload.targetEntityId(), box, System.currentTimeMillis()));
    }

    /** The precise head box for this shooter's target, or {@code null} if nothing recent matches. */
    public static AABB forTarget(UUID player, int entityId) {
        Hint hint = HINTS.get(player);

        if (hint == null || hint.entityId != entityId
                || System.currentTimeMillis() - hint.recordedAtMillis > MAX_AGE_MILLIS) {
            return null;
        }
        return hint.box;
    }
}
