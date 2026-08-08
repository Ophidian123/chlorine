package com.chlorine.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Watches a rolling FPS average and works through a small ladder of
 * cheap, non-rendering-graph options — simulation distance, then entity
 * render distance scaling, then particles — lowering one tier at a time
 * once the tier before it is maxed out, and restoring them in reverse
 * order as FPS recovers.
 *
 * This deliberately does NOT touch render distance. Both vanilla and
 * Sodium rebuild the entire chunk render graph whenever render distance
 * changes — every currently visible chunk's mesh gets discarded and
 * rebuilt, the same hitch as dragging the slider in Video Settings. There's
 * no supported way to make that incremental without reimplementing part of
 * the chunk render graph, which is squarely Sodium's territory, not
 * something safe to bolt on here.
 *
 * Tier 1 — simulation distance: server-tick-facing (entities, random
 * ticks, block updates), not tied to the render graph, so it's free to
 * change with no hitch. Usually the biggest lever on a weak laptop CPU.
 *
 * Tier 2 — entity distance scaling: a per-frame culling threshold for how
 * far away entities get drawn (0.5–5.0 multiplier). Unlike render
 * distance this isn't tied to a fixed-size grid, so changing it doesn't
 * force a rebuild — it just changes a distance check future frames use.
 *
 * Tier 3 — particles: dropped to MINIMAL as a last resort. Handled via
 * OptionsCompat's reflection-based enum lookup rather than importing the
 * particle status enum directly, since its exact package moved in
 * 26.1/26.2 and couldn't be confirmed from outside a working build.
 *
 * === RISK NOTE ===
 * Targets `options.simulationDistance()` and `options.entityDistanceScaling()`,
 * both OptionInstance<T>. If either doesn't compile against 26.2, open
 * net.minecraft.client.Options in your IDE and adjust the accessor name.
 */
public class PerformanceScaler {
    private final Deque<Integer> fpsSamples = new ArrayDeque<>();
    private int ticksSinceLower = 0;
    private int ticksSinceRaise = 0;
    private int originalSimulationDistance = -1;
    private double originalEntityDistanceScaling = -1;
    private Object originalParticleStatus = null;

    public void tick(Minecraft client) {
        if (client.level == null) {
            return; // not in a world — nothing to scale
        }

        int fps = client.getFps();
        fpsSamples.addLast(fps);

        int window = Math.max(20, Chlorine.CONFIG.fpsCheckWindowTicks);
        while (fpsSamples.size() > window) {
            fpsSamples.removeFirst();
        }

        ticksSinceLower++;
        ticksSinceRaise++;
        if (fpsSamples.size() < window) {
            return; // still gathering samples
        }

        double avg = fpsSamples.stream().mapToInt(Integer::intValue).average().orElse(fps);

        OptionInstance<Integer> simOption = client.options.simulationDistance();
        int simCurrent = simOption.get();
        if (originalSimulationDistance < 0) {
            originalSimulationDistance = simCurrent;
        }
        boolean simAtFloor = simCurrent <= Chlorine.CONFIG.minSimulationDistance;

        OptionInstance<Double> entityOption = Chlorine.CONFIG.enableEntityDistanceScaling
                ? client.options.entityDistanceScaling() : null;
        double entityCurrent = entityOption != null ? entityOption.get() : -1;
        if (entityOption != null && originalEntityDistanceScaling < 0) {
            originalEntityDistanceScaling = entityCurrent;
        }
        boolean entityAtFloor = entityOption == null
                || entityCurrent <= Chlorine.CONFIG.minEntityDistanceScaling;

        boolean lowFps = avg < Chlorine.CONFIG.lowFpsThreshold;
        boolean goodFps = avg > Chlorine.CONFIG.targetFps;

        if (lowFps && ticksSinceLower >= Chlorine.CONFIG.lowerCooldownTicks) {
            if (!simAtFloor) {
                int next = Math.max(Chlorine.CONFIG.minSimulationDistance, simCurrent - Math.max(1, Chlorine.CONFIG.simulationDistanceStep));
                simOption.set(next);
                ticksSinceLower = 0;
                fpsSamples.clear();
                Chlorine.LOGGER.debug("FPS averaging {}, lowering simulation distance {} -> {}", avg, simCurrent, next);
            } else if (entityOption != null && !entityAtFloor) {
                double next = Math.max(Chlorine.CONFIG.minEntityDistanceScaling, entityCurrent - Chlorine.CONFIG.entityDistanceScalingStep);
                entityOption.set(next);
                ticksSinceLower = 0;
                fpsSamples.clear();
                Chlorine.LOGGER.debug("FPS averaging {}, lowering entity distance scaling {} -> {}", avg, entityCurrent, next);
            } else if (Chlorine.CONFIG.enableParticleScaling) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                OptionInstance particleOption = client.options.particles();
                if (originalParticleStatus == null) {
                    originalParticleStatus = particleOption.get();
                }
                if (!"MINIMAL".equals(String.valueOf(particleOption.get()))
                        && OptionsCompat.setEnumByName(particleOption, "MINIMAL")) {
                    ticksSinceLower = 0;
                    Chlorine.LOGGER.debug("Still low on FPS with sim/entity distance at floor, dropping particles to MINIMAL");
                }
            }
        } else if (goodFps && ticksSinceRaise >= Chlorine.CONFIG.raiseCooldownTicks) {
            // Restore in reverse order: particles, then entity distance, then simulation distance.
            if (originalParticleStatus != null) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                OptionInstance particleOption = client.options.particles();
                particleOption.set(originalParticleStatus);
                originalParticleStatus = null;
                ticksSinceRaise = 0;
                Chlorine.LOGGER.debug("FPS recovered, restoring original particle setting");
            } else if (entityOption != null && entityCurrent < originalEntityDistanceScaling) {
                double next = Math.min(originalEntityDistanceScaling, entityCurrent + Chlorine.CONFIG.entityDistanceScalingStep);
                entityOption.set(next);
                ticksSinceRaise = 0;
                fpsSamples.clear();
                Chlorine.LOGGER.debug("FPS averaging {}, raising entity distance scaling {} -> {}", avg, entityCurrent, next);
            } else if (simCurrent < originalSimulationDistance) {
                int next = Math.min(originalSimulationDistance, simCurrent + Math.max(1, Chlorine.CONFIG.simulationDistanceStep));
                simOption.set(next);
                ticksSinceRaise = 0;
                fpsSamples.clear();
                Chlorine.LOGGER.debug("FPS averaging {}, raising simulation distance {} -> {}", avg, simCurrent, next);
            }
        }
    }
}
