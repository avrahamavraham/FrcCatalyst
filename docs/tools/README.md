---
layout: default
title: Tools
nav_order: 8
has_children: false
---

# Catalyst Tools

A small toolbox that ships next to the library. Everything here is static —
GitHub Pages serves it directly, nothing to install.

## Catalyst Tuner

A browser-based live tuner for every PID and Motion Magic gain Catalyst
v0.3.2+ publishes under `Catalyst/Tuning/`.

- Open `docs/tools/tuner/index.html` directly, or visit it on the
  documentation site once Pages is built.
- Enter the robot's host (`10.TE.AM.2` on the field, `roborio-####-frc.local`
  on tethered, `127.0.0.1` when running the simulator) and click Connect.
- The tuner subscribes to `Catalyst/Tuning/` and auto-discovers mechanisms.
- Drag a slider or type a number — the change is written back over NT4
  immediately. The mechanism re-applies it on the next periodic.
- "Reset all to defaults" rewrites every gain to the value it had when the
  tuner first saw it (which is the value from your Config builder).
- "Export as Java" produces builder-snippet code you can paste straight back
  into your robot project.
- "Lock for competition" publishes a hint string to NT — but the real
  competition lockdown is calling `TunableNumber.disableTuning()` once in
  `robotInit()`. See [docs/advanced/tuning.md](../advanced/tuning.html).

The tuner uses the standard NT4 WebSocket protocol on port 5810 and the
`@msgpack/msgpack` library loaded from esm.sh. No build step, no install.

## AdvantageScope tab bundles

The files in `docs/tools/advantagescope/` describe the field paths each
mechanism publishes and a recommended way to visualize them. Each bundle is
a small JSON file you can use as a template when you set up tabs in
AdvantageScope (or Elastic, Glass, Shuffleboard — the schema is dashboard-
agnostic).

### Available bundles

| Mechanism | File |
|---|---|
| LinearMechanism | [linear.json](advantagescope/linear.json) |
| RotationalMechanism | [rotational.json](advantagescope/rotational.json) |
| FlywheelMechanism | [flywheel.json](advantagescope/flywheel.json) |
| RollerMechanism | [roller.json](advantagescope/roller.json) |
| WinchMechanism | [winch.json](advantagescope/winch.json) |
| ClawMechanism | [claw.json](advantagescope/claw.json) |
| DifferentialWristMechanism | [diffwrist.json](advantagescope/diffwrist.json) |
| PneumaticMechanism | [pneumatic.json](advantagescope/pneumatic.json) |

### How to use a bundle

1. Open the JSON for the mechanism you want to monitor.
2. Substitute the `{MECH}` placeholder with the actual mechanism name from
   your robot's Config builder (e.g. `Arm`, `Elevator`, `Shooter`).
3. In AdvantageScope, create a new tab for each entry in the bundle's `tabs`
   array. Set the tab type to the value of `type` (Line Graph, Table,
   Console).
4. For each axis, drag the listed NT keys onto the matching axis (left/right).

The schema for these files is documented at
[`catalyst-tab-bundle.schema.json`](catalyst-tab-bundle.schema.json). It is
intentionally tool-agnostic so the same bundle works for any dashboard.

### Why not a native AdvantageScope `.json` layout?

AdvantageScope's native layout format is version-sensitive and changes
between releases. A portable bundle that lists field paths + the intended
chart shape stays useful across AS versions, and works for teams running
Elastic or Glass instead.

## Hosting

`docs/` is a Jekyll site published via GitHub Pages. The tuner is a single
self-contained HTML file with no build step, so it works either at
`https://frccatalyst.github.io/FrcCatalyst/tools/tuner/` once Pages is
rebuilt, or by opening the local `index.html` directly in any modern browser.
