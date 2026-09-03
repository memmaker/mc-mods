package dev.explorercraft.stealthandalert;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

import java.util.EnumSet;

/// Vanilla idle head-turning, suppressed while the mob is alert so it doesn't fight our look control.
public class StealthLookAroundGoal extends RandomLookAroundGoal {
    private final Mob mob;

    public StealthLookAroundGoal(Mob mob) {
        super(mob);
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    private boolean busy() {
        return mob.getAttachedOrCreate(StealthAndAlert.ALERT).state() > AlertData.IDLE
                && !mob.getAttachedOrCreate(StealthAndAlert.SEARCH).searchingAround();
    }

    @Override
    public boolean canUse() {
        return !busy() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !busy() && super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        super.tick();
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.getNavigation().stop();
    }
}
