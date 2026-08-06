# Chlorine

A laptop-friendly performance mod for **Fabric, Minecraft 26.2**.

Named for the joke: Sodium (the mod) + Chlorine = salt. Chlorine is built
to sit *alongside* Sodium rather than compete with it — it doesn't touch
chunk rendering, memory layout, or GUI batching, since Sodium, FerriteCore
and ImmediatelyFast already do that well. Instead it covers the gaps:

| Feature | What it does | Why it's not already covered |
|---|---|---|
| **Adaptive perf scaler** | Watches a rolling FPS average and works down a ladder — simulation distance, then entity render distance, then particles — one tier at a time, restoring in reverse order as FPS recovers | Sodium makes each frame faster — it doesn't decide how much to simulate/draw around the player. Deliberately avoids render distance, which forces a full chunk-render-graph rebuild (a hitch) in both vanilla and Sodium whenever it changes |
| **Laptop power saver** | Caps framerate and hides clouds while the window is unfocused/minimized | None of Sodium/Iris/EntityCulling/ImmediatelyFast address unfocused-window heat, fan noise, or battery drain |
| **Distant mob AI throttle** | Skips goal-selector AI ticking (not physics) for mobs with no player nearby, on a staggered schedule | Tick-logic optimization, a different axis than Sodium entirely |
| **Item entity merging** | Periodically merges nearby stackable item drops near each player | Reduces entity count/tick+render load from dense farms, beyond what vanilla's touching-only merge does |
| **Low-end auto-tune** | On first launch, if your heap/core count look constrained, sets entity shadows, biome blend, mipmaps, particles, and ambient occlusion to lighter defaults | One-time setup nicety, not something the render-focused mods do |
| **In-game settings screen** | A real config screen (Mod Menu → Chlorine → Config), same as Sodium/Sodium Extra/Iris have | No need to hand-edit JSON to tune anything |

Chlorine works standalone, but is written to be fully compatible with
**Sodium, Iris, Lithium, FerriteCore, EntityCulling, and ImmediatelyFast** —
none of its features touch the same code paths those mods do.

**Note on dependencies:** Chlorine *requires* [Cloth Config API](https://modrinth.com/mod/cloth-config)
to be installed (it's what the settings screen is built with — without it
Chlorine won't load at all, rather than silently losing just the screen).
[Mod Menu](https://modrinth.com/mod/modmenu) is optional — it's what adds
the actual "Config" button; without it you can still edit
`config/chlorine.json` by hand.

## Building

You'll need a JDK 25, **Gradle 9.5.1** installed locally, and internet
access (Loom downloads Minecraft and dependencies on first run).

This repo doesn't include a Gradle wrapper (`gradlew`) — it needs a small
binary (`gradle-wrapper.jar`) that I couldn't generate without network
access. Two options:

- Install Gradle yourself ([sdkman](https://sdkman.io/), your package
  manager, or [gradle.org](https://gradle.org/install/)) and run:
  ```bash
  gradle build
  ```
- Or generate the wrapper once, using your installed Gradle, and commit
  it so future clones don't need Gradle installed:
  ```bash
  gradle wrapper --gradle-version 9.5.1
  ```
  After that, `./gradlew build` works like normal.

The output jar lands in `build/libs/chlorine-0.1.0.jar`. Drop that (and
Fabric API) into your `mods` folder.

### Building via GitHub Actions instead

This repo includes `.github/workflows/build.yml`, which installs Gradle
9.5.1 directly (no wrapper needed) and builds on push. Check the
**Actions** tab for the run, and download the built jar from the run's
**Artifacts** section. If it fails, the log will point at the exact
line — paste that back and I can fix the accessor/target it's
complaining about.

## Configuration

On first launch, Chlorine writes `config/chlorine.json` with sensible
defaults. Edit it and relaunch to change behavior — every option is
documented inline in `ChlorineConfig.java`. Key ones:

- `lowFpsThreshold` / `targetFps` — the FPS band that drives the adaptive scaler
- `minSimulationDistance` / `simulationDistanceStep` — tier 1 floor and step size
- `enableEntityDistanceScaling` / `minEntityDistanceScaling` / `entityDistanceScalingStep` — tier 2 floor and step size
- `enableParticleScaling` — whether tier 3 (drop to minimal particles) is allowed
- `lowerCooldownTicks` / `raiseCooldownTicks` — minimum time between drops vs. between raises
- `hideCloudsWhenUnfocused` — whether the power saver also hides clouds while unfocused
- `itemMergeIntervalTicks` / `itemMergeScanRadius` / `itemMergeRadius` — how often, how far, and how close items merge
- `autoTuneMaxMemoryMb` / `autoTuneMinCores` — thresholds for the low-end auto-tune to kick in
- `unfocusedFramerateLimit` — FPS cap while the window is unfocused
- `aiActiveRadius` / `aiThrottleInterval` — how far "no player nearby" means, and how aggressively to throttle AI beyond that

Set any feature's `enable...` flag to `false` to turn it off entirely.

**Note on adaptive simulation distance:** this intentionally scales
*simulation* distance, not render distance. Changing render distance isn't
a cheap incremental operation — both vanilla and Sodium rebuild the entire
chunk render graph when it changes (the same hitch as dragging the slider
in Video Settings), so Chlorine leaves it alone entirely. Simulation
distance controls server tick radius instead (entities, random ticks,
block updates), has no such rebuild cost, and targets CPU tick load
directly — usually the actual bottleneck on a weak laptop CPU. If you'd
rather it not touch this at all, set `enableAdaptiveSimulationDistance` to
`false`.

## ⚠️ Important: this targets a very new Minecraft version

Minecraft 26.2 (and the 26.1 unobfuscation switch before it) is only a
few weeks old as of this writing, and moved a large number of classes
around beyond just removing obfuscation. Confirmed working as of the last
successful CI build:

- `MobAiThrottleMixin.java`'s target (`Mob.customServerAiStep()`)
- `client.options.simulationDistance()`, `entityDistanceScaling()`,
  `framerateLimit()`, `cloudStatus()`, `entityShadows()`,
  `biomeBlendRadius()`, `mipmapLevels()`, `ambientOcclusion()`
- `ItemMerger.java`'s `ItemStack.isSameItemSameComponents(...)`
- The particle-status option, handled via `OptionsCompat.java`'s
  reflection-based enum lookup (its exact class/package still isn't
  pinned down, but the lookup-by-name approach means that doesn't matter)

Dropped entirely rather than guessed twice: a "graphics mode"
(Fast/Fancy/Fabulous) tweak in `LowEndAutoTune` — `Options#graphicsMode()`
doesn't appear to exist in 26.2 at all, likely reworked as part of the
26.1/26.2 rendering pipeline rewrite (Vulkan support was added).

**Not yet build-verified** — the newest addition, the in-game settings
screen:

- **`ChlorineConfigScreenBuilder.java`** — imports
  `net.minecraft.client.gui.screens.Screen` and
  `net.minecraft.network.chat.Component`. Both have been stable Mojmap
  locations for a long time and I'm fairly confident in them, but
  26.1/26.2 specifically reorganized GUI code (see the
  [migration primers](https://docs.neoforged.net/primer/docs/26.2/)), so
  this is the current highest-risk guess in the project. If it fails, the
  compiler error will say exactly which import is wrong.
- Cloth Config's own builder methods (`startBooleanToggle`,
  `startIntSlider`, `startIntField`, `startDoubleField`,
  `startLongField`) are much lower risk — that API has stayed close to
  identical across years of Minecraft versions, which is the whole reason
  this screen is built with Cloth Config instead of raw vanilla widgets.

Everything else (Fabric API events, FabricLoader config paths, Gson) is
Fabric-API/library-level code that doesn't depend on Minecraft's internal
naming, so it should be solid as-is.

If you hit compile errors, paste them back and I can help fix the exact
targets — that's a five-minute fix once we can see the real method
signatures.

## Project layout

```
src/main/java/com/chlorine/           common code (loads on client & server)
  Chlorine.java                       mod entrypoint
  ChlorineConfig.java                 JSON config
  ItemMerger.java                     periodic item-drop merging
  mixin/MobAiThrottleMixin.java       distant-mob AI throttle

src/client/java/com/chlorine/client/  client-only code
  ChlorineClient.java                 client entrypoint
  PerformanceScaler.java              adaptive simulation distance + particle fallback
  PowerSaver.java                     unfocused-window framerate cap
  LowEndAutoTune.java                 one-shot startup tuning for weak systems
  OptionsCompat.java                  reflection helper for the particle-status option
  ChlorineConfigScreenBuilder.java    in-game settings screen (Cloth Config)
  ChlorineModMenuIntegration.java     hooks the screen into Mod Menu's "Config" button
```

## License

MIT — do whatever you want with it.
