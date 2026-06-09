package frc.lib.catalyst.io;

import frc.lib.catalyst.logging.CatalystInputs;
import frc.lib.catalyst.logging.LogTable;

/**
 * Per-loop input snapshot for a {@code TurretMechanism}.
 *
 * <p>Captures the turret's mechanism angle (robot-relative), the
 * commanded setpoint, the field-relative aim bearing it's tracking, and
 * standard motor health. Published under {@code /Catalyst/&lt;name&gt;/...}.
 */
public class TurretMechanismInputs implements CatalystInputs {

    /** Turret angle relative to the chassis, in degrees. 0 = bore points robot-forward. */
    public double angleDegrees = 0.0;

    /** Turret angular velocity in degrees per second. */
    public double angularVelocityDPS = 0.0;

    /** Last commanded turret setpoint (robot-relative degrees). */
    public double setpointDegrees = 0.0;

    /** Field-relative bearing the turret is aiming at, in degrees (NaN when not aiming). */
    public double aimFieldAngleDegrees = 0.0;

    /** True when within the configured angular tolerance of the setpoint. */
    public boolean atSetpoint = false;

    /** True when the last aim request could not be reached the short way and the turret unwrapped. */
    public boolean unwrapping = false;

    /** Stator current on the leader motor in amps. */
    public double statorCurrentAmps = 0.0;

    /** Supply current on the leader motor in amps. */
    public double supplyCurrentAmps = 0.0;

    /** Applied output voltage on the leader. */
    public double appliedVolts = 0.0;

    /** Leader motor temperature in degrees Celsius. */
    public double temperatureC = 0.0;

    /** True when the encoder has been seeded (hard stop or manual zero). */
    public boolean hasBeenZeroed = false;

    @Override
    public void toLog(LogTable table) {
        table.put("AngleDegrees", angleDegrees);
        table.put("AngularVelocityDPS", angularVelocityDPS);
        table.put("SetpointDegrees", setpointDegrees);
        table.put("AimFieldAngleDegrees", aimFieldAngleDegrees);
        table.put("AtSetpoint", atSetpoint);
        table.put("Unwrapping", unwrapping);
        table.put("StatorCurrentAmps", statorCurrentAmps);
        table.put("SupplyCurrentAmps", supplyCurrentAmps);
        table.put("AppliedVolts", appliedVolts);
        table.put("TemperatureC", temperatureC);
        table.put("HasBeenZeroed", hasBeenZeroed);
    }

    @Override
    public void fromLog(LogTable table) {
        angleDegrees = table.get("AngleDegrees", angleDegrees);
        angularVelocityDPS = table.get("AngularVelocityDPS", angularVelocityDPS);
        setpointDegrees = table.get("SetpointDegrees", setpointDegrees);
        aimFieldAngleDegrees = table.get("AimFieldAngleDegrees", aimFieldAngleDegrees);
        atSetpoint = table.get("AtSetpoint", atSetpoint);
        unwrapping = table.get("Unwrapping", unwrapping);
        statorCurrentAmps = table.get("StatorCurrentAmps", statorCurrentAmps);
        supplyCurrentAmps = table.get("SupplyCurrentAmps", supplyCurrentAmps);
        appliedVolts = table.get("AppliedVolts", appliedVolts);
        temperatureC = table.get("TemperatureC", temperatureC);
        hasBeenZeroed = table.get("HasBeenZeroed", hasBeenZeroed);
    }
}
