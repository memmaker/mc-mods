package dev.explorercraft.stealthandalert;

import net.minecraft.world.phys.Vec3;

/// Where a point in the world lands on the screen.
///
/// Kept out of the client package on purpose: it is pure geometry, so it can be tested without a
/// running client, and getting the camera's basis vectors wrong is exactly the kind of mistake
/// that only shows up as symbols scattered across the screen.
public final class ScreenProjection {
    private ScreenProjection() {
    }

    /// Screen position in GUI pixels, plus how far in front of the camera the point sits.
    public record Point(double x, double y, double depth) {
    }

    /// Null when the point is behind the camera, where the projection is meaningless.
    public static Point project(Vec3 cameraPos, float pitch, float yaw, double fovDegrees,
                                int width, int height, Vec3 worldPos) {
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
        // Yaw grows clockwise from +Z, so the camera's right hand points along yaw + 90.
        Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F);
        Vec3 up = right.cross(forward);

        Vec3 delta = worldPos.subtract(cameraPos);
        double depth = delta.dot(forward);
        if (depth <= 0.1) return null;

        // Half the screen height over tan(half fov) turns a camera-space ratio into pixels.
        double scale = (height / 2.0) / Math.tan(Math.toRadians(fovDegrees) / 2.0);

        return new Point(
                width / 2.0 + delta.dot(right) / depth * scale,
                height / 2.0 - delta.dot(up) / depth * scale,
                depth
        );
    }
}
