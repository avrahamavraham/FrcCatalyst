---
layout: default
title: Tools
nav_order: 8
has_children: false
---

# Catalyst Tools
{: .no_toc }

Browser-based, zero-install tooling that ships next to the library.
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Open a tool

Everything below is hosted right on this GitHub Pages site — no download,
no `npm install`, no build step. Click and go. The same files also live in
the repo under `docs/tools/` if you want to run them off your laptop while
disconnected from the internet.

<style>
.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  margin: 18px 0 28px;
}
.tool-card {
  display: block;
  padding: 18px 18px 16px;
  border: 1px solid var(--border-color, #2a3050);
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  background: var(--code-background-color, rgba(255,255,255,0.03));
  transition: transform 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease;
}
.tool-card:hover {
  transform: translateY(-2px);
  border-color: #e94560;
  box-shadow: 0 6px 14px rgba(0,0,0,0.25);
  text-decoration: none;
}
.tool-card .icon { font-size: 26px; margin-bottom: 8px; display: block; }
.tool-card .name { font-weight: 700; font-size: 16px; margin: 0 0 6px; color: #e94560; }
.tool-card .desc { font-size: 13px; color: var(--body-text-color, #555); line-height: 1.5; }
.tool-card .tag {
  display: inline-block;
  font-size: 10px; font-weight: 700;
  text-transform: uppercase; letter-spacing: 1px;
  padding: 2px 7px; border-radius: 999px;
  margin-top: 10px;
}
.tag.live { background: rgba(74, 222, 128, 0.18); color: #4ade80; }
.tag.calc { background: rgba(96, 165, 250, 0.18); color: #60a5fa; }
.tag.docs { background: rgba(233, 69, 96, 0.18); color: #e94560; }
</style>

<div class="tool-grid">

<a class="tool-card" href="builder/">
  <span class="icon">🛠️</span>
  <p class="name">Catalyst Builder</p>
  <p class="desc">Form-driven Java code generator for every mechanism. Persistence, .java download, full-subsystem-class mode, snippet import.</p>
  <span class="tag docs">Generator</span>
</a>

<a class="tool-card" href="tuner/">
  <span class="icon">🎚</span>
  <p class="name">Catalyst Tuner</p>
  <p class="desc">Live NT4 PID + Motion Magic tuner. Drag sliders against your running robot, export the final gains as Java or JSON.</p>
  <span class="tag live">Robot-connected</span>
</a>

<a class="tool-card" href="health/">
  <span class="icon">🩺</span>
  <p class="name">Health Dashboard</p>
  <p class="desc">Live NT4 viewer for <code>/Catalyst/Health/</code>. Per-subsystem cards, severity filters, search, report download.</p>
  <span class="tag live">Robot-connected</span>
</a>

<a class="tool-card" href="motion/">
  <span class="icon">📈</span>
  <p class="name">Motion Profile Visualizer</p>
  <p class="desc">Sketch a Motion Magic profile before committing. Cruise / accel / jerk → live s-v-a curves and a paste-ready <code>.motionMagic(...)</code> snippet.</p>
  <span class="tag calc">Calculator</span>
</a>

<a class="tool-card" href="motors/">
  <span class="icon">⚡</span>
  <p class="name">MotorType Browser</p>
  <p class="desc">Searchable table of every <code>MotorType</code> preset with a built-in gear-ratio calculator. Max RPM, output torque, holding voltage at a glance.</p>
  <span class="tag calc">Calculator</span>
</a>

</div>

> The three robot-connected tools speak plain NT4 WebSocket (port `5810`) and
> pull msgpack from a public CDN. Nothing is uploaded anywhere, and there's
> no telemetry back to the library. The calculator tools work entirely
> offline once the page has loaded.

---

## Catalyst Builder

Form on the left, generated Java on the right. Pick a mechanism, fill in
CAN IDs, gear ratios, PID gains, and any followers — the code preview
updates live. Two output modes:

- **Config snippet** (default) — just the `Foo.Config.builder()...build()`
  call. Drop it inside your existing `RobotContainer` or subsystem.
- **Full subsystem class** — wraps the config in a complete
  `public class FooSubsystem extends SubsystemBase` skeleton with the
  default-command pattern teams typically want. Toggle at the top of the
  preview pane.

Other niceties shipped in v0.3.5.1:

- **Persistence.** Your work is saved to `localStorage` per mechanism. Open
  the page tomorrow and the form is still filled in.
- **Download as `.java`.** One-click download of the generated code as a
  file ready to drop into `src/main/java/`.
- **Import existing config.** Paste any `Foo.Config.builder()...build()`
  snippet (yours or a teammate's) and the form fills itself in from it.

The Builder is inspired by [tcrvo / yteam3211's FRC Catalyst Subsystem Generator](https://yteam3211.github.io/frc-catalyst-subsystem-generator).

---

## Catalyst Tuner

A browser-based live tuner for every PID and Motion Magic gain Catalyst
v0.3.2+ publishes under `Catalyst/Tuning/`.

- Enter the robot's host (`10.TE.AM.2` on the field, `roborio-####-frc.local`
  on tethered, `127.0.0.1` when running the simulator) and click Connect.
- The tuner subscribes to `Catalyst/Tuning/` and auto-discovers mechanisms.
- Drag a slider or type a number — the change is written back over NT4
  immediately. The mechanism re-applies it on the next periodic.
- **Reset all to defaults** rewrites every gain to the value it had when
  the tuner first saw it (which is the value from your Config builder).
- **Export as Java** produces builder-snippet code you can paste straight
  back into your robot project.
- **Download gains JSON** saves a snapshot of every tuned value to a
  `.json` file — useful for archiving working tunes between events.
- **Lock for competition** publishes a hint string to NT, but the real
  lockdown is calling `TunableNumber.disableTuning()` once in
  `robotInit()`. See [the live tuning guide](../advanced/tuning.html).

The tuner uses the standard NT4 WebSocket protocol on port 5810 and the
`@msgpack/msgpack` library loaded from esm.sh. No build step, no install.

---

## Health Dashboard

Single-file viewer that subscribes to `/Catalyst/Health/...` and shows
every registered `HealthCheck` grouped by subsystem.

- Severity-colored status pills (HEALTHY / WARN / ERR).
- Filter buttons: **All / Firing only / Errors only** plus a search box.
- ERROR-level firing dots pulse so they're impossible to miss across the
  pit.
- **Download report** writes a plain-text snapshot of every check's
  current state — paste into a team chat when you're triaging.
- Lights up `/Catalyst/Safety/Tripped` so when the `RobotSafety` watchdog
  fires you see it at the top of the page.

See [docs/advanced/health.md](../advanced/health.html) for the API and
how to add your own checks.

---

## Motion Profile Visualizer

Open at [tools/motion/](motion/). Numerically integrates a trapezoidal
(jerk = 0) or S-curve (jerk &gt; 0) Motion Magic profile and draws live
position / velocity / acceleration curves as you drag the sliders.

- **Sliders** for distance, cruise velocity, acceleration, and jerk.
- **Built-in presets** for common patterns (geared elevator, slow arm,
  flywheel spin-up, quick S-curve snap).
- **Live stats** — total time, time at cruise, whether the move actually
  reaches cruise velocity (helpful to spot under-aggressive accel).
- **Copy `.motionMagic(…)` snippet** with one click — drops the current
  cruise/accel/jerk into the form a Catalyst mechanism expects.

Useful when picking starting Motion Magic constants for a new mechanism
or sanity-checking whether your accel limit is actually fast enough to
ever leave the ramp-up phase.

## MotorType Browser

Open at [tools/motors/](motors/). Interactive table of every
[`MotorType`](../utilities/#motortype) preset Catalyst ships, with a
side-panel calculator that does the math you'd otherwise scribble on
the side of your notebook:

- **Sortable, filterable table** — by name, torque, free speed, current.
  Vendor / FOC chips on each row.
- **Calculator** — pick a motor, type in a gear ratio + motor count
  (and optionally a load torque), get max mechanism RPM, output stall
  torque, total stall current, and the gravity-feedforward holding
  voltage estimate.
- **Copy `MotorType.X`** as a constant reference, or **copy a builder
  line** ready to paste into any Catalyst Config builder.

Specs match the values inside
[`src/main/java/frc/lib/catalyst/hardware/MotorType.java`](https://github.com/TomAs-1226/FrcCatalyst/blob/main/src/main/java/frc/lib/catalyst/hardware/MotorType.java)
and WPILib 2026's `DCMotor.getKraken*` / `getFalcon500*` factories.

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

---

## Hosting

`docs/` is a Jekyll site published via GitHub Pages. All three tools are
single self-contained HTML files with no build step, so they work either
on the docs site or by opening the local `index.html` directly in any
modern browser.
