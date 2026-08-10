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
| **Distant mob AI throttle** | Skips goal-selector AI ticking (not physics) for classic AI mobs (zombies, skeletons, etc.) with no player nearby, staggered | Tick-logic optimization, a different axis than Sodium entirely |
| **Distant Brain-mob throttle** | Same idea for Brain-based mobs (villagers, piglins, hoglins, allays...) — they don't use the goal selector at all, so this is a separate mixin targeting `Brain.tick()` directly | Villages/bastions with many mobs are a well-known lag source Lithium doesn't fully own; not covered by the goal-selector throttle above since Brain mobs override that method entirely |
| **Item entity merging** | Periodically merges nearby stackable item drops near each player | Reduces entity count/tick+render load from dense farms, beyond what vanilla's touching-only merge does |
| **XP orb merging** | Same idea, for `ExperienceOrb` entities | Grinders/farms leave many small floating orbs; implemented defensively (reflection-based, disables itself rather than risk losing XP — see `XpOrbMerger.java`) |
| **Sound instance budget** | Caps how many new sounds can start in the same client tick | Not about distance (vanilla already culls that) — about smoothing audio-engine bursts (e.g. a mob farm killing dozens of mobs at once), which nothing else targets |
| **Particle spawn budget** | Same idea, for particles — caps new particles per client tick | Fireworks/potion clouds/explosions can spike frame time in one instant regardless of individual particle cost |
| **Particle spawn budget** | Same idea, for particles — caps new particles per client tick | Fireworks, potion clouds, big explosions can spike frame time all at once regardless of any individual particle's cost |
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

You'll need a JDK 25 and internet access the first time (Loom downloads
Minecraft and dependencies on first run). A Gradle wrapper is included.

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

The installable output lands in `build/libs/chlorine-0.1.8.jar`. Drop that
(and Fabric API) into your `mods` folder. Do not install the `-sources.jar`:
it is source code for IDEs, not a Minecraft mod.

### Building via GitHub Actions instead

This repo includes `.github/workflows/build.yml`, which uses the committed
Gradle wrapper and builds on push. Check the
**Actions** tab for the run, and download the built jar from the run's
**Artifacts** section. The workflow uploads only the installable mod JAR,
not the sources JAR. If it fails, the log will point at the exact
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
- **`BrainTickThrottleMixin.java`** — targets
  `net.minecraft.world.entity.ai.Brain#tick(ServerLevel, LivingEntity)`.
  This has been a stable Mojmap location/signature since the Brain system
  was introduced in 1.14, and 26.2 focused on rendering/registration
  rather than AI, so it's a reasonable bet — but like the settings
  screen, this hasn't been through a CI run yet.
- **`SoundBudgetMixin.java`** — targets
  `net.minecraft.client.sounds.SoundManager#play(SoundInstance)` and
  `net.minecraft.client.resources.sounds.SoundInstance`. The sound system
  hasn't changed much since its 1.13 rewrite, so this is one of the
  lower-risk guesses here, but still unverified against 26.2.
- **`ParticleBudgetMixin.java`** — targets
  `net.minecraft.client.particle.ParticleEngine#add(Particle)`. Same
  reasoning as the sound budget — the particle engine's core "register
  this for rendering" entry point hasn't moved much historically — lower
  risk, still unverified against 26.2.
- **`XpOrbMerger.java`** — deliberately does *not* carry the same "if
  this fails, fix the accessor" caveat as everything else, because it's
  designed not to need one: it looks up `ExperienceOrb`'s value field by
  name via reflection and permanently disables itself for the session if
  that lookup or an update ever fails, rather than risk silently losing
  a player's XP. If merging never seems to activate, check the log for
  a "disabling XP orb merging" warning, then open
  net.minecraft.world.entity.ExperienceOrb in your IDE, confirm the real
  field name, and update `VALUE_FIELD_NAME` in the file.

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
  XpOrbMerger.java                    periodic XP orb merging (fails safe via reflection)
  mixin/MobAiThrottleMixin.java       distant-mob AI throttle (classic goal-selector mobs)
  mixin/BrainTickThrottleMixin.java   distant Brain-mob throttle (villagers, piglins, ...)

src/client/java/com/chlorine/client/  client-only code
  ChlorineClient.java                 client entrypoint
  PerformanceScaler.java              adaptive simulation distance + particle fallback
  PowerSaver.java                     unfocused-window framerate cap
  LowEndAutoTune.java                 one-shot startup tuning for weak systems
  OptionsCompat.java                  reflection helper for the particle-status option
  ChlorineConfigScreenBuilder.java    in-game settings screen (Cloth Config)
  ChlorineModMenuIntegration.java     hooks the screen into Mod Menu's "Config" button

src/client/java/com/chlorine/mixin/client/  client-only mixins
  SoundBudgetMixin.java                caps new sounds started per client tick
  ParticleBudgetMixin.java             caps new particles started per client tick
```

## License

MIT — do whatever you want with it.
