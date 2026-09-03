package dev.explorercraft.stealthandalert;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/// Noise the player makes, and who hears it.
///
/// The original mod hooked fifteen separate loader events. Vanilla already fires a game event for
/// every one of those actions — the same signal sculk sensors listen to — so one hook covers the lot.
public final class Acoustics {
    private Acoustics() {
    }

    /// How loud an action is, how far it carries, and how alarming it sounds.
    private record Noise(double volume, double radius, int threat) {
    }

    private static final Map<Holder<GameEvent>, Noise> NOISES = new HashMap<>();

    static {
        NOISES.put(GameEvent.STEP, new Noise(26.0, 3.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.SWIM, new Noise(24.0, 2.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.SPLASH, new Noise(35.0, 4.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.HIT_GROUND, new Noise(38.0, 4.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.TELEPORT, new Noise(42.0, 3.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.EAT, new Noise(41.0, 2.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.DRINK, new Noise(30.0, 2.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.ENTITY_DAMAGE, new Noise(43.0, 5.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.BLOCK_DESTROY, new Noise(44.0, 5.5, AlertSoundData.LOW));
        NOISES.put(GameEvent.BLOCK_PLACE, new Noise(42.0, 4.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.CONTAINER_OPEN, new Noise(36.0, 5.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.CONTAINER_CLOSE, new Noise(34.0, 5.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.BLOCK_OPEN, new Noise(32.0, 3.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.BLOCK_CLOSE, new Noise(32.0, 3.0, AlertSoundData.LOW));
        NOISES.put(GameEvent.BLOCK_ACTIVATE, new Noise(28.0, 2.5, AlertSoundData.LOW));
        NOISES.put(GameEvent.BLOCK_DEACTIVATE, new Noise(28.0, 2.5, AlertSoundData.LOW));
        NOISES.put(GameEvent.PROJECTILE_SHOOT, new Noise(42.0, 6.0, AlertSoundData.MEDIUM));
        NOISES.put(GameEvent.PROJECTILE_LAND, new Noise(50.0, 7.0, AlertSoundData.MEDIUM));
        NOISES.put(GameEvent.INSTRUMENT_PLAY, new Noise(80.0, 24.0, AlertSoundData.MEDIUM));
        NOISES.put(GameEvent.PRIME_FUSE, new Noise(40.0, 6.0, AlertSoundData.MEDIUM));
        NOISES.put(GameEvent.EXPLODE, new Noise(150.0, 32.0, AlertSoundData.HIGH));
    }

    /// Sprinting is loud, crouching is quiet, and crawling is quieter still.
    private static double stanceMultiplier(Player player) {
        if (player.isSprinting()) return 1.3;
        if (player.isVisuallyCrawling()) return 0.5;
        if (player.isCrouching()) return 0.6;
        return 1.0;
    }

    public static void onGameEvent(Level level, Holder<GameEvent> event, Vec3 pos, Entity source) {
        if (level.isClientSide()) return;
        Noise noise = NOISES.get(event);
        if (noise == null) return;
        if (!(source instanceof Player player) || player.isCreative() || player.isSpectator()) return;

        double multiplier = stanceMultiplier(player);
        emit(level, pos, player, noise.volume() * multiplier, noise.radius() * multiplier, noise.threat());
    }

    /// A noise at a position: every seeker in range works out how much of it reaches its ears.
    public static void emit(Level level, Vec3 pos, Player source, double volume, double radius, int threat) {
        if (volume <= 0 || radius <= 0) return;

        AABB box = new AABB(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius, pos.z + radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && StealthTags.is(m, StealthTags.SEEKERS))) {
            Vec3 ear = mob.getEyePosition();
            double distance = pos.distanceTo(ear);
            if (distance > radius) continue;

            double heard = volume - occlusion(level, pos, ear, distance);
            if (heard <= 0) continue;

            double score = heard + threat * 20.0 - distance * 0.5;
            AlertSoundData current = mob.getAttachedOrCreate(StealthAndAlert.SOUND);
            if (score > current.score()) {
                mob.setAttached(StealthAndAlert.SOUND,
                        new AlertSoundData(Optional.of(source.getUUID()), Optional.of(pos), heard, distance, threat, score));
            }
        }
    }

    /// Walls eat sound: thickness costs 8 per block, and each new material costs another 4.
    private static double occlusion(Level level, Vec3 from, Vec3 to, double distance) {
        Vec3 direction = to.subtract(from).normalize();
        BlockPos origin = BlockPos.containing(from);
        BlockPos.MutableBlockPos cursor = origin.mutable();

        double stepSize = 0.5;
        int steps = (int) (distance / stepSize);
        double thickness = 0.0;
        int layers = 0;
        BlockState last = null;

        for (int i = 0; i < steps; i++) {
            Vec3 point = from.add(direction.scale(i * stepSize));
            cursor.set(point.x, point.y, point.z);
            if (cursor.equals(origin)) continue;

            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && state.isCollisionShapeFullBlock(level, cursor)) {
                thickness += stepSize;
                if (last == null || !state.getBlock().equals(last.getBlock())) {
                    layers++;
                }
            }
            last = state;
        }

        return thickness * 8.0 + layers * 4.0;
    }

    /// Folds what the mob heard into what it saw. A noise can only raise the alert, never lower it.
    public static AlertData applyToAlert(Mob mob, AlertData visual, AlertSoundData sound) {
        if (sound.pos().isEmpty() || sound.source().isEmpty()) return visual;
        if (visual.canSeeAnyone() || visual.willFighting() || visual.state() == AlertData.FIGHTING) return visual;

        double volume = sound.volume();
        double distance = sound.distance();
        int threat = sound.threatLevel();
        boolean searchingAround = mob.getAttachedOrCreate(StealthAndAlert.SEARCH).searchingAround();
        boolean sameFloor = Math.abs(mob.getY() - sound.pos().get().y) <= 10;

        // -1 means the noise was not worth reacting to; anything else is the state to move to.
        int newState = switch (visual.state()) {
            case AlertData.IDLE -> {
                if (volume < 34.0) yield -1;
                yield threat <= AlertSoundData.MEDIUM && volume <= 42.0 ? AlertData.SUSPICIOUS : AlertData.SEARCHING;
            }
            case AlertData.SUSPICIOUS -> {
                if (volume < 30.0) yield -1;
                double ignoreBeyond = switch (threat) {
                    case AlertSoundData.LOW -> 7.0;
                    case AlertSoundData.MEDIUM -> 12.0;
                    default -> Double.MAX_VALUE;
                };
                double investigateWithin = switch (threat) {
                    case AlertSoundData.LOW -> 6.0;
                    case AlertSoundData.MEDIUM -> 10.0;
                    default -> Double.MAX_VALUE;
                };
                if (distance > ignoreBeyond) yield -1;
                yield distance > investigateWithin ? AlertData.SUSPICIOUS : AlertData.SEARCHING;
            }
            case AlertData.SEARCHING -> {
                // Already looking: only redirect if the mob has finished walking to its current spot.
                if (volume < 28.0) yield -1;
                double reach = switch (threat) {
                    case AlertSoundData.LOW -> 16.0;
                    case AlertSoundData.MEDIUM -> 20.0;
                    default -> 32.0;
                };
                if (distance > reach) yield -1;
                yield threat == AlertSoundData.HIGH || (searchingAround && sameFloor) ? AlertData.SEARCHING : -1;
            }
            default -> -1;
        };

        if (newState < 0) return visual;
        if (newState == AlertData.SEARCHING) {
            mob.setAttached(StealthAndAlert.SEARCH, SearchData.DEFAULT);
        }

        return new AlertData(newState, visual.targetAwareness(), visual.targetStates(), visual.targetReactionTicks(),
                visual.targetMemoryTicks(), sound.pos(), visual.primaryTarget(), 0, StealthConfig.PATIENCE_TICKS,
                visual.canSeeAnyone(), visual.willFighting());
    }
}
