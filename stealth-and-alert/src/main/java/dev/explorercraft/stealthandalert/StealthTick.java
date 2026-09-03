package dev.explorercraft.stealthandalert;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/// Runs once per mob per tick: look, score, decide, act.
public final class StealthTick {
    private StealthTick() {
    }

    public static void run(Mob mob) {
        if (mob.level().isClientSide()) return;
        if (!StealthTags.is(mob, StealthTags.SEEKERS) && !StealthTags.is(mob, StealthTags.PROTECTED)) return;

        AlertData oldData = mob.getAttachedOrCreate(StealthAndAlert.ALERT);

        // Nobody around and nothing remembered: nothing to do.
        if (mob.level().players().isEmpty() && oldData.targetReactionTicks().isEmpty()) return;

        Set<UUID> trackedPlayers = new HashSet<>(oldData.targetReactionTicks().keySet());
        double range = StealthConfig.MAX_DETECTION_RANGE;
        for (Player p : mob.level().getEntitiesOfClass(Player.class,
                mob.getBoundingBox().inflate(range),
                p -> mob.distanceToSqr(p) <= range * range)) {
            trackedPlayers.add(p.getUUID());
        }

        Map<UUID, StealthEngine.IndividualResult> results = new HashMap<>();
        boolean anyoneVisible = false;

        for (UUID uuid : trackedPlayers) {
            Player player = mob.level().getPlayerByUUID(uuid);
            if (player == null) continue;

            boolean canSee = Perception.shouldArouseAlert(mob, player);
            if (canSee) anyoneVisible = true;

            results.put(uuid, StealthEngine.updateIndividual(
                    player,
                    mob,
                    oldData.state(),
                    oldData.targetAwareness().getOrDefault(uuid, 0.0F),
                    oldData.targetReactionTicks().getOrDefault(uuid, StealthConfig.REACTION_TICKS),
                    oldData.stateOf(uuid),
                    oldData.targetMemoryTicks().getOrDefault(uuid, 0),
                    canSee,
                    StealthAndAlert.visibilityOf(player)
            ));
        }

        StealthEngine.GlobalResult global = StealthEngine.updateGlobalContext(mob, oldData, results, anyoneVisible);
        AlertData newData = assemble(results, global);
        newData = Acoustics.applyToAlert(mob, newData, mob.getAttachedOrCreate(StealthAndAlert.SOUND));
        mob.setAttached(StealthAndAlert.ALERT, newData);
        mob.setAttached(StealthAndAlert.SOUND, AlertSoundData.NONE);

        if (StealthTags.is(mob, StealthTags.PROTECTED) || (mob.isBaby() && !StealthTags.is(mob, StealthTags.SEEKERS))) return;

        Player primary = newData.primaryTarget().map(uuid -> mob.level().getPlayerByUUID(uuid)).orElse(null);
        AlertBehavior.execute(mob, newData, primary != null && Perception.shouldArouseAlert(mob, primary));
    }

    /// Drops players the mob has fully forgotten, so the maps don't grow forever.
    private static AlertData assemble(Map<UUID, StealthEngine.IndividualResult> results, StealthEngine.GlobalResult global) {
        Map<UUID, Float> awareness = new HashMap<>();
        Map<UUID, Integer> states = new HashMap<>();
        Map<UUID, Integer> reactions = new HashMap<>();
        Map<UUID, Integer> memories = new HashMap<>();

        results.forEach((uuid, result) -> {
            boolean forgotten = result.level() <= 0.0F
                    && result.pState() == AlertData.UNTRACKED
                    && result.reaction() >= StealthConfig.REACTION_TICKS
                    && result.memory() <= 0;
            if (forgotten) return;

            awareness.put(uuid, result.level());
            states.put(uuid, result.pState());
            reactions.put(uuid, result.reaction());
            memories.put(uuid, result.memory());
        });

        return new AlertData(
                global.state(), awareness, states, reactions, memories,
                global.lkp(), global.primaryTarget(), global.stateTicks(), global.patienceTicks(),
                global.isSeeingAnyone(), global.willFighting()
        );
    }
}
