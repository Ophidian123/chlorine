package com.chlorine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain JSON config stored at config/chlorine.json. Every field here is a
 * public mutable field on purpose — PerformanceScaler/PowerSaver read
 * these live. You can edit the JSON file directly, or use the in-game
 * settings screen (Mod Menu → Chlorine → Config, powered by Cloth Config —
 * see ChlorineConfigScreenBuilder.java), which just writes back to this
 * same file.
 */
public class ChlorineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("chlorine.json");

    // --- Adaptive simulation distance (client) ---
    // Deliberately targets SIMULATION distance, not render distance.
    // Render distance changes force a full chunk-render-graph rebuild in
    // both vanilla and Sodium (a hitch across all visible terrain) — see
    // PerformanceScaler.java for the full explanation. Simulation distance
    // is a separate, server-tick-facing value (entity ticking, random
    // ticks, block updates) with no such rebuild cost, so it can be
    // adjusted freely and still meaningfully reduces CPU tick load, which
    // is usually the actual bottleneck on a weak laptop CPU anyway.
    public boolean enableAdaptiveSimulationDistance = true;
    /** Never scale simulation distance below this. */
    public int minSimulationDistance = 1;
    /**
     * A hard ceiling on simulation distance, enforced independently of and
     * respected by the adaptive scaler and the chunk-gen governor above.
     * Off by default (existing behavior — raising toward your session's
     * actual original value — is unchanged unless you turn this on). See
     * SimDistanceCap.java: neither of the systems above can truly know a
     * given value is unsustainable for your CPU long-term, since they
     * only react to FPS/speed reading fine in the specific instant they
     * check before raising — this is a plain, always-correct backstop.
     */
    public boolean enableSimDistanceCap = false;
    /** Simulation distance will never be allowed to exceed this, regardless of any other system's target. */
    public int maxSimulationDistance = 8;
    /** Below this average FPS, simulation distance steps down. */
    public int lowFpsThreshold = 30;
    /** Above this average FPS, simulation distance steps back up (toward your original setting). */
    public int targetFps = 55;
    /** How many client ticks to average FPS over before making a decision. */
    public int fpsCheckWindowTicks = 100;
    /** How many chunks to move per adjustment. */
    public int simulationDistanceStep = 2;
    /** Minimum ticks between lowering simulation distance. */
    public int lowerCooldownTicks = 100; // 5s — no rebuild cost, so this can react quickly
    /** Minimum ticks between raising simulation distance back up. */
    public int raiseCooldownTicks = 400; // 20s
    /** If FPS is still bad once simulation distance is already at its floor, also drop particles to minimal. */
    public boolean enableParticleScaling = true;
    /** Also scale down entity render distance (0.5–5.0 multiplier) once simulation distance is maxed out. */
    public boolean enableEntityDistanceScaling = true;
    /** Floor for entity distance scaling. */
    public double minEntityDistanceScaling = 0.5;
    /** How much to move entity distance scaling per adjustment. */
    public double entityDistanceScalingStep = 0.25;

    // --- Laptop power saver (client) ---
    public boolean enablePowerSaver = true;
    /** Framerate cap applied while the window is unfocused/minimized. */
    public int unfocusedFramerateLimit = 15;
    /** Also hide clouds while the window is unfocused (cheap, purely cosmetic while you're not looking). */
    public boolean hideCloudsWhenUnfocused = true;

    // --- Distant mob AI throttle (server/common) ---
    // Only affects classic goal-selector mobs (zombies, skeletons, etc.)
    // — Brain-based mobs (villagers, piglins, ...) override
    // customServerAiStep() entirely rather than calling super(), so this
    // mixin never actually reaches them. See enableBrainThrottle below
    // for the Brain-based equivalent.
    public boolean enableDistantMobAiThrottle = true;
    /** Mobs within this many blocks of any player always tick AI normally. */
    public double aiActiveRadius = 48.0;
    /** Beyond that radius, only run the AI goal selector every Nth tick. */
    public int aiThrottleInterval = 8;

    // --- Distant Brain-based mob throttle (server/common) ---
    // Villagers, piglins, hoglins, allays, frogs, and other Brain-driven
    // mobs don't use the classic goal selector at all — they run through
    // Brain.tick(), which handles memory/sensor updates and behavior
    // selection. This is often the more expensive path (villages/piglin
    // bastions with many mobs), and it's not something Lithium or the
    // mob AI throttle above already covers. Same shape as that throttle:
    // skip Brain.tick() on off-ticks for Brain-owners with no player
    // nearby, staggered by entity ID. Reuses aiActiveRadius above for the
    // "someone's watching" distance.
    public boolean enableBrainThrottle = true;
    /** Beyond aiActiveRadius, only run Brain.tick() every Nth tick. */
    public int brainThrottleInterval = 8;

    // --- Item entity merging (server/common) ---
    // Vanilla already merges touching item stacks, but dense drops (mob
    // farms, crop farms) still end up with dozens of separate item
    // entities each ticking and rendering individually. This periodically
    // scans near each player and merges stackable items within range —
    // pure gameplay-visible cleanup, no rendering/AI internals involved.
    public boolean enableItemMerging = true;
    /** How often to run a merge pass. */
    public int itemMergeIntervalTicks = 100; // 5s
    /** How far around each player to scan for items to merge. */
    public double itemMergeScanRadius = 48.0;
    /** Items within this many blocks of each other get merged. */
    public double itemMergeRadius = 2.0;

    // --- XP orb merging (server/common) ---
    // Same idea as item merging, applied to ExperienceOrb — grinders and
    // farms can leave dozens of small floating orbs ticking individually.
    // Implemented defensively via reflection (see XpOrbMerger.java): if
    // the orb's internal value field can't be found/updated, merging is
    // silently disabled rather than risking any XP loss. No orb is ever
    // discarded unless its value was successfully added to another first.
    public boolean enableXpOrbMerging = true;
    /** How often to run a merge pass. */
    public int xpMergeIntervalTicks = 100; // 5s
    /** How far around each player to scan for orbs to merge. */
    public double xpMergeScanRadius = 48.0;
    /** Orbs within this many blocks of each other get merged. */
    public double xpMergeRadius = 2.0;

    // --- Sound instance budget (client) ---
    // Not distance-based — vanilla already attenuates/culls inaudible
    // sounds. This caps how many *new* sounds can start within the same
    // client tick, so a burst (e.g. a mob farm killing dozens of mobs at
    // once) doesn't slam the audio engine all in one moment. Overflow
    // sounds for that tick are simply dropped, not queued/delayed.
    public boolean enableSoundBudget = true;
    /** Max new sounds allowed to start per client tick. */
    public int maxNewSoundsPerTick = 8;

    // --- Particle spawn budget (client) ---
    // Same shape as the sound budget, same reasoning: not distance-based
    // (vanilla already culls particles that fall outside relevant
    // ranges), this caps how many *new* particles can be created within
    // the same client tick, so a burst (fireworks, a big potion cloud, a
    // large explosion) doesn't spike frame time all at once. Overflow
    // particles for that tick are simply dropped, not queued.
    public boolean enableParticleBudget = true;
    /** Max new particles allowed to spawn per client tick. */
    public int maxNewParticlesPerTick = 200;

    // --- Low-end auto-tune (client, applied once on startup) ---
    // A few vanilla visual options are meaningfully expensive and safe to
    // default lower on a machine that's clearly constrained — this just
    // sets them programmatically instead of you finding them in the menu.
    public boolean enableLowEndAutoTune = true;
    /** If max JVM heap is below this many MB, treat the system as memory-constrained. */
    public long autoTuneMaxMemoryMb = 3000;
    /** If available CPU cores is below this, treat the system as CPU-constrained. */
    public int autoTuneMinCores = 4;

    // --- Distant entity animation throttle (client) ---
    // Visible entities beyond this distance still render, but their
    // skeletal animation (limb swing, head rotation, wing flapping) is
    // updated less frequently — holding the last pose for a few frames
    // instead of recalculating every frame. Imperceptible at distance,
    // meaningful CPU savings in areas with many visible mobs.
    public boolean enableEntityAnimationThrottle = true;
    /** Beyond this many blocks, throttle animation updates. */
    public double entityAnimThrottleDistance = 48.0;
    /** Only recalculate animation every Nth frame for distant entities. */
    public int entityAnimThrottleInterval = 4;

    // --- Item frame render throttle (client) ---
    // Item frames (especially those holding maps) are surprisingly
    // expensive to render — each one is a full entity render with its own
    // item model or map texture. Storage rooms and trading halls with
    // hundreds of frames cause major frametime spikes. This skips
    // rendering item frame contents entirely beyond a configurable
    // distance.
    public boolean enableItemFrameThrottle = true;
    /** Beyond this many blocks, skip rendering item frame contents. */
    public double itemFrameRenderDistance = 32.0;
    /** Beyond that distance, only re-extract render state (item/map contents) every Nth frame — never fully stops, so state is never left permanently stale. */
    public int itemFrameThrottleInterval = 20;

    // --- Beacon beam & portal particle throttle (client) ---
    // Beacon beams are tall, multi-layered animated vertex geometry
    // rendered every frame regardless of distance. Nether portal blocks
    // continuously spawn dense ambient particles. Both are purely
    // cosmetic at distance and safe to cull.
    public boolean enableBeaconThrottle = true;
    /** Beyond this many blocks, skip rendering beacon beams. */
    public double beaconThrottleDistance = 96.0;
    /** Beyond this many blocks, skip nether portal ambient particles. */
    public boolean enablePortalParticleThrottle = true;
    public double portalParticleThrottleDistance = 32.0;

    // --- Elytra / fast-travel chunk generation governor (client) ---
    // Flying fast with an Elytra or Riptide trident causes heavy CPU
    // thread congestion as the server/client spam chunk loading requests.
    // This temporarily lowers simulation distance while traveling above
    // a speed threshold, reducing tick-side load and prioritizing smooth
    // frame delivery over maximum loaded area. Same proven mechanism as
    // PerformanceScaler's adaptive simulation distance, just triggered
    // by speed instead of FPS.
    public boolean enableChunkGenGovernor = true;
    /** Speed in blocks/tick above which the governor activates. */
    public double chunkGenSpeedThreshold = 1.2;
    /** How many chunks to reduce simulation distance by while fast-traveling. */
    public int chunkGenSimDistReduction = 4;
    /** How many ticks after slowing down before restoring simulation distance. */
    public int chunkGenRestoreCooldownTicks = 100;

    // --- Lightmap texture refresh throttle: DISABLED ---
    // See disabled-mixins/README.md — the mixin implementing this doesn't
    // currently compile (targets a class that appears to have been
    // restructured in 26.1's lighting rewrite) and has been pulled out of
    // the build. These fields are intentionally removed rather than left
    // as dead config, since a toggle that silently does nothing is worse
    // than no toggle. Re-add here once the mixin is fixed and re-enabled.

    // --- Distance-based sound pre-cull (client) ---
    // Complements the existing per-tick sound budget. Instead of capping
    // count, this drops sounds whose distance-attenuated volume falls
    // below a threshold BEFORE they enter the OpenAL pipeline. Dense
    // farms with hundreds of quiet distant mob sounds never even reach
    // the audio engine.
    public boolean enableSoundPreCull = true;
    /** Sounds whose estimated volume (after distance attenuation) is below
     *  this fraction (0.0–1.0) are dropped before reaching OpenAL. */
    public double soundPreCullVolumeThreshold = 0.05;

    // --- Server tick-health diagnostics (server/common) ---
    // Pure observability, no behavior change: periodically logs a warning
    // if average server tick time creeps above a threshold, so slowdowns
    // show up in the log instead of only being felt as vague lag. Added
    // by ServerTickDiagnostics.java, which was present in an earlier
    // upload but never wired up to config fields — this was a genuine
    // compile error (referenced fields that didn't exist) until now.
    public boolean enableTickDiagnostics = true;
    /** How many server ticks to average over before checking/logging. */
    public int tickDiagnosticsIntervalTicks = 200; // 10s
    /** Log a warning if the average tick time (ms) is at or above this. Vanilla's budget per tick is 50ms. */
    public double tickDiagnosticsWarnMs = 55.0;

    // --- Sub-vanilla simulation distance override (server/common): EXPERIMENTAL ---
    // See SimDistanceOverride.java for the full explanation — this is the
    // The server-side target is stable in the 26.2 API, so this is enabled by
    // default and the effective server simulation distance can reach 1 chunk.
    // Writes directly to ServerChunkCache rather than fighting the client-side
    // Options slider's 5-32 validation range, so it works independently
    // of (and shouldn't be combined with) enableAdaptiveSimulationDistance,
    // enableChunkGenGovernor, and enableSimDistanceCap above, which all
    // operate through that Options value instead.
    public boolean enableSimDistanceOverride = true;
    /** Target simulation distance, allowed below vanilla's normal minimum of 5. */
    public int overrideSimulationDistance = 1;

    public static ChlorineConfig load() {
        try {
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                    ChlorineConfig cfg = GSON.fromJson(reader, ChlorineConfig.class);
                    if (cfg != null) {
                        cfg.save(); // re-write to pick up any new fields with their defaults
                        return cfg;
                    }
                }
            }
        } catch (IOException e) {
            Chlorine.LOGGER.warn("Failed to read chlorine.json, regenerating defaults", e);
        }

        ChlorineConfig fresh = new ChlorineConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Chlorine.LOGGER.warn("Failed to write chlorine.json", e);
        }
    }
}
