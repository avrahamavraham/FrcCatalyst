package frc.lib.catalyst.util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

/**
 * Hardware-independent aiming math for a turreted shooter, including
 * Shoot-On-The-Fly (SOTF) motion compensation.
 *
 * <p>This class touches no motors and no NetworkTables — it's pure
 * geometry, so it can be unit-tested without a robot. Feed it the fused
 * robot pose and the field-relative chassis velocity; it returns a
 * {@link Solution} containing the field-relative angle the turret should
 * point, the distance to use for shooter/hood lookups, and (if you
 * supplied the tables) the shooter RPM and hood angle.
 *
 * <h2>The SOTF method (virtual goal)</h2>
 *
 * <p>When the robot is moving, the game piece inherits the robot's
 * velocity. To hit a stationary target you aim at a <em>virtual goal</em>
 * shifted opposite to the robot's motion by one time-of-flight:
 *
 * <pre>
 *   virtualGoal = target − v_field · timeOfFlight
 * </pre>
 *
 * <p>Time-of-flight depends on the distance to the <em>virtual</em> goal,
 * not the real one, so the solve iterates a few times to converge
 * (typically 2–3 iterations is plenty). This is the same approach used
 * by top FRC teams for shoot-while-moving.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // One-time setup. Fill the tables from your own range testing.
 * AimingSolver solver = AimingSolver.builder()
 *     .target(FieldConstants.SPEAKER_CENTER)          // alliance-resolved Translation2d
 *     .shotTime(new InterpolatingTable()
 *         .add(1.5, 0.28).add(3.0, 0.42).add(5.0, 0.61))   // distance(m) -> flight time(s)
 *     .shooterRpm(new InterpolatingTable()
 *         .add(1.5, 2600).add(3.0, 3400).add(5.0, 4200))
 *     .hoodAngle(new InterpolatingTable()
 *         .add(1.5, 12).add(3.0, 28).add(5.0, 40))
 *     .iterations(3)
 *     .build();
 *
 * // Each loop:
 * AimingSolver.Solution s = solver.solve(drive.getPose(), drive.getFieldRelativeSpeeds());
 * turret.aimAtFieldAngle(s.turretFieldAngleDeg(), drive.getHeading().getDegrees());
 * shooter.spinUp(s.shooterRpm());
 * hood.goTo(s.hoodDegrees());
 * boolean readyToShoot = turret.atSetpoint() && shooter.atSpeed() && s.feasible();
 * }</pre>
 */
public final class AimingSolver {

    /**
     * The result of an aiming solve.
     *
     * @param turretFieldAngleDeg  field-relative bearing the turret bore should
     *                             point (degrees, CCW positive, 0 = +X field axis).
     *                             Convert to a turret command with
     *                             {@code turretFieldAngleDeg - robotHeadingDeg}.
     * @param distanceMeters       distance to the virtual goal — use this for
     *                             shooter / hood lookups.
     * @param shotTimeSeconds      estimated time of flight at that distance.
     * @param shooterRpm           looked-up shooter speed (0 if no table supplied).
     * @param hoodDegrees          looked-up hood angle (0 if no table supplied).
     * @param virtualGoal          the motion-compensated aim point in field
     *                             coordinates (the real target when stationary).
     * @param feasible             false when the solve produced a degenerate
     *                             result (robot essentially on top of the target).
     */
    public record Solution(
            double turretFieldAngleDeg,
            double distanceMeters,
            double shotTimeSeconds,
            double shooterRpm,
            double hoodDegrees,
            Translation2d virtualGoal,
            boolean feasible) {}

    private final InterpolatingTable shotTime;   // distance(m) -> flight time(s); may be null
    private final InterpolatingTable shooterRpm; // distance(m) -> rpm; may be null
    private final InterpolatingTable hoodAngle;  // distance(m) -> degrees; may be null
    private final int iterations;
    private final double minFeasibleDistance;

    private volatile Translation2d target;

    private AimingSolver(Builder b) {
        this.shotTime = b.shotTime;
        this.shooterRpm = b.shooterRpm;
        this.hoodAngle = b.hoodAngle;
        this.iterations = Math.max(1, b.iterations);
        this.minFeasibleDistance = b.minFeasibleDistance;
        this.target = b.target;
    }

    /**
     * Update the target at runtime — call this on alliance change so the
     * solver aims at your alliance's goal.
     */
    public void setTarget(Translation2d target) {
        this.target = target;
    }

    /** The current aim target in field coordinates. */
    public Translation2d getTarget() {
        return target;
    }

    /**
     * Static aim — ignores robot motion. Use for stationary shots or as a
     * fallback when you don't trust the velocity estimate.
     */
    public Solution solveStatic(Pose2d robotPose) {
        return solve(robotPose, new ChassisSpeeds());
    }

    /**
     * Full Shoot-On-The-Fly solve.
     *
     * @param robotPose            fused field pose (latency-compensated upstream
     *                             via {@link PoseHistory} if you have it).
     * @param fieldRelativeSpeeds  chassis speeds in the FIELD frame. If you only
     *                             have robot-relative speeds, rotate them first
     *                             with {@link ChassisSpeeds#fromRobotRelativeSpeeds}.
     */
    public Solution solve(Pose2d robotPose, ChassisSpeeds fieldRelativeSpeeds) {
        Translation2d robotXY = robotPose.getTranslation();
        Translation2d goal = target;

        // First pass uses the real target distance to seed the flight time.
        double dist = goal.getDistance(robotXY);
        double tof = lookup(shotTime, dist, 0.0);

        Translation2d virtualGoal = goal;
        for (int i = 0; i < iterations && tof > 0.0; i++) {
            Translation2d shift = new Translation2d(
                    fieldRelativeSpeeds.vxMetersPerSecond * tof,
                    fieldRelativeSpeeds.vyMetersPerSecond * tof);
            virtualGoal = goal.minus(shift);
            dist = virtualGoal.getDistance(robotXY);
            tof = lookup(shotTime, dist, 0.0);
        }

        Translation2d aim = virtualGoal.minus(robotXY);
        boolean feasible = aim.getNorm() >= minFeasibleDistance;

        // atan2 of a zero vector is 0; guard so a robot sitting on the target
        // doesn't spin the turret to a meaningless angle.
        double fieldAngleDeg = feasible
                ? Math.toDegrees(Math.atan2(aim.getY(), aim.getX()))
                : 0.0;

        return new Solution(
                fieldAngleDeg,
                dist,
                tof,
                lookup(shooterRpm, dist, 0.0),
                lookup(hoodAngle, dist, 0.0),
                virtualGoal,
                feasible);
    }

    private static double lookup(InterpolatingTable table, double key, double fallback) {
        if (table == null || table.size() == 0) return fallback;
        return table.get(key);
    }

    // ============================================================
    //                        BUILDER
    // ============================================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Translation2d target = new Translation2d();
        private InterpolatingTable shotTime;
        private InterpolatingTable shooterRpm;
        private InterpolatingTable hoodAngle;
        private int iterations = 3;
        private double minFeasibleDistance = 0.10; // metres

        /** Field-coordinate aim point. Resolve alliance before passing it in. */
        public Builder target(Translation2d target) {
            this.target = target;
            return this;
        }

        /**
         * Distance (metres) → time of flight (seconds). Required for SOTF —
         * without it {@link #solve} behaves like {@link #solveStatic}.
         */
        public Builder shotTime(InterpolatingTable shotTimeByDistance) {
            this.shotTime = shotTimeByDistance;
            return this;
        }

        /** Distance (metres) → shooter RPM. Optional. */
        public Builder shooterRpm(InterpolatingTable rpmByDistance) {
            this.shooterRpm = rpmByDistance;
            return this;
        }

        /** Distance (metres) → hood angle (degrees). Optional. */
        public Builder hoodAngle(InterpolatingTable hoodByDistance) {
            this.hoodAngle = hoodByDistance;
            return this;
        }

        /** Number of virtual-goal refinement passes. Default 3. */
        public Builder iterations(int n) {
            this.iterations = n;
            return this;
        }

        /**
         * Below this robot-to-goal distance the solve is treated as
         * infeasible (turret holds, {@code Solution.feasible()} is false).
         * Default 0.10 m.
         */
        public Builder minFeasibleDistance(double meters) {
            this.minFeasibleDistance = meters;
            return this;
        }

        public AimingSolver build() {
            return new AimingSolver(this);
        }
    }
}
