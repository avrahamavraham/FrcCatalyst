package frc.lib.catalyst.mechanisms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TurretMechanism#resolveTurretAngle} — the continuous-angle
 * resolver that keeps a limited-travel turret from unwinding its wiring while a
 * target sweeps across the robot.
 */
class TurretMathTest {

    private static final double MIN = -270, MAX = 270;

    @Test
    void picksClosestReachableRepresentation() {
        // Want to point at 170 deg while currently near -170 deg. The short way
        // is through -190 (= 170 - 360), which is in range.
        double r = TurretMechanism.resolveTurretAngle(170, -170, MIN, MAX);
        assertEquals(-190, r, 1e-9);
        assertTrue(Math.abs(r - (-170)) <= 180);
    }

    @Test
    void staysPutWhenAlreadyThere() {
        assertEquals(90, TurretMechanism.resolveTurretAngle(90, 90, MIN, MAX), 1e-9);
    }

    @Test
    void takesLongWayWhenShortWayOutOfRange() {
        // Narrow turret: only +/-100 deg of travel.
        // At +95, asked to go to -95. Short way (-95) is in range and closest.
        double r = TurretMechanism.resolveTurretAngle(-95, 95, -100, 100);
        assertEquals(-95, r, 1e-9);
        // But asked to reach a bearing only representable past the limit:
        // desired 150 from current 95, with +/-100 range. 150 out of range;
        // 150-360 = -210 out of range. Nearest representation clamps into range.
        double r2 = TurretMechanism.resolveTurretAngle(150, 95, -100, 100);
        assertTrue(r2 >= -100 && r2 <= 100, "must clamp into travel; got " + r2);
    }

    @Test
    void resolvedAngleAlwaysWithinLimits() {
        for (double desired = -540; desired <= 540; desired += 37) {
            for (double current = MIN; current <= MAX; current += 53) {
                double r = TurretMechanism.resolveTurretAngle(desired, current, MIN, MAX);
                assertTrue(r >= MIN - 1e-6 && r <= MAX + 1e-6,
                        "out of range: desired=" + desired + " current=" + current + " -> " + r);
                // resolved must represent the same physical bearing as desired
                double diff = Math.IEEEremainder(r - desired, 360.0);
                assertEquals(0.0, diff, 1e-6,
                        "resolved is not a 360k of desired: " + r + " vs " + desired);
            }
        }
    }
}
