package frc.lib.catalyst.mechanisms;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.lib.catalyst.hardware.CatalystMotor;
import frc.lib.catalyst.hardware.MotorType;
import frc.lib.catalyst.io.FlywheelMechanismInputs;
import frc.lib.catalyst.util.HealthCheck;
import frc.lib.catalyst.util.HealthMonitor;
import frc.lib.catalyst.util.TunableGains;

/**
 * Generic flywheel mechanism. Use for shooters, accelerator wheels,
 * or any mechanism that requires precise velocity control.
 *
 * <p>Supports single or dual motor configurations. Dual motors can
 * spin at different speeds for spin control.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Single flywheel
 * FlywheelMechanism shooter = new FlywheelMechanism(
 *     FlywheelMechanism.Config.builder()
 *         .name("Shooter")
 *         .motor(20)
 *         .gearRatio(1.5)
 *         .pid(0.3, 0, 0)
 *         .feedforward(0.12, 0.11)
 *         .velocityTolerance(3.0)
 *         .currentLimit(60)
 *         .build());
 *
 * // Dual flywheel (top/bottom for spin)
 * FlywheelMechanism dualShooter = new FlywheelMechanism(
 *     FlywheelMechanism.Config.builder()
 *         .name("Shooter")
 *         .motor(20)
 *         .secondMotor(21)
 *         .gearRatio(1.5)
 *         .pid(0.3, 0, 0)
 *         .feedforward(0.12, 0.11)
 *         .velocityTolerance(3.0)
 *         .build());
 * }</pre>
 */
public class FlywheelMechanism extends CatalystMechanism {

    private final Config config;
    private final CatalystMotor primaryMotor;
    private final CatalystMotor secondaryMotor;

    // Simulation
    private FlywheelSim primarySim;
    private FlywheelSim secondarySim;

    // State
    private double primarySetpointRPS = 0;
    private double secondarySetpointRPS = 0;

    private final FlywheelMechanismInputs inputs = new FlywheelMechanismInputs();

    // Live-tunable Slot 0 gains. Flywheels are velocity-controlled, so no
    // Motion Magic — just PID + kS/kV/kA. Disabled via TunableNumber.disableTuning().
    private final TunableGains tunableGains;

    public FlywheelMechanism(Config config) {
        super(config.name);
        this.config = config;

        // Primary motor
        CatalystMotor.Builder primaryMotorBuilder = CatalystMotor.builder(config.primaryMotorCanId)
                .name(config.name + "Primary")
                .canBus(config.canBus)
                .inverted(config.primaryInverted)
                .brakeMode(false) // flywheels usually coast
                .currentLimit(config.currentLimit)
                .statorCurrentLimit(config.statorCurrentLimit)
                .gearRatio(config.gearRatio)
                .pid(config.kP, config.kI, config.kD)
                .feedforward(config.kS, config.kV, config.kA);

        if (config.primaryFollowerCanId > 0) {
            primaryMotorBuilder.withFollower(config.primaryFollowerCanId, config.primaryFollowerOppose);
        }

        for (int i = 0; i < config.primaryAdditionalFollowerCanIds.length; i++) {
            primaryMotorBuilder.withFollower(
                config.primaryAdditionalFollowerCanIds[i], 
                config.primaryAdditionalFollowerOppose[i]);
        }

        primaryMotor = primaryMotorBuilder.build();

        // Secondary motor (independent, not a follower)
        if (config.secondaryMotorCanId >= 0) {
            CatalystMotor.Builder secondaryMotorBuilder = CatalystMotor.builder(config.secondaryMotorCanId)
                    .name(config.name + "secondary")
                    .canBus(config.canBus)
                    .inverted(config.secondaryInverted)
                    .brakeMode(false) // flywheels usually coast
                    .currentLimit(config.currentLimit)
                    .statorCurrentLimit(config.statorCurrentLimit)
                    .gearRatio(config.gearRatio)
                    .pid(config.kP, config.kI, config.kD)
                    .feedforward(config.kS, config.kV, config.kA);

            if (config.secondaryFollowerCanId > 0) {
                secondaryMotorBuilder.withFollower(config.secondaryFollowerCanId, config.secondaryFollowerOppose);
            }

            for (int i = 0; i < config.secondaryAdditionalFollowerCanIds.length; i++) {
                secondaryMotorBuilder.withFollower(
                    config.secondaryAdditionalFollowerCanIds[i], 
                    config.secondaryAdditionalFollowerOppose[i]);
            }
            this.secondaryMotor = secondaryMotorBuilder.build();
        } else {
            this.secondaryMotor = null;
        }

        this.tunableGains = new TunableGains(
                config.name,
                config.kP, config.kI, config.kD,
                config.kS, config.kV, config.kA, 0);

        // Simulation
        if (RobotBase.isSimulation()) {
            DCMotor motorModel = config.motorType.getDCMotor(1);
            primarySim = new FlywheelSim(
                    LinearSystemId.createFlywheelSystem(motorModel, config.moi, config.gearRatio),
                    motorModel);
            if (secondaryMotor != null) {
                secondarySim = new FlywheelSim(
                        LinearSystemId.createFlywheelSystem(motorModel, config.moi, config.gearRatio),
                        motorModel);
            }
        }

        registerHealthChecks();
    }

    private void registerHealthChecks() {
        HealthMonitor.standardMotorChecks(name, primaryMotor, config.statorCurrentLimit, 70);
        if (secondaryMotor != null) {
            HealthMonitor.standardMotorChecks(name, "Sec", secondaryMotor, config.statorCurrentLimit, 70);
        }

        HealthCheck.builder(name, "NotSpinningUp")
                .severity(HealthCheck.Severity.WARN)
                .description("Commanded a non-zero setpoint but flywheel is not spinning up")
                .when(() -> primarySetpointRPS > 1.0
                        && Math.abs(primaryMotor.getVelocity()) < 1.0
                        && Math.abs(primaryMotor.getAppliedVoltage()) > 2.0)
                .detail(() -> String.format("setpoint %.1f rps, actual %.1f rps",
                        primarySetpointRPS, primaryMotor.getVelocity()))
                .debounce(1.0)
                .clearAfter(0.5)
                .register();
    }

    // --- Getters ---

    /** Get primary flywheel velocity in rotations per second. */
    public double getVelocity() {
        return primaryMotor.getVelocity();
    }

    /** Get secondary flywheel velocity in RPS (0 if no secondary). */
    public double getSecondaryVelocity() {
        return secondaryMotor != null ? secondaryMotor.getVelocity() : 0;
    }

    /** Get primary setpoint in RPS. */
    public double getSetpoint() {
        return primarySetpointRPS;
    }

    /** Get secondary setpoint in RPS. */
    public double getSecondarySetpoint() {
        return secondarySetpointRPS;
    }

    /** Check if primary flywheel is at target velocity within tolerance. */
    public boolean atSpeed() {
        if (primarySetpointRPS == 0) return false;
        boolean primaryOk = Math.abs(getVelocity() - primarySetpointRPS) < config.velocityTolerance;
        if (secondaryMotor == null) return primaryOk;
        boolean secondaryOk = Math.abs(getSecondaryVelocity() - secondarySetpointRPS) < config.velocityTolerance;
        return primaryOk && secondaryOk;
    }

    /** Trigger that fires when flywheel is at target speed. */
    public Trigger atSpeedTrigger() {
        return new Trigger(this::atSpeed);
    }

    // --- Command Factories ---

    /** Command to spin up to a target velocity (rotations per second). */
    public Command spinUp(double velocityRPS) {
        return run(() -> {
            primarySetpointRPS = velocityRPS;
            secondarySetpointRPS = velocityRPS;
            primaryMotor.setVelocity(velocityRPS);
            if (secondaryMotor != null) {
                secondaryMotor.setVelocity(velocityRPS);
            }
            setState("SpinUp " + String.format("%.0f", velocityRPS) + " RPS");
        }).finallyDo(() -> {
            primaryMotor.stop();
            if (secondaryMotor != null) secondaryMotor.stop();
            primarySetpointRPS = 0;
            secondarySetpointRPS = 0;
            setState("Idle");
        }).withName(name + ".SpinUp(" + String.format("%.0f", velocityRPS) + ")");
    }

    /**
     * Command to spin up dual flywheels at different speeds.
     * Useful for spin control (backspin/topspin).
     */
    public Command spinUp(double primaryRPS, double secondaryRPS) {
        if (secondaryMotor == null) {
            return spinUp(primaryRPS);
        }
        return run(() -> {
            primarySetpointRPS = primaryRPS;
            secondarySetpointRPS = secondaryRPS;
            primaryMotor.setVelocity(primaryRPS);
            secondaryMotor.setVelocity(secondaryRPS);
            setState("SpinUp " + String.format("%.0f/%.0f", primaryRPS, secondaryRPS) + " RPS");
        }).finallyDo(() -> {
            primaryMotor.stop();
            secondaryMotor.stop();
            primarySetpointRPS = 0;
            secondarySetpointRPS = 0;
            setState("Idle");
        }).withName(name + ".SpinUp(" + String.format("%.0f/%.0f", primaryRPS, secondaryRPS) + ")");
    }

    /** Command to spin up and wait until at speed. */
    public Command spinUpAndWait(double velocityRPS) {
        return spinUp(velocityRPS).until(this::atSpeed)
                .withName(name + ".SpinUpAndWait(" + String.format("%.0f", velocityRPS) + ")");
    }

    /** Command to run at a set voltage (open loop). */
    public Command runVoltage(double volts) {
        return run(() -> {
            primaryMotor.setVoltage(volts);
            if (secondaryMotor != null) secondaryMotor.setVoltage(volts);
            setState("Voltage " + String.format("%.1fV", volts));
        }).finallyDo(() -> {
            primaryMotor.stop();
            if (secondaryMotor != null) secondaryMotor.stop();
            setState("Idle");
        }).withName(name + ".RunVoltage(" + String.format("%.1f", volts) + ")");
    }

    // --- Internals ---

    @Override
    protected void stop() {
        primaryMotor.stop();
        if (secondaryMotor != null) secondaryMotor.stop();
        primarySetpointRPS = 0;
        secondarySetpointRPS = 0;
        setState("Stopped");
    }

    @Override
    protected void updateTelemetry() {
        primaryMotor.updateTelemetry();
        if (secondaryMotor != null) secondaryMotor.updateTelemetry();

        if (secondaryMotor != null) {
            tunableGains.checkAndApply(primaryMotor, secondaryMotor);
        } else {
            tunableGains.checkAndApply(primaryMotor);
        }

        inputs.primaryVelocityRPS = getVelocity();
        inputs.primaryStatorCurrentAmps = primaryMotor.getStatorCurrent();
        inputs.primaryAppliedVolts = primaryMotor.getAppliedVoltage();
        inputs.primaryTemperatureC = primaryMotor.getTemperature();
        inputs.primarySetpointRPS = primarySetpointRPS;
        if (secondaryMotor != null) {
            inputs.secondaryVelocityRPS = getSecondaryVelocity();
            inputs.secondaryStatorCurrentAmps = secondaryMotor.getStatorCurrent();
            inputs.secondaryAppliedVolts = secondaryMotor.getAppliedVoltage();
            inputs.secondaryTemperatureC = secondaryMotor.getTemperature();
            inputs.secondarySetpointRPS = secondarySetpointRPS;
        } else {
            inputs.secondaryVelocityRPS = 0.0;
            inputs.secondaryStatorCurrentAmps = 0.0;
            inputs.secondaryAppliedVolts = 0.0;
            inputs.secondaryTemperatureC = 0.0;
            inputs.secondarySetpointRPS = 0.0;
        }
        inputs.atSpeed = atSpeed();
        processInputs(inputs);

        // Per-key telemetry for v0.2 dashboard compatibility.
        log("VelocityRPS", inputs.primaryVelocityRPS);
        log("SetpointRPS", inputs.primarySetpointRPS);
        log("AtSpeed", inputs.atSpeed);
        if (secondaryMotor != null) {
            log("SecondaryVelocityRPS", inputs.secondaryVelocityRPS);
            log("SecondarySetpointRPS", inputs.secondarySetpointRPS);
        }

        HealthMonitor.getInstance().update();
    }

    @Override
    public void simulationPeriodic() {
        if (primarySim != null) {
            var simState = primaryMotor.getTalonFX().getSimState();
            primarySim.setInput(simState.getMotorVoltage());
            primarySim.update(0.02);
            simState.setRotorVelocity(primarySim.getAngularVelocityRPM() / 60.0 * config.gearRatio);
        }
        if (secondarySim != null && secondaryMotor != null) {
            var simState = secondaryMotor.getTalonFX().getSimState();
            secondarySim.setInput(simState.getMotorVoltage());
            secondarySim.update(0.02);
            simState.setRotorVelocity(secondarySim.getAngularVelocityRPM() / 60.0 * config.gearRatio);
        }
    }

    public CatalystMotor getPrimaryMotor() { return primaryMotor; }
    public CatalystMotor getSecondaryMotor() { return secondaryMotor; }

    // ===========================================
    //                  CONFIG
    // ===========================================

    public static class Config {
        final String name;
        final int primaryMotorCanId;
        final int primaryFollowerCanId;
        final boolean primaryFollowerOppose;
        final int[] primaryAdditionalFollowerCanIds;
        final boolean[] primaryAdditionalFollowerOppose;
        final int secondaryMotorCanId;
        final int secondaryFollowerCanId;
        final boolean secondaryFollowerOppose;
        final int[] secondaryAdditionalFollowerCanIds;
        final boolean[] secondaryAdditionalFollowerOppose;
        final String canBus;
        final boolean primaryInverted;
        final boolean secondaryInverted;
        final MotorType motorType;
        final double gearRatio;
        final double moi;
        final double currentLimit;
        final double statorCurrentLimit;
        final double velocityTolerance;
        final double kP, kI, kD;
        final double kS, kV, kA;

        private Config(Builder b) {
            this.name = b.name;
            this.primaryMotorCanId = b.primaryMotorCanId;            
            this.primaryFollowerCanId = b.primaryFollowerCanId;
            this.primaryFollowerOppose = b.primaryFollowerOppose;
            this.primaryAdditionalFollowerCanIds = b.primaryAdditionalFollowerCanIds;
            this.primaryAdditionalFollowerOppose = b.primaryAdditionalFollowerOppose;
            this.secondaryMotorCanId = b.secondaryMotorCanId;
            this.secondaryFollowerCanId = b.secondaryFollowerCanId;
            this.secondaryFollowerOppose = b.secondaryFollowerOppose;
            this.secondaryAdditionalFollowerCanIds = b.secondaryAdditionalFollowerCanIds;
            this.secondaryAdditionalFollowerOppose = b.secondaryAdditionalFollowerOppose;
            this.canBus = b.canBus;
            this.primaryInverted = b.primaryInverted;
            this.secondaryInverted = b.secondaryInverted;
            this.motorType = b.motorType;
            this.gearRatio = b.gearRatio;
            this.moi = b.moi;
            this.currentLimit = b.currentLimit;
            this.statorCurrentLimit = b.statorCurrentLimit;
            this.velocityTolerance = b.velocityTolerance;
            this.kP = b.kP; this.kI = b.kI; this.kD = b.kD;
            this.kS = b.kS; this.kV = b.kV; this.kA = b.kA;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name = "FlywheelMechanism";
            private int primaryMotorCanId = 0;
            public boolean primaryFollowerOppose;
            public int primaryFollowerCanId;
            public boolean[] primaryAdditionalFollowerOppose;
            public int[] primaryAdditionalFollowerCanIds;
            private int secondaryMotorCanId = -1;
            public boolean[] secondaryAdditionalFollowerOppose;
            public int[] secondaryAdditionalFollowerCanIds;
            public boolean secondaryFollowerOppose;
            public int secondaryFollowerCanId;
            private String canBus = "";
            private boolean primaryInverted = false;
            private boolean secondaryInverted = false;
            private MotorType motorType = MotorType.KRAKEN_X60;
            private double gearRatio = 1.0;
            private double moi = 0.01; // kg*m^2
            private double currentLimit = 60;
            private double statorCurrentLimit = 120;
            private double velocityTolerance = 3.0; // RPS
            private double kP = 0, kI = 0, kD = 0;
            private double kS = 0, kV = 0, kA = 0;

            public Builder name(String name) { this.name = name; return this; }
            public Builder motor(int canId) { this.primaryMotorCanId = canId; return this; }
            
            public Builder primaryFollower(int canId, boolean oppose) {
                this.primaryFollowerCanId = canId;
                this.primaryFollowerOppose = oppose;
                return this;
            }

            
            /**
             * Add an additional follower motor beyond the primary follower.
             * <p>This is the fix for the long-standing limitation where Catalyst only
             * wired up one follower per mechanism. Call this method once per follower —
             * for a four-Kraken elevator with two followers on each side of the leader,
             * call this three times.
             *
             * @param canId  follower CAN ID
             * @param oppose if true, the follower runs opposed to the leader (mirrored)
             */
            public Builder primaryAdditionalFollower(int canId, boolean oppose) {
                int[] ids = new int[this.primaryAdditionalFollowerCanIds.length + 1];
                boolean[] opp = new boolean[this.primaryAdditionalFollowerOppose.length + 1];
                System.arraycopy(this.primaryAdditionalFollowerCanIds, 0, ids, 0, this.primaryAdditionalFollowerCanIds.length);
                System.arraycopy(this.primaryAdditionalFollowerOppose, 0, opp, 0, this.primaryAdditionalFollowerOppose.length);
                ids[ids.length - 1] = canId;
                opp[opp.length - 1] = oppose;
                this.primaryAdditionalFollowerCanIds = ids;
                this.primaryAdditionalFollowerOppose = opp;
                return this;
            }

            public Builder secondaryFollower(int canId, boolean oppose) {
                this.secondaryFollowerCanId = canId;
                this.secondaryFollowerOppose = oppose;
                return this;
            }

            
            /**
             * Add an additional follower motor beyond the primary follower.
             * <p>This is the fix for the long-standing limitation where Catalyst only
             * wired up one follower per mechanism. Call this method once per follower —
             * for a four-Kraken elevator with two followers on each side of the leader,
             * call this three times.
             *
             * @param canId  follower CAN ID
             * @param oppose if true, the follower runs opposed to the leader (mirrored)
             */
            public Builder secondaryAdditionalFollower(int canId, boolean oppose) {
                int[] ids = new int[this.secondaryAdditionalFollowerCanIds.length + 1];
                boolean[] opp = new boolean[this.secondaryAdditionalFollowerOppose.length + 1];
                System.arraycopy(this.secondaryAdditionalFollowerCanIds, 0, ids, 0, this.secondaryAdditionalFollowerCanIds.length);
                System.arraycopy(this.secondaryAdditionalFollowerOppose, 0, opp, 0, this.secondaryAdditionalFollowerOppose.length);
                ids[ids.length - 1] = canId;
                opp[opp.length - 1] = oppose;
                this.secondaryAdditionalFollowerCanIds = ids;
                this.secondaryAdditionalFollowerOppose = opp;
                return this;
            }


            /** Add a second motor for dual-flywheel setups (independent, NOT a follower). */
            public Builder secondMotor(int canId) { this.secondaryMotorCanId = canId; return this; }

            public Builder canBus(String canBus) { this.canBus = canBus; return this; }
            public Builder primaryInverted(boolean inv) { this.primaryInverted = inv; return this; }
            public Builder secondaryInverted(boolean inv) { this.secondaryInverted = inv; return this; }
            /** Set the motor type for accurate simulation (default: Kraken X60). */
            public Builder motorType(MotorType type) { this.motorType = type; return this; }
            public Builder gearRatio(double ratio) { this.gearRatio = ratio; return this; }

            /** Moment of inertia in kg*m^2 (for simulation). */
            public Builder moi(double kgm2) { this.moi = kgm2; return this; }

            public Builder currentLimit(double amps) { this.currentLimit = amps; return this; }
            public Builder statorCurrentLimit(double amps) { this.statorCurrentLimit = amps; return this; }

            /** How close to target speed (in RPS) counts as "at speed". */
            public Builder velocityTolerance(double rps) { this.velocityTolerance = rps; return this; }

            public Builder pid(double kP, double kI, double kD) {
                this.kP = kP; this.kI = kI; this.kD = kD; return this;
            }

            public Builder feedforward(double kS, double kV) {
                this.kS = kS; this.kV = kV; return this;
            }

            public Builder feedforward(double kS, double kV, double kA) {
                this.kS = kS; this.kV = kV; this.kA = kA; return this;
            }

            public Config build() {
                if (primaryMotorCanId == 0) {
                    throw new IllegalStateException("Motor CAN ID must be set");
                }
                return new Config(this);
            }
        }
    }
}
