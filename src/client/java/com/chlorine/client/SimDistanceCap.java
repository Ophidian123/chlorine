package com.chlorine.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Enforces a hard ceiling on simulation distance, independent of
 * PerformanceScaler (FPS-based) and ChunkGenGovernor (speed-based).
 *
 * Neither of those can truly know a value like 12 chunks is unsustainable
 * for a given CPU — they only know FPS happened to read as "good enough"
 * in the specific instant they checked before raising. On some hardware,
 * FPS can look fine for the brief window right after a raise and then
 * settle lower once more chunks actually start ticking, which the
 * reactive raise logic has no way to anticipate. This is a simple,
 * always-correct backstop instead: whatever any other system sets
 * simulation distance to, this brings it back down to the configured
 * maximum on the very next tick, no exceptions.
 *
 * Runs after PerformanceScaler and ChunkGenGovernor in ChlorineClient's
 * tick order specifically so it always gets the final say.
 */
public class SimDistanceCap {
    public void tick(Minecraft client) {
        if (!Chlorine.CONFIG.enableSimDistanceCap) {
            return;
        }
        if (client.level == null) {
            return;
        }

        OptionInstance<Integer> simOption = client.options.simulationDistance();
        int current = simOption.get();
        // Never let the cap be set below the adaptive scaler's own floor
        // — that would just make the two fight each other every tick.
        int max = Math.max(Chlorine.CONFIG.minSimulationDistance, Chlorine.CONFIG.maxSimulationDistance);

        if (current > max) {
            simOption.set(max);
            Chlorine.LOGGER.debug("Simulation distance {} exceeded configured cap, lowering to {}", current, max);
        }
    }
}
