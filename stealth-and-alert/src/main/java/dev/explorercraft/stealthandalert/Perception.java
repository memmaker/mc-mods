package dev.explorercraft.stealthandalert;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/// How visible a player is, and whether a given mob can actually see them.
public final class Perception {
    private Perception() {
    }

    /// Light, cover and stance rolled into one 0..1 number. The HUD eye shows this.
    public static double visibility(Player player) {
        if (fullyHidden(player)) return 0.0;

        Level level = player.level();
        BlockPos pos = player.blockPosition();

        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.dimensionType().hasSkyLight()
                ? Math.max(0, level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken())
                : 0;

        int ambientLight = Math.max(blockLight, skyLight);
        if (!level.dimensionType().hasSkyLight() && ambientLight == 0) {
            // Pitch black in the End or Nether still isn't pitch black.
            if (level.dimension() == Level.END) {
                ambientLight = 3;
            } else if (level.dimension() == Level.NETHER) {
                ambientLight = 4;
            }
        }

        double adjustedLight = Math.sqrt(ambientLight / 15.0);
        double visibility = 0.15 + adjustedLight * 0.85;

        if (inTallGrass(level, pos)) {
            visibility *= 0.5;
        }

        if (player.isVisuallyCrawling() || player.isVisuallySwimming()) {
            visibility *= 0.65;
        } else if (player.isCrouching()) {
            visibility *= 0.8;
        }

        if (player.isSprinting()) {
            visibility *= 1.25;
        }

        return Math.clamp(visibility, 0.0, 1.0);
    }

    /// The full check a mob runs before its awareness bar moves at all.
    public static boolean shouldArouseAlert(Mob mob, Player player) {
        if (mob.level().getDifficulty() == Difficulty.PEACEFUL) return false;
        if (player == null || !player.isAlive() || !mob.isAlive()) return false;
        if (player.isCreative() || player.isSpectator()) return false;
        if (isPlayerPet(mob, player)) return false;
        if (StealthTags.is(mob, StealthTags.PROTECTED)) return false;

        double distanceSqr = mob.distanceToSqr(player);
        double visibility = StealthAndAlert.visibilityOf(player);

        // Effectively invisible: only noticed at arm's length, a little further if already locked on.
        if (visibility <= StealthConfig.VISIBILITY_THRESHOLD + 0.0001) {
            double minDistance = mob.getAttachedOrCreate(StealthAndAlert.ALERT).stateOf(player.getUUID()) < AlertData.TRACKING
                    ? StealthConfig.MIN_INVISIBLE_DISTANCE
                    : StealthConfig.MIN_INVISIBLE_DISTANCE_TO_TRACKING;
            if (distanceSqr > minDistance * minDistance) return false;
        }

        boolean touching = mob.getBoundingBox().intersects(player.getBoundingBox().inflate(0.4));
        return touching || hasLineOfSight(mob, player, visibility);
    }

    /// Range, vision cone, then a multi-point ray check against the hitbox.
    public static boolean hasLineOfSight(Mob observer, Entity target, double visibility) {
        double maxDistance = StealthConfig.MAX_DETECTION_RANGE * (1.0 - rangeReduction(visibility));
        if (observer.distanceToSqr(target) > maxDistance * maxDistance) return false;

        Vec3 eyePos = observer.getEyePosition();
        Vec3 lookVec = observer.getViewVector(1.0F);
        Vec3 targetDir = target.getEyePosition().subtract(eyePos).normalize();

        return withinFov(lookVec, targetDir) && canSeeAnyPart(observer, target, eyePos);
    }

    /// Hiding in the dark shrinks how far a mob can spot you, on a smoothstep curve.
    private static double rangeReduction(double visibility) {
        double linear = Math.clamp((visibility - StealthConfig.VISIBILITY_THRESHOLD) / (1.0 - StealthConfig.VISIBILITY_THRESHOLD), 0.0, 1.0);
        double stealth = 1.0 - linear;
        return StealthConfig.MAX_RANGE_REDUCTION * stealth * stealth * (3.0 - 2.0 * stealth);
    }

    private static boolean withinFov(Vec3 lookVec, Vec3 targetDir) {
        boolean lookingStraightUpOrDown = Math.abs(lookVec.x) < 0.0001 && Math.abs(lookVec.z) < 0.0001;

        if (!lookingStraightUpOrDown) {
            Vec3 lookHorizontal = new Vec3(lookVec.x, 0, lookVec.z).normalize();
            Vec3 targetHorizontal = new Vec3(targetDir.x, 0, targetDir.z).normalize();
            double threshold = Math.cos(Math.toRadians(StealthConfig.HORIZONTAL_FOV) / 2.0);
            if (lookHorizontal.dot(targetHorizontal) < threshold) return false;
        }

        // Pitch is measured relative to where the mob is currently looking, not the horizon.
        double relativePitch = Math.toDegrees(Math.asin(targetDir.y)) - Math.toDegrees(Math.asin(lookVec.y));
        return relativePitch >= -StealthConfig.VERTICAL_DOWN_FOV && relativePitch <= StealthConfig.VERTICAL_UP_FOV;
    }

    private static boolean canSeeAnyPart(Mob observer, Entity target, Vec3 start) {
        double halfW = target.getBbWidth() / 2.0 * 0.8;
        double chestY = target.getY() + target.getBbHeight() * 0.5;
        double footY = target.getY() + 0.1;

        Vec3[] checkPoints = {
                target.getEyePosition(),
                new Vec3(target.getX(), chestY, target.getZ()),
                new Vec3(target.getX(), footY, target.getZ()),
                new Vec3(target.getX() + halfW, chestY, target.getZ() + halfW),
                new Vec3(target.getX() - halfW, chestY, target.getZ() - halfW),
                new Vec3(target.getX() + halfW, chestY, target.getZ() - halfW),
                new Vec3(target.getX() - halfW, chestY, target.getZ() + halfW)
        };

        for (Vec3 end : checkPoints) {
            if (observer.level().clip(clipContext(observer, start, end)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    /// Like a normal ray, except glass and fences don't hide you.
    private static ClipContext clipContext(Entity beginner, Vec3 start, Vec3 end) {
        return new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, beginner) {
            @Override
            public VoxelShape getBlockShape(BlockState blockState, BlockGetter level, BlockPos pos) {
                return blockState.is(StealthTags.SEE_THROUGHS) ? Shapes.empty() : super.getBlockShape(blockState, level, pos);
            }
        };
    }

    public static boolean isPlayerPet(Entity entity, Player player) {
        if (player == null) return false;
        if (entity instanceof OwnableEntity ownable) {
            return ownable.getOwner() == player;
        }
        if (entity instanceof IronGolem golem) {
            return golem.isPlayerCreated();
        }
        return entity instanceof SnowGolem;
    }

    private static boolean fullyHidden(Player player) {
        Level level = player.level();
        BlockPos center = player.blockPosition();

        if (inLargePatch(level, center, false)) return true;
        if (player.isVisuallyCrawling() && inLargePatch(level, center, true)) return true;

        return level.getBlockState(center).is(BlockTags.LEAVES) && level.getBlockState(center.above()).is(BlockTags.LEAVES);
    }

    private static boolean inTallGrass(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(StealthTags.CAN_COVER) && level.getBlockState(pos.above()).is(StealthTags.CAN_COVER);
    }

    /// A 2x2 patch of cover around the player hides them completely; a single block does not.
    private static boolean inLargePatch(Level level, BlockPos center, boolean shortGrass) {
        BlockPos[] origins = {center, center.north(), center.west(), center.north().west()};
        for (BlockPos origin : origins) {
            if (isPatch(level, origin, shortGrass)) return true;
        }
        return false;
    }

    private static boolean isPatch(Level level, BlockPos origin, boolean shortGrass) {
        BlockPos[] feet = {origin, origin.east(), origin.south(), origin.east().south()};
        for (BlockPos foot : feet) {
            BlockState state = level.getBlockState(foot);
            if (shortGrass) {
                if (!state.is(Blocks.SHORT_GRASS) && !state.is(Blocks.FERN) && !state.is(BlockTags.SMALL_FLOWERS)) return false;
            } else {
                if (!state.is(StealthTags.CAN_COVER) || !level.getBlockState(foot.above()).is(StealthTags.CAN_COVER)) return false;
            }
        }
        return true;
    }
}
