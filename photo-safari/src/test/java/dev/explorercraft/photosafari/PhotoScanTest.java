package dev.explorercraft.photosafari;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoScanTest {
    private static final Vec3 EYE = Vec3.ZERO;
    private static final Vec3 NORTH = new Vec3(0.0, 0.0, -1.0);

    @Test
    void subjectStraightAheadIsInFrame() {
        assertTrue(PhotoScan.inFrame(EYE, NORTH, 70.0, 16.0 / 9.0, new Vec3(0.0, 0.0, -10.0)));
    }

    @Test
    void subjectBehindIsNot() {
        assertFalse(PhotoScan.inFrame(EYE, NORTH, 70.0, 16.0 / 9.0, new Vec3(0.0, 0.0, 10.0)));
    }

    @Test
    void subjectOutsideTheVerticalFovIsNot() {
        // 70 degrees vertical means anything above ~0.70 blocks up at 1 block away is out.
        assertTrue(PhotoScan.inFrame(EYE, NORTH, 70.0, 16.0 / 9.0, new Vec3(0.0, 0.6, -1.0)));
        assertFalse(PhotoScan.inFrame(EYE, NORTH, 70.0, 16.0 / 9.0, new Vec3(0.0, 0.8, -1.0)));
    }

    @Test
    void zoomingNarrowsTheFrame() {
        Vec3 edge = new Vec3(0.0, 0.6, -1.0);
        assertTrue(PhotoScan.inFrame(EYE, NORTH, 70.0, 16.0 / 9.0, edge));
        assertFalse(PhotoScan.inFrame(EYE, NORTH, 20.0, 16.0 / 9.0, edge));
    }

    @Test
    void wideAspectKeepsSubjectsThatAreOffToTheSide() {
        Vec3 side = new Vec3(1.0, 0.0, -1.0);
        assertTrue(PhotoScan.inFrame(EYE, NORTH, 70.0, 16.0 / 9.0, side));
        assertFalse(PhotoScan.inFrame(EYE, NORTH, 70.0, 0.5, side));
    }

    @Test
    void looksStraightDownWithoutBlowingUp() {
        Vec3 down = new Vec3(0.0, -1.0, 0.0);
        assertTrue(PhotoScan.inFrame(EYE, down, 70.0, 16.0 / 9.0, new Vec3(0.0, -5.0, 0.0)));
        assertFalse(PhotoScan.inFrame(EYE, down, 70.0, 16.0 / 9.0, new Vec3(0.0, 5.0, 0.0)));
    }
}
