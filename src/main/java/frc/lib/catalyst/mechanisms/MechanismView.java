package frc.lib.catalyst.mechanisms;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A uniform, mechanism-agnostic snapshot used by the generic simulation
 * dashboard ({@code frc.lib.catalyst.sim.SimDashboard}). Every
 * {@link CatalystMechanism} produces one via {@link CatalystMechanism#describe()}
 * so the dashboard can render and drive <em>any</em> mechanism — including a
 * team's own subclass — without knowing its concrete type.
 *
 * <p>{@link Double#NaN} is the universal "not applicable" sentinel (a flywheel
 * has no {@code min}/{@code max}; a claw has no numeric {@code value}). The
 * dashboard picks a widget from {@link #kind()}, not from which fields are NaN.
 *
 * @param name        display name ({@link CatalystMechanism#getMechanismName()})
 * @param kind        one of: linear, rotational, roller, flywheel, turret,
 *                    claw, diffwrist, winch, pneumatic, generic
 * @param value       primary numeric observable, or NaN
 * @param unit        unit of {@code value}: m, deg, rps, frac, psi, ""
 * @param setpoint    commanded target, or NaN
 * @param min         range minimum, or NaN
 * @param max         range maximum, or NaN
 * @param velocity    rate of {@code value}, or NaN
 * @param currentAmps stator current, or NaN
 * @param extra       kind-specific extras (state enums, hasPiece, roll axis…)
 */
public record MechanismView(
        String name,
        String kind,
        double value,
        String unit,
        double setpoint,
        double min,
        double max,
        double velocity,
        double currentAmps,
        Map<String, String> extra) {

    /**
     * Null-coalesce {@code extra} so a hand-built view (bypassing the
     * {@link Builder}) can never feed a null map to the dashboard. Keeps the
     * "any user mechanism is safe to render" guarantee at the source.
     */
    public MechanismView {
        extra = (extra == null) ? Map.of() : extra;
    }

    /** Start building a view for the given name + kind. */
    public static Builder of(String name, String kind) {
        return new Builder(name, kind);
    }

    /** Fluent builder so each mechanism's {@code describe()} stays readable. */
    public static final class Builder {
        private final String name, kind;
        private double value = Double.NaN, setpoint = Double.NaN;
        private double min = Double.NaN, max = Double.NaN;
        private double velocity = Double.NaN, current = Double.NaN;
        private String unit = "";
        private final Map<String, String> extra = new LinkedHashMap<>();

        private Builder(String name, String kind) {
            this.name = name;
            this.kind = kind;
        }

        public Builder value(double v, String unit) { this.value = v; this.unit = unit; return this; }
        public Builder setpoint(double v) { this.setpoint = v; return this; }
        public Builder range(double min, double max) { this.min = min; this.max = max; return this; }
        public Builder velocity(double v) { this.velocity = v; return this; }
        public Builder current(double a) { this.current = a; return this; }
        public Builder extra(String key, String val) { this.extra.put(key, val); return this; }
        public Builder extra(String key, boolean val) { this.extra.put(key, Boolean.toString(val)); return this; }
        public Builder extra(String key, double val) { this.extra.put(key, fmt(val)); return this; }

        private static String fmt(double v) {
            return (Double.isNaN(v) || Double.isInfinite(v)) ? "0" : String.format("%.2f", v);
        }

        public MechanismView build() {
            return new MechanismView(name, kind, value, unit, setpoint, min, max, velocity, current, extra);
        }
    }
}
