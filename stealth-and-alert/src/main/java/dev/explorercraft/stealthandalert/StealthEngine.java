package dev.explorercraft.stealthandalert;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/// The awareness maths. Pure logic, no world mutation, ported straight from the original mod.
public final class StealthEngine {
    private StealthEngine() {
    }

    public record IndividualResult(float level, int reaction, int pState, int memory) {
    }

    public record GlobalResult(
            int state,
            Optional<Vec3> lkp,
            Optional<UUID> primaryTarget,
            int stateTicks,
            int patienceTicks,
            boolean isSeeingAnyone,
            boolean willFighting
    ) {
    }

    /// One mob's read on one player: react, fill the bar, lock on, or forget.
    public static IndividualResult updateIndividual(
            Player player,
            Mob mob,
            int state,
            float currentLevel,
            int currentReaction,
            int currentPState,
            int currentMemory,
            boolean canSee,
            double visibility
    ) {
        float nextLevel = currentLevel;
        int nextReaction = currentReaction;
        int nextPState = currentPState;
        int nextMemory = currentMemory;

        if (!player.isAlive()) {
            return new IndividualResult(0F, StealthConfig.REACTION_TICKS, AlertData.UNTRACKED, 0);
        }

        if (canSee) {
            // Reaction delay before the mob starts filling the bar at all.
            if (currentPState == AlertData.UNTRACKED) {
                if (currentReaction > 0) {
                    nextReaction--;
                    nextMemory = Math.max(0, currentMemory - 1);
                } else {
                    nextPState = AlertData.AWARE;
                }
            }

            if (nextPState == AlertData.AWARE) {
                nextReaction = 0;

                double rawVis = Math.clamp(visibility, StealthConfig.VISIBILITY_THRESHOLD, 1.0);
                double t = (rawVis - StealthConfig.VISIBILITY_THRESHOLD) / (1.0 - StealthConfig.VISIBILITY_THRESHOLD);
                double minBase = 0.6;
                double visModifier = minBase + (1.0 - minBase) * Math.pow(t, 0.5);
                visModifier *= StealthConfig.INCREASE_VISIBILITY_FACTOR;
                visModifier = Math.clamp(visModifier, 0.1, 10.0);

                double distanceSqr = mob.distanceToSqr(player);
                double distModifier = 1.0;
                if (distanceSqr <= 256) {
                    double dist = Math.clamp(Math.sqrt(distanceSqr), 0.4, 16);
                    distModifier = Math.pow(8.0 / dist, 2.5);
                    distModifier = Math.clamp(distModifier, 1, 300);
                    distModifier *= StealthConfig.INCREASE_DISTANCE_FACTOR;
                }

                double stateModifier = 1.0;
                if (state == AlertData.SUSPICIOUS) {
                    stateModifier *= StealthConfig.INCREASE_SUSPICIOUS_FACTOR;
                } else if (state == AlertData.SEARCHING) {
                    stateModifier *= StealthConfig.INCREASE_SEARCHING_FACTOR;
                }

                double rate = StealthConfig.INCREASE_BASIC_RATE * visModifier * distModifier * stateModifier;
                nextLevel = (float) Math.min(100.0, currentLevel + rate);
                if (nextLevel >= 100.0F) {
                    nextPState = AlertData.TRACKING;
                }
            }

            if (nextPState == AlertData.TRACKING) {
                nextLevel = 100.0F;
                nextReaction = 0;
                nextMemory = StealthConfig.MEMORY_TICKS;
            }
        } else {
            if (currentPState == AlertData.TRACKING) {
                // Locked on but out of sight: hold the lock for a short grace period.
                if (nextReaction <= 0) {
                    nextReaction = StealthConfig.TRACKING_TICKS;
                    if (nextReaction <= 0) nextPState = AlertData.AWARE;
                } else {
                    nextReaction--;
                    if (nextReaction <= 0) {
                        nextPState = AlertData.AWARE;
                    }
                }
            } else {
                float stateModifier = 1F;
                if (state == AlertData.SUSPICIOUS) {
                    stateModifier = StealthConfig.DECREASE_SUSPICIOUS_FACTOR;
                } else if (state == AlertData.SEARCHING) {
                    stateModifier = StealthConfig.DECREASE_SEARCHING_FACTOR;
                }
                nextLevel = Math.max(0.0F, currentLevel - StealthConfig.DECREASE_BASIC_RATE * stateModifier);
                nextMemory = Math.max(0, currentMemory - 1);
            }

            if (nextLevel <= 0.0F) {
                nextPState = AlertData.UNTRACKED;
                nextReaction = StealthConfig.REACTION_TICKS;
            }
        }
        return new IndividualResult(nextLevel, nextReaction, nextPState, nextMemory);
    }

    /// Merges every player's result into the mob's own state: who it hunts, where it looks, when it calms down.
    public static GlobalResult updateGlobalContext(
            Mob mob,
            AlertData oldData,
            Map<UUID, IndividualResult> currentResults,
            boolean anyTargetVisible
    ) {
        int nextState = oldData.state();
        int nextStateTicks = oldData.stateChangeTicks();
        int nextPatienceTicks = oldData.patienceTicks();
        Optional<Vec3> nextLkp = oldData.lastKnownPos();
        Optional<UUID> nextPrimary = oldData.primaryTarget();
        boolean willFighting = oldData.willFighting();

        float maxLevel = 0.0F;
        for (IndividualResult res : currentResults.values()) {
            maxLevel = Math.max(maxLevel, res.level());
        }

        // Primary target contest: highest awareness wins, ties broken by distance.
        UUID topTargetUuid = null;
        UUID oldId = nextPrimary.orElse(null);
        Player oldPlayer = null;

        if (oldId != null) {
            oldPlayer = mob.level().getPlayerByUUID(oldId);
            if (oldPlayer == null || !oldPlayer.isAlive()) {
                nextPrimary = Optional.empty();
            }
        }

        if (nextPrimary.isPresent() && currentResults.containsKey(oldId) && currentResults.get(oldId).level() >= maxLevel) {
            topTargetUuid = oldId;
            for (Map.Entry<UUID, IndividualResult> entry : currentResults.entrySet()) {
                UUID candidateId = entry.getKey();
                if (candidateId.equals(oldId)) continue;
                if (entry.getValue().level() < maxLevel) continue;

                Player candidate = mob.level().getPlayerByUUID(candidateId);
                if (candidate != null
                        && entry.getValue().pState() >= AlertData.AWARE
                        && Perception.shouldArouseAlert(mob, candidate)
                        && mob.distanceToSqr(candidate) < mob.distanceToSqr(oldPlayer)) {
                    topTargetUuid = candidateId;
                    oldPlayer = candidate;
                }
            }
        }

        if (topTargetUuid == null && maxLevel > 0.0F && anyTargetVisible) {
            for (Map.Entry<UUID, IndividualResult> entry : currentResults.entrySet()) {
                if (entry.getValue().level() < maxLevel) continue;
                UUID candidateId = entry.getKey();
                Player p = mob.level().getPlayerByUUID(candidateId);
                if (p == null || !Perception.shouldArouseAlert(mob, p) || entry.getValue().pState() < AlertData.AWARE) {
                    continue;
                }
                if (topTargetUuid == null) {
                    topTargetUuid = candidateId;
                } else {
                    Player winner = mob.level().getPlayerByUUID(topTargetUuid);
                    if (winner != null && mob.distanceToSqr(p) < mob.distanceToSqr(winner)) {
                        topTargetUuid = candidateId;
                    }
                }
            }
            nextPrimary = Optional.ofNullable(topTargetUuid);
        }

        if (anyTargetVisible) {
            if (maxLevel > 0.0F) {
                nextPatienceTicks = StealthConfig.PATIENCE_TICKS;
                if (!willFighting) {
                    nextStateTicks = 0;
                }
            }

            boolean anyoneTracking = currentResults.values().stream()
                    .anyMatch(r -> r.pState() == AlertData.TRACKING);

            if (anyoneTracking) {
                // Short beat between "spotted you" and the mob actually attacking.
                if (nextState < AlertData.FIGHTING && !willFighting) {
                    nextState = AlertData.SEARCHING;
                    nextStateTicks = 10;
                    willFighting = true;
                }
            } else if (!willFighting) {
                if (maxLevel >= 50.0F && nextState < AlertData.SEARCHING) {
                    nextState = AlertData.SEARCHING;
                } else if (maxLevel > 0.0F && nextState < AlertData.SUSPICIOUS) {
                    nextState = AlertData.SUSPICIOUS;
                }
            }

            if (nextStateTicks > 0) {
                nextStateTicks--;
                if (nextStateTicks <= 0 && willFighting) {
                    nextState = AlertData.FIGHTING;
                    willFighting = false;
                }
            } else if (willFighting) {
                willFighting = false;
            }

            if (nextPrimary.isPresent()) {
                Player primary = mob.level().getPlayerByUUID(nextPrimary.get());
                if (primary != null && Perception.shouldArouseAlert(mob, primary)) {
                    nextLkp = Optional.of(primary.position());
                }
            }
        } else {
            if (!willFighting) {
                if (nextState == AlertData.FIGHTING) {
                    if (nextStateTicks <= 0) {
                        nextStateTicks = StealthConfig.TRACKING_TICKS;
                    }
                    if (nextPrimary.isPresent() && oldData.stateOf(nextPrimary.get()) == AlertData.TRACKING) {
                        Player primary = mob.level().getPlayerByUUID(nextPrimary.get());
                        if (primary != null) {
                            nextLkp = Optional.of(primary.position());
                        }
                    }
                }
                if (maxLevel <= 0.0F) {
                    if (nextState == AlertData.SEARCHING) {
                        boolean reachedLkp = nextLkp.isPresent() && mob.distanceToSqr(nextLkp.get()) < 4.0;
                        if ((reachedLkp || --nextPatienceTicks <= 1) && nextStateTicks <= 0) {
                            nextStateTicks = 600;
                        }
                    } else if (nextState == AlertData.SUSPICIOUS && nextStateTicks <= 0) {
                        nextStateTicks = 300;
                    }
                }
            }

            if (nextStateTicks > 0) {
                nextStateTicks--;
                if (nextStateTicks <= 0) {
                    if (willFighting) {
                        nextState = AlertData.FIGHTING;
                        willFighting = false;
                    } else if (nextState == AlertData.FIGHTING) {
                        nextState = AlertData.SEARCHING;
                    } else {
                        nextState = AlertData.IDLE;
                        nextLkp = Optional.empty();
                        nextPrimary = Optional.empty();
                        nextPatienceTicks = StealthConfig.PATIENCE_TICKS;
                    }
                }
            }
        }
        return new GlobalResult(nextState, nextLkp, nextPrimary, nextStateTicks, nextPatienceTicks, anyTargetVisible, willFighting);
    }
}
