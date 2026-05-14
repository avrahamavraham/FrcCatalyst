package frc.lib.catalyst.util;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A single, debounced fault condition that {@link HealthMonitor} evaluates once per loop.
 *
 * <p>A check has a {@link BooleanSupplier} predicate that returns {@code true}
 * while the condition is "unhealthy" (e.g., motor over 80&nbsp;A). The check
 * only fires once the predicate has been true continuously for
 * {@link Builder#debounce(double)} seconds — that defeats single-frame spikes.
 * Symmetrically, once firing, the check clears only after the predicate has
 * been false continuously for {@link Builder#clearAfter(double)} seconds.
 *
 * <p>Built-in mechanism wiring uses this for over-current, over-temperature,
 * stall, and disconnect checks. Teams can add their own:
 *
 * <pre>{@code
 * HealthCheck.builder(this, "PressureLow")
 *     .severity(HealthCheck.Severity.WARN)
 *     .description("Air pressure below operating threshold")
 *     .when(() -> pressure.getPSI() < 80)
 *     .detail(() -> String.format("%.0f psi", pressure.getPSI()))
 *     .debounce(0.5)
 *     .clearAfter(2.0)
 *     .register();
 * }</pre>
 */
public final class HealthCheck {

    public enum Severity { INFO, WARN, ERROR }

    private final String subsystem;
    private final String id;
    private final String description;
    private final Severity severity;
    private final BooleanSupplier predicate;
    private final Supplier<String> detail;
    private final double debounceSec;
    private final double clearAfterSec;
    private final Runnable onFire;
    private final Runnable onClear;

    private double firstTrueTs = -1;
    private double lastFalseTs = -1;
    private boolean firing = false;
    private double firedAtTs = -1;
    private String lastDetail = "";

    private HealthCheck(Builder b) {
        this.subsystem = b.subsystem;
        this.id = b.id;
        this.description = b.description != null ? b.description : b.id;
        this.severity = b.severity;
        this.predicate = b.predicate;
        this.detail = b.detail;
        this.debounceSec = Math.max(0, b.debounceSec);
        this.clearAfterSec = Math.max(0, b.clearAfterSec);
        this.onFire = b.onFire;
        this.onClear = b.onClear;
    }

    public String subsystem() { return subsystem; }
    public String id() { return id; }
    public String description() { return description; }
    public Severity severity() { return severity; }
    public boolean isFiring() { return firing; }
    public double firedAt() { return firedAtTs; }
    public String currentDetail() { return lastDetail; }

    /**
     * Tick this check. Called by {@link HealthMonitor} each loop. Returns one of
     * {@link Transition#NONE}, {@link Transition#FIRED}, {@link Transition#CLEARED}
     * so the caller can update bookkeeping only on edges.
     */
    enum Transition { NONE, FIRED, CLEARED }

    Transition evaluate(double now) {
        boolean cond;
        try {
            cond = predicate.getAsBoolean();
        } catch (Throwable t) {
            // A buggy predicate should not take down the whole HealthMonitor loop.
            // Treat as "not firing" and continue. Catalyst doesn't try to be cute
            // about logging this — if a user's lambda is broken, their normal
            // logs will surface it once they touch the mechanism.
            cond = false;
        }
        if (detail != null) {
            try { lastDetail = detail.get(); }
            catch (Throwable t) { lastDetail = ""; }
        }

        if (cond) {
            if (firstTrueTs < 0) firstTrueTs = now;
            lastFalseTs = -1;
            if (!firing && (now - firstTrueTs) >= debounceSec) {
                firing = true;
                firedAtTs = now;
                if (onFire != null) safeRun(onFire);
                return Transition.FIRED;
            }
        } else {
            firstTrueTs = -1;
            if (firing) {
                if (lastFalseTs < 0) lastFalseTs = now;
                if ((now - lastFalseTs) >= clearAfterSec) {
                    firing = false;
                    if (onClear != null) safeRun(onClear);
                    return Transition.CLEARED;
                }
            }
        }
        return Transition.NONE;
    }

    private static void safeRun(Runnable r) {
        try { r.run(); } catch (Throwable ignored) {}
    }

    // -------------------------------------------------------------
    //                          BUILDER
    // -------------------------------------------------------------

    public static Builder builder(String subsystem, String id) {
        return new Builder(subsystem, id);
    }

    public static final class Builder {
        private final String subsystem;
        private final String id;
        private String description;
        private Severity severity = Severity.WARN;
        private BooleanSupplier predicate = () -> false;
        private Supplier<String> detail;
        private double debounceSec = 0.25;
        private double clearAfterSec = 0.5;
        private Runnable onFire;
        private Runnable onClear;

        private Builder(String subsystem, String id) {
            this.subsystem = subsystem;
            this.id = id;
        }

        public Builder severity(Severity s) { this.severity = s; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder when(BooleanSupplier p) { this.predicate = p; return this; }
        public Builder detail(Supplier<String> d) { this.detail = d; return this; }
        public Builder debounce(double seconds) { this.debounceSec = seconds; return this; }
        public Builder clearAfter(double seconds) { this.clearAfterSec = seconds; return this; }
        public Builder onFire(Runnable r) { this.onFire = r; return this; }
        public Builder onClear(Runnable r) { this.onClear = r; return this; }

        /** Build and register with the global {@link HealthMonitor}. */
        public HealthCheck register() {
            HealthCheck c = new HealthCheck(this);
            HealthMonitor.getInstance().register(c);
            return c;
        }

        /** Build the check without registering. Useful for tests. */
        public HealthCheck build() {
            return new HealthCheck(this);
        }
    }
}
