package com.chlorine.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Temporarily reduces simulation distance while the player is traveling at
 * high speed (Elytra, Riptide trident, fast minecarts, etc.).
 *
 * Flying fast causes heavy CPU thread congestion as the server/client spam
 * chunk loading and generation requests, leading to frametime spikes and
 * freezes — especially on laptops with slower storage. This detects high
 * horizontal speed and lowers simulation distance by a configurable
 * amount, reducing tick-side load and prioritising smooth frame delivery
 * over maximum loaded area. When the player slows down, the original
 * simulation distance is restored after a cooldown.
 *
 * Uses the same simulation-distance adjustment mechanism as
 * PerformanceScaler (no render-graph rebuild cost), triggered by player
 * speed instead of FPS. The two share a common notion of "the user's
 * real original simulation distance" via SimDistanceBaseline.java rather
 * than each independently snapshotting "current value" as their own
 * "original" — without that, whichever one acted second would capture
 * the other's already-lowered value as its baseline, undershooting the
 * true original on restore. If PerformanceScaler has already lowered
 * simulation distance due to low FPS, this governor won't lower it
 * further below the configured minSimulationDistance floor.
 */
public class ChunkGenGovernor {
    private double prevX = Double.NaN;
    private double prevZ = Double.NaN;
    private int originalSimDistance = -1;
    private boolean governing = false;
    private int ticksSinceSlow = 0;

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            prevX = Double.NaN;
            prevZ = Double.NaN;
            return;
        }

        double x = client.player.getX();
        double z = client.player.getZ();

        if (Double.isNaN(prevX)) {
            prevX = x;
            prevZ = z;
            return;
        }

        double dx = x - prevX;
        double dz = z - prevZ;
        double speed = Math.sqrt(dx * dx + dz * dz); // blocks per tick
        prevX = x;
        prevZ = z;

        boolean fast = speed >= Chlorine.CONFIG.chunkGenSpeedThreshold;

        OptionInstance<Integer> simOption = client.options.simulationDistance();
        int simCurrent = simOption.get();

        if (fast && !governing) {
            // Activate: capture the shared baseline and lower
            originalSimDistance = SimDistanceBaseline.getOrCapture(simCurrent);
            int reduced = Math.max(
                Chlorine.CONFIG.minSimulationDistance,
                simCurrent - Math.max(1, Chlorine.CONFIG.chunkGenSimDistReduction)
            );
            if (reduced < simCurrent) {
                simOption.set(reduced);
                Chlorine.LOGGER.debug(
                    "Fast travel detected ({} blocks/tick), lowering simulation distance {} -> {}",
                    String.format("%.1f", speed), simCurrent, reduced
                );
            }
            governing = true;
            ticksSinceSlow = 0;
        } else if (!fast && governing) {
            ticksSinceSlow++;
            if (ticksSinceSlow >= Chlorine.CONFIG.chunkGenRestoreCooldownTicks) {
                // Restore to the shared baseline (not just whatever this
                // class captured — PerformanceScaler may have adjusted
                // things further while we were governing), clamped to the
                // hard cap if one's enabled — see SimDistanceCap.java.
                int baseline = SimDistanceBaseline.getOrCapture(simCurrent);
                int target = Chlorine.CONFIG.enableSimDistanceCap
                        ? Math.min(baseline, Chlorine.CONFIG.maxSimulationDistance)
                        : baseline;
                if (simCurrent < target) {
                    simOption.set(target);
                    SimDistanceBaseline.clearIfAtOrAboveBaseline(target);
                    Chlorine.LOGGER.debug(
                        "Speed dropped, restoring simulation distance -> {}",
                        target
                    );
                }
                governing = false;
                originalSimDistance = -1;
            }
        } else if (fast) {
            // Still fast — keep the counter reset
            ticksSinceSlow = 0;
        }
    }
}
