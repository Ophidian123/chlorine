# Chlorine

Chlorine is a laptop-friendly Fabric performance mod for **Minecraft 26.2**.
It is designed to complement—not replace—Sodium, Iris, Lithium, FerriteCore,
EntityCulling, and ImmediatelyFast.

## Current features

| Feature | Purpose | Why it does not overlap the recommended mods |
|---|---|---|
| Adaptive performance scaler | Reduces simulation distance, then entity distance and particle settings when sustained FPS is low; restores them slowly when FPS recovers. | Sodium optimizes rendering, but does not automatically choose lower-cost gameplay/visual settings. |
| Unfocused-window power saver | Reduces FPS and optionally hides clouds while Minecraft is unfocused. | This targets battery use, heat, and fan noise rather than rendering implementation. |
| Distant classic-mob AI throttle | Runs goal-selector updates less often when no player is nearby. | Lithium optimizes vanilla logic without changing timing; Chlorine deliberately trades distant, unobserved AI responsiveness for fewer ticks. |
| Distant Brain-mob throttle | Applies the same idea to Brain-driven mobs such as villagers and piglins. | Covers a separate AI path from classic goal selectors. |
| Adaptive item merging | Combines compatible nearby item entities and switches to a faster cleanup cadence after a dense merge pass. | Reduces farm entity buildup without touching rendering, culling, or memory internals. |
| Adaptive XP-orb merging | Combines nearby experience orbs, using the same burst cleanup and disabling itself safely if it cannot verify an update. | Reduces entity count; it does not touch rendering or memory internals. |
| Tick diagnostics | Optionally logs sustained server-work time above a chosen threshold. | Measures possible tick pressure; it does not overlap or replace Lithium's logic optimizations. |
| Low-end auto-tune | On first launch, applies lower-cost visual defaults when the available heap or CPU core count is constrained. | This is an opt-in settings policy, not a renderer rewrite. |
| Mod Menu configuration | Provides an in-game settings screen through Cloth Config and Mod Menu. | Configuration only. |

### Deliberately removed in 0.1.8

Sound and particle per-tick budgets were removed because their Minecraft 26.2
targets changed and caused mixin startup crashes. They are not included in the
0.1.9 JAR.

## Candidate future features

These are ideas to profile and implement one at a time. None duplicate Sodium's
renderer, Iris shader pipeline, FerriteCore's memory optimizations,
ImmediatelyFast's immediate-mode batching, or EntityCulling's visibility work.

| Candidate | Benefit | Compatibility notes |
|---|---|---|
| Chunk-generation travel governor | During sustained generation lag, temporarily lower simulation distance while flying/exploring, then restore it. | Avoids touching Sodium's chunk renderer; should never alter render distance automatically. |
| Configurable item/XP merge filters | Let players exempt named items, equipment, or particular item types from merging. | Improves gameplay safety rather than raw throughput; no overlap. |
| Idle singleplayer saver | Detect a stationary, unfocused, or paused player and lower the integrated server's simulation cost. | Must never apply on multiplayer clients or while a world is actively being played. |
| Tick-time diagnostics | Optional lightweight warnings that identify whether entity count, block entities, or chunk generation appears to be the bottleneck. | Measures and reports; does not replace Lithium's optimizations. |
| Low-end preset profiles | Offer conservative, balanced, and battery-saver presets for the existing safe settings. | Settings orchestration only; no renderer/memory/culling overlap. |

Not planned: chunk rendering rewrites (Sodium), shaders (Iris), memory-data
deduplication (FerriteCore), visibility culling (EntityCulling), or GUI/entity
draw batching (ImmediatelyFast). Lithium already covers broad, behavior-preserving
game-logic optimization; Chlorine should only add clearly opt-in policy changes
where a player accepts the tradeoff.

## Dependencies

- Required: Fabric Loader, Fabric API, and Cloth Config API.
- Optional: Mod Menu (for the Config button).
- Recommended alongside Chlorine: Sodium, Iris, Lithium, FerriteCore,
  EntityCulling, and ImmediatelyFast.

## Building

Use JDK 25. The Gradle wrapper downloads required build dependencies on the
first run.

```bash
# Windows PowerShell
.\gradlew.bat build

# macOS/Linux
./gradlew build
```

The installable mod is `build/libs/chlorine-0.1.10.jar`. Do **not** install the
`-sources.jar`; it is for IDEs only.

If this repository is uploaded with its outer `chlorine-mod-src_4` directory,
use the root workflow at `.github/workflows/build-chlorine.yml`; it explicitly
builds the nested `chlorine` project and uploads only the installable JAR.

## 0.1.10 additions

- **Adaptive merge bursts:** After a pass merges 24 or more item entities or
  XP orbs, Chlorine performs the next pass after 20 ticks instead of the
  normal interval. It also de-duplicates entities visible to overlapping
  player scan areas, avoiding repeated work on multiplayer farms.
- **Optional tick diagnostics:** Disabled by default. When enabled, it logs
  an average server-work time over a configurable sampling window; it does
  not alter gameplay or performance settings automatically.

## Configuration

On first launch Chlorine writes `config/chlorine.json`. Key controls include:

- Adaptive scaling: `lowFpsThreshold`, `targetFps`, `minSimulationDistance`,
  `simulationDistanceStep`, `enableEntityDistanceScaling`, and
  `enableParticleScaling`.
- Power saver: `unfocusedFramerateLimit` and `hideCloudsWhenUnfocused`.
- AI throttles: `aiActiveRadius`, `aiThrottleInterval`, and
  `brainThrottleInterval`.
- Merging: the `itemMerge...` / `xpMerge...` settings, including the
  `...BurstThreshold` and `...BurstIntervalTicks` controls.
- Diagnostics: `enableTickDiagnostics`, `tickDiagnosticsIntervalTicks`, and
  `tickDiagnosticsWarnMs`.
- Auto-tune: `autoTuneMaxMemoryMb` and `autoTuneMinCores`.

Every `enable...` setting can be set to `false` to turn that feature off.

## License

All Rights Reserved (ARR)

Copyright (c) 2026 Chlorine contributors. This software is proprietary and confidential.
Unauthorized copying, modification, distribution, or use of this software in any form is strictly prohibited.
