package dev.explorercraft.photosafari;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/// Decides whether an entity was actually captured on a photo: inside the camera
/// frustum, close enough, and not hidden behind blocks.
public final class PhotoScan {
    public static final double MAX_DISTANCE = 64.0;

    /// Field of view the server assumes when verifying a client report. Zooming only ever
    /// narrows the client frustum, so a wide cone here is a safe upper bound.
    private static final double SERVER_FOV_DEGREES = 110.0;
    private static final double SERVER_ASPECT = 2.0;

    private PhotoScan() {
    }

    /// Only things that move on their own count towards the safari, so no players,
    /// armor stands or dropped items.
    public static boolean isWildlife(Entity entity) {
        return entity instanceof Mob && isWildlifeType(entity.getType());
    }

    /// A spawn egg is the closest thing to "this is a mob you can meet". Asking the game
    /// which egg spawns which type also catches mods that name their eggs their own way.
    public static boolean isWildlifeType(EntityType<?> type) {
        return SpawnEggItem.byId(type).isPresent();
    }

    public static boolean isPhotographed(Level level, Vec3 eye, Vec3 look, double fovDegrees, double aspect, Entity target) {
        if (target.isInvisible() || target.isRemoved()) {
            return false;
        }

        AABB box = target.getBoundingBox();
        if (eye.distanceTo(box.getCenter()) > MAX_DISTANCE) {
            return false;
        }

        if (!inFrame(eye, look, fovDegrees, aspect, box.getCenter())) {
            return false;
        }

        return hasLineOfSight(level, eye, box, target);
    }

    /// Server-side re-check of a client report, using a generous frustum.
    public static boolean isPhotographedLenient(Level level, Vec3 eye, Vec3 look, Entity target) {
        return isPhotographed(level, eye, look, SERVER_FOV_DEGREES, SERVER_ASPECT, target);
    }

    static boolean inFrame(Vec3 eye, Vec3 look, double fovDegrees, double aspect, Vec3 point) {
        Vec3 forward = look.normalize();
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0e-6) {
            // Looking straight up or down, any horizontal axis will do.
            right = new Vec3(1.0, 0.0, 0.0);
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        Vec3 delta = point.subtract(eye);
        double depth = delta.dot(forward);
        if (depth <= 0.0) {
            return false;
        }

        double tanY = Math.tan(Math.toRadians(fovDegrees) / 2.0);
        double tanX = tanY * aspect;
        return Math.abs(delta.dot(up)) / depth <= tanY
                && Math.abs(delta.dot(right)) / depth <= tanX;
    }

    /// Visible if any sample point on the entity can be reached without hitting a block.
    private static boolean hasLineOfSight(Level level, Vec3 eye, AABB box, Entity target) {
        AABB inset = box.deflate(box.getXsize() * 0.1, box.getYsize() * 0.1, box.getZsize() * 0.1);
        Vec3[] samples = {
                box.getCenter(),
                new Vec3(inset.minX, inset.maxY, inset.minZ),
                new Vec3(inset.maxX, inset.maxY, inset.maxZ),
                new Vec3(inset.minX, inset.minY, inset.maxZ),
                new Vec3(inset.maxX, inset.minY, inset.minZ),
        };

        for (Vec3 sample : samples) {
            // ponytail: blocks only. Entities hiding behind other entities still count,
            // switch to a per-entity raycast if that turns out to matter.
            HitResult hit = level.clip(new ClipContext(eye, sample,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        return false;
    }
}
