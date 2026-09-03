package dev.explorercraft.stealthandalert;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/// Turns the alert state into what the mob actually does: stare, chase, or go and look.
public final class AlertBehavior {
    private AlertBehavior() {
    }

    public static void execute(Mob mob, AlertData data, boolean canSeePrimary) {
        if (data.state() == AlertData.IDLE) return;

        Player primary = data.primaryTarget()
                .map(uuid -> mob.level().getPlayerByUUID(uuid))
                .orElse(null);

        if (canSeePrimary && primary != null) {
            int pState = data.stateOf(primary.getUUID());
            if (pState < AlertData.AWARE) return;

            mob.getLookControl().setLookAt(primary, 30.0F, 30.0F);

            if (data.state() == AlertData.FIGHTING && pState == AlertData.TRACKING) {
                if (mob instanceof Piglin piglin) {
                    piglin.getBrain().eraseMemory(MemoryModuleType.PACIFIED);
                    piglin.getBrain().setMemory(MemoryModuleType.ANGRY_AT, primary.getUUID());
                }
                mob.setTarget(primary);
            } else {
                // Seen but not yet confirmed: the mob watches instead of charging.
                if (mob.getTarget() == primary) mob.setTarget(null);
                if (!mob.getNavigation().isDone()) mob.getNavigation().stop();
            }
            mob.setAttached(StealthAndAlert.SEARCH, SearchData.DEFAULT);
            return;
        }

        if (mob.getTarget() != null && mob.getTarget().equals(primary)) {
            if (StealthAndAlert.visibilityOf(primary) <= StealthConfig.VISIBILITY_THRESHOLD + 0.0001
                    || primary.isCreative() || primary.isSpectator()) {
                mob.setTarget(null);
            } else if (data.state() < AlertData.FIGHTING && data.stateOf(primary.getUUID()) < AlertData.TRACKING) {
                mob.setTarget(null);
            }
        }

        Vec3 lkp = data.lastKnownPos().orElse(null);
        if (lkp == null) return;

        if (data.state() == AlertData.SUSPICIOUS) {
            mob.setAttached(StealthAndAlert.SEARCH, SearchData.DEFAULT);
            mob.getLookControl().setLookAt(lkp.x, lkp.y + mob.getEyeHeight(), lkp.z, 30.0F, 30.0F);
            mob.getNavigation().stop();
        } else if (data.state() == AlertData.SEARCHING || (data.state() == AlertData.FIGHTING && !data.canSeeAnyone())) {
            investigate(mob, lkp);
        }
    }

    /// Walk to the last known position, then wander around it until patience runs out.
    private static void investigate(Mob mob, Vec3 lkp) {
        SearchData data = mob.getAttachedOrCreate(StealthAndAlert.SEARCH);
        boolean searchingAround = data.searchingAround();
        boolean moving = data.moving();
        int stayTicks = data.stayTicks();
        Vec3 targetPos = searchingAround ? data.targetPos().orElse(null) : lkp;

        if (targetPos == null) {
            mob.setAttached(StealthAndAlert.SEARCH, SearchData.DEFAULT);
            return;
        }

        targetPos = groundPos(mob, targetPos);
        double distSqr = mob.distanceToSqr(targetPos);

        if (distSqr > 2.25 && !searchingAround) {
            mob.getLookControl().setLookAt(targetPos.x, targetPos.y + mob.getEyeHeight(), targetPos.z);
            mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);

            if (mob instanceof EnderMan && (distSqr > 64.0 || mob.getNavigation().isStuck()) && mob.getRandom().nextInt(100) == 0) {
                tryTeleportNear(mob, targetPos);
            }
            if (mob.getAttachedOrCreate(StealthAndAlert.ALERT).patienceTicks() <= 0) {
                searchingAround = true;
                moving = false;
            }
        } else if (!searchingAround) {
            searchingAround = true;
            moving = false;
        } else if (!moving) {
            mob.getNavigation().stop();
            if (stayTicks <= 0) {
                stayTicks = 100 + mob.getRandom().nextInt(100);
            }
            if (--stayTicks <= 0) {
                moving = true;
                targetPos = nextSearchPoint(mob, lkp);
            }
        } else {
            mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
            if (mob.getNavigation().isDone()) {
                moving = false;
            }
        }

        mob.setAttached(StealthAndAlert.SEARCH, new SearchData(searchingAround, moving, stayTicks, Optional.of(targetPos)));
    }

    private static Vec3 nextSearchPoint(Mob mob, Vec3 lkp) {
        double radius = 4.0 + mob.getRandom().nextDouble() * 6.0;
        double angle = mob.getRandom().nextDouble() * Math.PI * 2;
        return new Vec3(lkp.x + Math.cos(angle) * radius, lkp.y, lkp.z + Math.sin(angle) * radius);
    }

    private static boolean tryTeleportNear(Mob mob, Vec3 targetPos) {
        for (int i = 0; i < 10; i++) {
            double dx = targetPos.x + (mob.getRandom().nextDouble() - 0.5) * 8.0;
            double dy = targetPos.y + (mob.getRandom().nextInt(7) - 3);
            double dz = targetPos.z + (mob.getRandom().nextDouble() - 0.5) * 8.0;
            Vec3 from = mob.position();
            if (mob.randomTeleport(dx, dy, dz, true)) {
                mob.level().gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(mob));
                if (!mob.isSilent()) {
                    mob.level().playSound(null, from.x(), from.y(), from.z(), SoundEvents.ENDERMAN_TELEPORT, mob.getSoundSource(), 1.0F, 1.0F);
                    mob.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
                mob.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    /// A position in mid-air is useless to a walking mob: drop it onto the nearest floor within 4 blocks.
    private static Vec3 groundPos(Mob mob, Vec3 pos) {
        double heightDifference = pos.y - mob.getY();
        if (heightDifference > 0 && heightDifference <= 1.75) {
            return new Vec3(pos.x, mob.getY(), pos.z);
        }

        BlockPos.MutableBlockPos mutable = BlockPos.containing(pos).mutable();
        int scanDepth = 0;
        boolean foundFloor = false;

        while (mutable.getY() >= mob.level().getMinY() && scanDepth < 4) {
            BlockState state = mob.level().getBlockState(mutable);
            if (!state.isAir() && !state.getCollisionShape(mob.level(), mutable).isEmpty()) {
                foundFloor = true;
                break;
            }
            mutable.move(0, -1, 0);
            scanDepth++;
            if (!mob.level().hasChunk(mutable.getX() >> 4, mutable.getZ() >> 4)) break;
        }

        return foundFloor ? Vec3.atBottomCenterOf(mutable.above()) : new Vec3(pos.x, mob.getY(), pos.z);
    }
}
