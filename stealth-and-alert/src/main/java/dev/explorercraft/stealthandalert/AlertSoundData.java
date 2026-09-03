package dev.explorercraft.stealthandalert;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/// The loudest noise a mob heard this tick. Rebuilt every tick, never saved.
public record AlertSoundData(Optional<UUID> source, Optional<Vec3> pos, double volume, double distance,
                            int threatLevel, double score) {
    public static final int LOW = 0;
    public static final int MEDIUM = 1;
    public static final int HIGH = 2;

    public static final AlertSoundData NONE = new AlertSoundData(Optional.empty(), Optional.empty(), 0, 0, LOW, 0);
}
