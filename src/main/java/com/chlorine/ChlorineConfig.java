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
    public int minSimulationDistance = 4;
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
    public boolean enableDistantMobAiThrottle = true;
    /** Mobs within this many blocks of any player always tick AI normally. */
    public double aiActiveRadius = 48.0;
    /** Beyond that radius, only run the AI goal selector every Nth tick. */
    public int aiThrottleInterval = 8;

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

    // --- Low-end auto-tune (client, applied once on startup) ---
    // A few vanilla visual options are meaningfully expensive and safe to
    // default lower on a machine that's clearly constrained — this just
    // sets them programmatically instead of you finding them in the menu.
    public boolean enableLowEndAutoTune = true;
    /** If max JVM heap is below this many MB, treat the system as memory-constrained. */
    public long autoTuneMaxMemoryMb = 3000;
    /** If available CPU cores is below this, treat the system as CPU-constrained. */
    public int autoTuneMinCores = 4;

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
