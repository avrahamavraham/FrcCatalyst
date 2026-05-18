---
layout: default
title: Home
nav_order: 1
permalink: /
---

# FrcCatalyst
{: .fs-9 }

Pre-built, configurable mechanism building blocks for FRC robots using CTRE Phoenix 6 and WPILib 2026.
{: .fs-6 .fw-300 }

[Get Started](getting-started/installation){: .btn .btn-primary .fs-5 .mb-4 .mb-md-0 .mr-2 }
[Open the Tools](tools/){: .btn .fs-5 .mb-4 .mb-md-0 .mr-2 }
[View on GitHub](https://github.com/TomAs-1226/FrcCatalyst){: .btn .fs-5 .mb-4 .mb-md-0 }

---

## Try the tools (no install required)

Five single-file browser tools hosted right on this site. Open one and use it
immediately — no clone, no `npm install`, no build step.

<style>
.hero-tools {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin: 16px 0 30px;
}
.hero-tool {
  display: block;
  padding: 14px 16px;
  border: 1px solid var(--border-color, #2a3050);
  border-radius: 10px;
  text-decoration: none !important;
  color: inherit;
  background: var(--code-background-color, rgba(255,255,255,0.03));
  transition: transform 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease;
}
.hero-tool:hover {
  transform: translateY(-2px);
  border-color: #e94560;
  box-shadow: 0 6px 14px rgba(0,0,0,0.25);
}
.hero-tool .icon { font-size: 22px; line-height: 1; margin-bottom: 6px; display: block; }
.hero-tool .name { font-weight: 700; color: #e94560; display: block; margin-bottom: 4px; }
.hero-tool .desc { font-size: 12px; line-height: 1.45; color: var(--body-text-color, #666); }
</style>

<div class="hero-tools">
  <a class="hero-tool" href="tools/builder/"><span class="icon">🛠️</span><span class="name">Catalyst Builder</span><span class="desc">Form → ready-to-paste Java config for every mechanism.</span></a>
  <a class="hero-tool" href="tools/tuner/"><span class="icon">🎚</span><span class="name">Catalyst Tuner</span><span class="desc">Live NT4 PID + Motion Magic tuner.</span></a>
  <a class="hero-tool" href="tools/health/"><span class="icon">🩺</span><span class="name">Health Dashboard</span><span class="desc">Live <code>/Catalyst/Health/</code> viewer with filters.</span></a>
  <a class="hero-tool" href="tools/motion/"><span class="icon">📈</span><span class="name">Motion Profile Visualizer</span><span class="desc">Sketch a Motion Magic profile before committing.</span></a>
  <a class="hero-tool" href="tools/motors/"><span class="icon">⚡</span><span class="name">MotorType Browser</span><span class="desc">Every motor preset + gear-ratio calculator.</span></a>
</div>

[Full tool docs →](tools/){: .btn .btn-outline .fs-4 .mb-4 }

---

<p>
  <img src="https://img.shields.io/badge/WPILib-2026.2.1-green?style=flat-square" alt="WPILib"/>
  <img src="https://img.shields.io/badge/Phoenix%206-26.1.1-orange?style=flat-square" alt="Phoenix 6"/>
  <img src="https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk" alt="Java 17"/>
  <img src="https://img.shields.io/badge/PathPlanner-2026.1.2-purple?style=flat-square" alt="PathPlanner"/>
  <img src="https://img.shields.io/badge/PhotonVision-v2026.3.1-yellow?style=flat-square" alt="PhotonVision"/>
</p>

## Why FrcCatalyst?

Writing robot code from scratch every season means re-implementing the same elevator, arm, intake, and swerve patterns. FrcCatalyst gives you battle-tested implementations with one builder call:

```java
LinearMechanism elevator = new LinearMechanism(
    LinearMechanism.Config.builder()
        .name("Elevator").motor(13).follower(14, true)
        .motorType(MotorType.KRAKEN_X60)
        .gearRatio(10.0).drumRadius(0.0254).mass(5.0)
        .pid(50, 0, 0.5).gravityGain(0.35)
        .motionMagic(2.0, 4.0, 20.0)
        .position("STOW", 0.0).position("HIGH", 1.1)
        .build()
);
```

You get Motion Magic control, gravity compensation, simulation, telemetry, safety limits, and command factories — all for free.

| Feature | Raw WPILib/Phoenix | FrcCatalyst |
|---------|-------------------|-------------|
| Elevator with gravity FF | ~150 lines | **8 lines** |
| Swerve + PathPlanner + Vision | ~400 lines | **15 lines** |
| Mechanism with sim + telemetry | Build it yourself | **Built-in** |
| Safe temperature cutoffs | Manual | **Automatic** |
| Limit switch auto-zeroing | Manual wiring | **One builder call** |

---

## What's Included

### Mechanisms

Pre-built, configurable mechanism types that cover virtually every FRC subsystem.

| Mechanism | Use Case | Key Features |
|-----------|----------|--------------|
| **LinearMechanism** | Elevators, slides | Position control, gravity FF, limit switches, multi-follower |
| **RotationalMechanism** | Arms, wrists, turrets | Cosine gravity, hard stops, Motion Magic, multi-follower |
| **FlywheelMechanism** | Shooters | Dual motor + per-shaft followers, velocity PID, at-speed trigger |
| **RollerMechanism** | Intakes, conveyors | Stall detection, beam break, auto-stop |
| **WinchMechanism** | Climbers | Extend/retract limits, position tracking, dual-arm support |
| **ClawMechanism** | Motor-driven grippers | Stall detection, beam break, multi-follower, passive-hold |
| **DifferentialWristMechanism** | Diffy wrists (pitch + roll) | **Phoenix-6 native differential control**, separate Slot 0 / Slot 1 tuning |
| **PneumaticMechanism** | Solenoids / pistons | Double or single solenoid, optional pressure-gating, pulse / toggle commands |
| **SuperstructureCoordinator** | Multi-mechanism | State machine with safe transitions |

### Subsystems

Complex subsystem wrappers that integrate multiple components.

- **SwerveSubsystem** — CTRE Tuner X wrapper with heading lock, point-at-target, skew correction, advanced drive, PathPlanner
- **VisionSubsystem** — Multi-camera Kalman filter with innovation tracking, high-speed rejection, heading divergence filtering
- **LEDSubsystem** — 14 addressable LED patterns including fire, gradient, larson scanner, alignment indicator

### Advanced Features (New)

Advanced control and estimation features:

| Feature | Description |
|---------|-------------|
| **StateSpaceController** | LQR + Kalman filter for optimal mechanism control |
| **MotionConstraintCalculator** | Physics-based max velocity/acceleration from motor specs |
| **SignalProcessor** | EMA, median, low-pass, composite sensor filters |
| **PoseHistory** | Temporal pose tracking with interpolation |
| **DynamicAutoBuilder** | Runtime path generation with PathPlanner |
| **TunableNumber** | Dashboard-editable constants for live PID tuning |
| **AutoSelector** | PathPlanner auto chooser with safe fallbacks |
| **GamePieceTracker** | Multi-stage game piece state machine with Triggers |
| **Skew Correction** | Pose exponential discretization for swerve |
| **Collision Zones** | Prevent physical mechanism collisions |

### Health & Safety (v0.3.3+)

| Feature | Description |
|---------|-------------|
| **HealthCheck / HealthMonitor** | Debounced INFO / WARN / ERROR check layer — every motor mechanism gets OverCurrent / HighTemp / OverTemp checks for free |
| **HealthHistory** | Ring buffer of recent fire/clear events, queryable + auto-published for the dashboard timeline |
| **RobotSafety** | Opt-in cross-mechanism watchdog with `trippedTrigger()` for one-line Command bindings |
| **MotorType** | Open class with NEO / Vortex / 550 / Minion / Kraken (incl. X44 and corrected FOC specs) presets — and a public constructor for your own |

### Browser Tools (live on this site)

- 🛠️ **[Catalyst Builder](tools/builder/)** — form-driven Java config generator with localStorage persistence, `.java` download, full-subsystem-class mode, and snippet import
- 🎚 **[Catalyst Tuner](tools/tuner/)** — NT4 PID + Motion Magic tuner with gains-snapshot JSON export
- 🩺 **[Health Dashboard](tools/health/)** — live `/Catalyst/Health/` viewer with severity filters, search, and report download

### Every Mechanism Includes

- Builder-pattern configuration with sensible defaults
- Motion Magic position control (TalonFX) or ProfiledPID (roboRIO)
- Named position presets (`goTo("STOW")`)
- Built-in simulation with accurate motor models
- Automatic NetworkTables telemetry
- Temperature cutoff, limit switch safety, and **HealthCheck-based fault monitoring**
- Pre-built command factories
- **Multi-follower support** (one builder call per follower)

---

## Documentation

| Section | Description |
|---------|-------------|
| [Installation](getting-started/installation) | Add FrcCatalyst to your project |
| [Quick Start](getting-started/quickstart) | Build your first mechanism in 5 minutes |
| [Mechanisms](mechanisms/) | Linear, Rotational, Flywheel, Roller, Winch, Claw, **Differential Wrist (native CTRE)**, **Pneumatic** |
| [Subsystems](subsystems/) | Swerve Drive, Vision, LEDs |
| [Utilities](utilities/) | Feedforward, profiles, alerts, **Health Kit**, **RobotSafety**, **MotorType** |
| [Advanced](advanced/) | State-space control, signal processing, dynamic paths, **Live Tuning**, **Health Monitoring** |
| [Tools](tools/) | **Catalyst Builder**, **Tuner**, **Health Dashboard** — live on this site |
| [Examples](examples/) | Complete robot examples with elevator, intake, and more |
| [Testing](testing/) | How to test your FrcCatalyst-based code |

---

## Compatibility

| Component | Version |
|-----------|---------|
| WPILib | 2026.2.1 |
| CTRE Phoenix 6 | 26.1.1 |
| PhotonVision | v2026.3.1 |
| PathPlanner | 2026.1.2 |
| Java | 17+ |
