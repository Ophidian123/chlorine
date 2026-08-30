package com.chlorine;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * EXPERIMENTAL. Overrides the server's actual simulation-distance radius
 * below vanilla's own minimum of 5 chunks.
 *
 * Vanilla's Video Settings slider enforces a 5-32 range at the Options/GUI
 * layer — that's client-side validation, not a hard engine limit. Rather
 * than fight that layer (which would mean guessing at Options' internal
 * construction code, exactly the GUI-internals territory hit hardest by
 * 26.1/26.2's reorganization — the least reliable place to guess in this
 * whole project), this writes directly to ChunkMap, the object on each
 * ServerLevel that actually decides which chunks get promoted to
 * "ticking" status. That's a server-side concept, so this works
 * identically in singleplayer (an embedded MinecraftServer) and on a
 * dedicated server — it never touches the client-facing slider at all,
 * which is why this has its own separate config value rather than trying
 * to unlock vanilla's slider to go lower.
 *
 * === HONESTY NOTE ===
 * This is the least-certain internal target in this entire project. I
 * don't have a confirmed, tested API surface for ChunkMap's internals —
 * I'm working from a reasonable guess at its structure (a
 * setSimulationDistance(int) method, or failing that a simulationDistance
 * field), not verified knowledge. Implemented defensively: tries the
 * setter first, falls back to the field, and permanently disables itself
 * for the session — logging clearly why — if neither works, rather than
 * risking a crash or silently doing nothing. Enabled by default. If it
 * disables itself, check the log for the exact reason and open
 * net.minecraft.server.level.ChunkMap in your IDE to find the real
 * mechanism.
 *
 * Re-applied every couple of seconds rather than once: vanilla may
 * periodically re-sync this value from the client's own (validated,
 * >=5) setting, which would silently revert a one-time override.
 *
 * === COMPATIBILITY NOTE ===
 * This operates on a different layer than PerformanceScaler,
 * ChunkGenGovernor, and SimDistanceCap, which all manipulate the client's
 * Options.simulationDistance() value instead. Running both at once is
 * likely to look like flickering/fighting — see the startup warning in
 * Chlorine.java's onInitialize(). If you want sub-vanilla simulation
 * distance, turn those three off instead of running them together.
 */
public final class SimDistanceOverride {
    private int ticksUntilNextPass = 0;
    private boolean disabled = false;
    private boolean lookupAttempted = false;
    private Method setterMethod;
    private Field field;

    private SimDistanceOverride() {
    }

    public static void register() {
        SimDistanceOverride override = new SimDistanceOverride();
        ServerTickEvents.END_SERVER_TICK.register(override::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        if (!Chlorine.CONFIG.enableSimDistanceOverride || disabled) {
            return;
        }
        if (--ticksUntilNextPass > 0) {
            return;
        }
        ticksUntilNextPass = 40; // re-apply every ~2 seconds in case vanilla resyncs it

        int target = Math.max(1, Chlorine.CONFIG.overrideSimulationDistance);

        for (ServerLevel level : server.getAllLevels()) {
            ChunkMap chunkMap = level.getChunkSource().chunkMap;
            if (!applyOverride(chunkMap, target)) {
                disableSelf();
                return;
            }
        }
    }

    private boolean applyOverride(ChunkMap chunkMap, int target) {
        if (!lookupAttempted) {
            lookupAttempted = true;
            try {
                setterMethod = ChunkMap.class.getDeclaredMethod("setSimulationDistance", int.class);
                setterMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                setterMethod = null;
            }
            if (setterMethod == null) {
                try {
                    field = ChunkMap.class.getDeclaredField("simulationDistance");
                    field.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    field = null;
                }
            }
        }

        try {
            if (setterMethod != null) {
                setterMethod.invoke(chunkMap, target);
                return true;
            }
            if (field != null) {
                field.setInt(chunkMap, target);
                return true;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            Chlorine.LOGGER.warn("Chlorine: failed to apply simulation distance override", e);
            return false;
        }
        return false;
    }

    private void disableSelf() {
        disabled = true;
        Chlorine.LOGGER.warn(
            "Chlorine: couldn't find a way to override simulation distance below vanilla's minimum "
            + "(tried a setSimulationDistance(int) method and a simulationDistance field on ChunkMap, "
            + "neither worked) — disabling this experimental feature for the session. Nothing else in "
            + "Chlorine is affected."
        );
    }
}
