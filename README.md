# Chlorine

A laptop-friendly performance mod for **Fabric, Minecraft 26.2**.

it doesn't touch
chunk rendering, memory layout, or GUI batching, since Sodium, FerriteCore
and ImmediatelyFast already do that well. Instead it covers the gaps:

| Feature | What it does | Why it's not already covered |
|---|---|---|
| **Adaptive perf scaler** | Watches a rolling FPS average and works down a ladder — simulation distance, then entity render distance, then particles — one tier at a time, restoring in reverse order as FPS recovers | Sodium makes each frame faster — it doesn't decide how much to simulate/draw around the player. Deliberately avoids render distance, which forces a full chunk-render-graph rebuild (a hitch) in both vanilla and Sodium whenever it changes |
| **Laptop power saver** | Caps framerate and hides clouds while the window is unfocused/minimized | None of Sodium/Iris/EntityCulling/ImmediatelyFast address unfocused-window heat, fan noise, or battery drain |
| **Distant mob AI throttle** | Skips goal-selector AI ticking (not physics) for mobs with no player nearby, on a staggered schedule | Tick-logic optimization, a different axis than Sodium entirely |
| **Item entity merging** | Periodically merges nearby stackable item drops near each player | Reduces entity count/tick+render load from dense farms, beyond what vanilla's touching-only merge does |
| **Low-end auto-tune** | On first launch, if your heap/core count look constrained, sets entity shadows, biome blend, mipmaps, particles, graphics mode, and ambient occlusion to lighter defaults | One-time setup nicety, not something the render-focused mods do |

Chlorine works standalone, but is written to be fully compatible with
**Sodium, Iris, Lithium, FerriteCore, EntityCulling, and ImmediatelyFast** —
none of its features touch the same code paths those mods do.

## Building

You'll need a JDK 25 and internet access (Loom downloads Minecraft, your
mappings, and dependencies on first run).

```bash
./gradlew build
```

The output jar lands in `build/libs/chlorine-0.1.0.jar`. Drop that (and
Fabric API) into your `mods` folder.

### Building via GitHub Actions instead

This repo includes `.github/workflows/build.yml`. Push it to a GitHub repo
and it'll build automatically on real infrastructure (with real internet
access, unlike the sandbox this was written in) — check the **Actions**
tab for the run, and download the built jar from the run's **Artifacts**
section. If it fails, the log will point at the exact line — paste that
back and I can fix the accessor/target it's complaining about.

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
few weeks old as of this writing. I built this using Mojang's official
mapping names, which are generally stable and well-documented — but I
wasn't able to actually compile or run this in the environment I wrote it
in (no internet access to pull Minecraft/Loom dependencies), so I can't
guarantee it compiles clean on the first try. The two spots most likely
to need a tweak if `./gradlew build` fails:

1. **`MobAiThrottleMixin.java`** — targets `Mob.customServerAiStep()`.
   This method name has been stable across many Minecraft versions, but
   if Mixin can't find it, open `net.minecraft.world.entity.Mob` in your
   IDE (after a Gradle sync pulls in sources) and fix the `method = "..."`
   value to match.
2. **`PerformanceScaler.java` / `PowerSaver.java` / `LowEndAutoTune.java`** — call
   `client.options.simulationDistance()`, `client.options.entityDistanceScaling()`,
   `client.options.particles()`, `client.options.framerateLimit()`,
   `client.options.cloudStatus()`, `client.options.entityShadows()`,
   `client.options.biomeBlendRadius()`, `client.options.mipmapLevels()`,
   `client.options.graphicsMode()`, `client.options.ambientOcclusion()`,
   which all return `OptionInstance<T>`. If `Options` exposes any of these
   differently in 26.2, adjust the relevant line. `ambientOcclusion()` is
   the shakiest guess of the bunch — it may turn out to be an enum
   (`AmbientOcclusionStatus`) rather than a boolean.
3. **`ItemMerger.java`** — calls `ItemStack.isSameItemSameComponents(...)`,
   the post-item-component-rework equality check. If that name has moved,
   open `net.minecraft.world.item.ItemStack` and swap in the current one.

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
```

## License

MIT — do whatever you want with it.
