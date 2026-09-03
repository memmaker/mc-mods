package dev.explorercraft.stealthandalert;

/// Tuning knobs, carried over from the original mod's NeoForge config defaults.
/// ponytail: constants, not a config file. Wire up a config screen if players ask to tune it.
public final class StealthConfig {
    private StealthConfig() {
    }

    public static final double MAX_DETECTION_RANGE = 48.0;
    public static final double HORIZONTAL_FOV = 120.0;
    public static final double VERTICAL_UP_FOV = 45.0;
    public static final double VERTICAL_DOWN_FOV = 60.0;

    public static final int REACTION_TICKS = 10;
    public static final int TRACKING_TICKS = 30;
    public static final int PATIENCE_TICKS = 800;
    public static final int MEMORY_TICKS = 2000;

    public static final double VISIBILITY_THRESHOLD = 0.05;
    public static final double MAX_RANGE_REDUCTION = 0.6;
    public static final double MIN_INVISIBLE_DISTANCE = 2.0;
    public static final double MIN_INVISIBLE_DISTANCE_TO_TRACKING = 5.0;

    public static final double INCREASE_BASIC_RATE = 2.8;
    public static final double INCREASE_VISIBILITY_FACTOR = 1.0;
    public static final double INCREASE_DISTANCE_FACTOR = 1.0;
    public static final double INCREASE_SUSPICIOUS_FACTOR = 1.0;
    public static final double INCREASE_SEARCHING_FACTOR = 1.3;

    public static final float DECREASE_BASIC_RATE = 1.8F;
    public static final float DECREASE_SUSPICIOUS_FACTOR = 1.0F;
    public static final float DECREASE_SEARCHING_FACTOR = 0.5F;
}
