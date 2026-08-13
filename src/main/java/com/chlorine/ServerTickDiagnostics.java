package com.chlorine;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/** Optional, low-overhead server tick-health reporting. */
public final class ServerTickDiagnostics {
    private long tickStartedAt;
    private long accumulatedNanos;
    private int samples;

    private ServerTickDiagnostics() {
    }

    public static void register() {
        ServerTickDiagnostics diagnostics = new ServerTickDiagnostics();
        ServerTickEvents.START_SERVER_TICK.register(diagnostics::onTickStart);
        ServerTickEvents.END_SERVER_TICK.register(diagnostics::onTickEnd);
    }

    private void onTickStart(MinecraftServer server) {
        tickStartedAt = System.nanoTime();
    }

    private void onTickEnd(MinecraftServer server) {
        if (tickStartedAt == 0L) {
            return;
        }
        accumulatedNanos += System.nanoTime() - tickStartedAt;
        samples++;

        int interval = Math.max(20, Chlorine.CONFIG.tickDiagnosticsIntervalTicks);
        if (samples < interval) {
            return;
        }
        double averageMs = accumulatedNanos / 1_000_000.0 / samples;
        accumulatedNanos = 0L;
        samples = 0;

        if (Chlorine.CONFIG.enableTickDiagnostics
                && averageMs >= Chlorine.CONFIG.tickDiagnosticsWarnMs) {
            Chlorine.LOGGER.warn(
                "Chlorine: server work averaged {} ms/tick over the last {} ticks (target: below 50 ms)",
                String.format(java.util.Locale.ROOT, "%.1f", averageMs), interval
            );
        }
    }
}
