---
layout: default
title: Driver
nav_order: 4
has_children: false
---

# Driver
{: .no_toc }

Controller-feel APIs added in v0.4.0-beta.
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## DriverProfile — deadband, curve, speed cap

Per-driver feel in one config object. Wrap a joystick `DoubleSupplier`
once and the swerve drive gets a shaped supplier in return. Swap
profiles to swap drivers.

```java
DriverProfile alice = DriverProfile.builder()
    .deadband(0.06)                       // radial deadband on raw input
    .curve(DriverProfile.Curve.CUBIC)     // y = x³ — softer near center
    .maxSpeed(0.85)                       // cap output at 85% of max
    .slowMode(0.25)                       // when slow mode is engaged
    .build();

drive.setDefaultCommand(drive.advancedDrive(
    alice.shape(() -> -driver.getLeftY()),
    alice.shape(() -> -driver.getLeftX()),
    alice.shape(() -> -driver.getRightX()),
    0.0));                                // deadband handled inside the profile

driver.leftBumper().onTrue (Commands.runOnce(alice::engageSlowMode));
driver.leftBumper().onFalse(Commands.runOnce(alice::disengageSlowMode));
```

### Response curves

| Curve | Formula | Feel |
|---|---|---|
| `LINEAR`    | `y = x`                                | Closest to raw |
| `QUADRATIC` | `y = x · \|x\|`                         | Gentler ramp from center |
| `CUBIC`     | `y = x³`                               | Soft center, sharp top — common for skilled drivers |
| `EXPO`      | `y = sign(x) · (eᵃˣ−1) / (eᵃ−1)`, a=3   | Aggressive shaping for fine alignment |

Defaults: deadband `0.05`, curve `LINEAR`, max speed `1.0`, slow
multiplier `0.3`. Override only what you want.

---

## RumbleEvents — bind triggers to controller rumble

Buzz the driver when auto-align finishes, buzz the operator when the
intake grabs a piece, buzz both for a `RobotSafety` trip. Patterns are
dispatched per-loop so overlapping events don't fight.

```java
RumbleEvents events = new RumbleEvents(driver.getHID(), operator.getHID());

events.onTrigger(swerve.atAlignmentTarget(),  Pattern.DOUBLE_TAP, Channel.DRIVER);
events.onTrigger(claw.hasPieceTrigger(),      Pattern.SHORT,      Channel.BOTH);
events.onTrigger(RobotSafety.trippedTrigger(),Pattern.LONG,       Channel.BOTH);

// In Robot.robotPeriodic():
@Override public void robotPeriodic() {
    CommandScheduler.getInstance().run();
    events.update();
}
```

### Patterns

| Pattern | Duration | Use |
|---|---|---|
| `SHORT`       | ~120 ms                      | "got it" feedback (piece intaken, aligned) |
| `LONG`        | ~400 ms                      | Warnings, fault notifications |
| `DOUBLE_TAP`  | two 80 ms buzzes, 60 ms gap  | "ready" — about to score, climber armed |
| `TRIPLE_TAP`  | three 70 ms buzzes           | High-importance alerts |
| `RAMP`        | 300 ms ramp-up               | "charging up" — flywheel spinning |

### Channels

- `Channel.DRIVER` — only the controller passed as the first arg
- `Channel.OPERATOR` — only the second arg
- `Channel.BOTH` — both controllers

Pass `null` for either constructor argument to skip that channel
entirely (useful when only one operator station exists).

### Manual fire

For one-off events that aren't trigger-driven:

```java
events.fire(Pattern.TRIPLE_TAP, Channel.BOTH);  // sound the alarm now
events.clear();                                  // stop every active rumble
```

---

## Pattern: integrate the two

The setup most teams converge on:

```java
public class RobotContainer {
    private final CommandXboxController driver   = new CommandXboxController(0);
    private final CommandXboxController operator = new CommandXboxController(1);

    private final DriverProfile profile = DriverProfile.builder()
        .deadband(0.05).curve(Curve.CUBIC).maxSpeed(0.9).slowMode(0.3).build();

    private final RumbleEvents events = new RumbleEvents(
        driver.getHID(), operator.getHID());

    public RobotContainer() {
        drive.setDefaultCommand(drive.advancedDrive(
            profile.shape(() -> -driver.getLeftY()),
            profile.shape(() -> -driver.getLeftX()),
            profile.shape(() -> -driver.getRightX()),
            0.0));

        driver.leftBumper().onTrue (Commands.runOnce(profile::engageSlowMode));
        driver.leftBumper().onFalse(Commands.runOnce(profile::disengageSlowMode));

        events.onTrigger(claw.hasPieceTrigger(), Pattern.SHORT, Channel.BOTH);
        events.onTrigger(swerve.atAlignmentTarget(), Pattern.DOUBLE_TAP, Channel.DRIVER);
        events.onTrigger(RobotState.lateMatch(20),   Pattern.LONG, Channel.BOTH);
    }
}
```
