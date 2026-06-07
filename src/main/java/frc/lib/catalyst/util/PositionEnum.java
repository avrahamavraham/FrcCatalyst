package frc.lib.catalyst.util;

/**
 * Implemented by enums that name a mechanism setpoint, so a whole enum
 * worth of positions can be registered with one
 * {@code .addPositionsFromEnum(MyPositions.class)} call on a Linear or
 * Rotational mechanism config.
 *
 * <p>Each enum constant's {@link Enum#name()} becomes the position name,
 * and {@link #getTarget()} returns the position value in the mechanism's
 * native unit (meters for {@code LinearMechanism}, degrees for
 * {@code RotationalMechanism}).
 *
 * <pre>{@code
 * public enum ArmPos implements PositionEnum {
 *     STOW(0), HANDOFF(45), L1(60), L2(85), L3(95);
 *
 *     private final double degrees;
 *     ArmPos(double degrees) { this.degrees = degrees; }
 *     public double getTarget() { return degrees; }
 * }
 *
 * RotationalMechanism arm = new RotationalMechanism(
 *     RotationalMechanism.Config.builder()
 *         .name("Arm")
 *         .motor(20)
 *         .addPositionsFromEnum(ArmPos.class)   // registers all five
 *         .build());
 *
 * arm.goTo(ArmPos.L2);   // type-safe — no risk of misspelling the name
 * }</pre>
 */
@FunctionalInterface
public interface PositionEnum {
    /** Position value in the mechanism's native unit. */
    double getTarget();
}
