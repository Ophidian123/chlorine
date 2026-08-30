package com.chlorine;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;

/**
 * Overrides the server's simulation-distance radius below vanilla's normal
 * minimum of 5 chunks.
 *
 * The client option remains validated at 5 or higher, so this uses the
 * server-side distance manager instead. ServerChunkCache exposes the
 * setSimulationDistance method that updates the ticket level used to decide
 * which chunks are ticking. This works in both singleplayer and dedicated
 * servers without reflection or changes to the client-facing slider.
 *
 * Re-applied every couple of seconds because vanilla may re-sync the server
 * value from the client's validated setting.
 */
public final class SimDistanceOverride {
    private int ticksUntilNextPass;

    private SimDistanceOverride() {
    }

    public static void register() {
        SimDistanceOverride override = new SimDistanceOverride();
        ServerTickEvents.END_SERVER_TICK.register(override::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        if (!Chlorine.CONFIG.enableSimDistanceOverride) {
            return;
        }
        if (--ticksUntilNextPass > 0) {
            return;
        }
        ticksUntilNextPass = 40;

        int target = Math.max(1, Chlorine.CONFIG.overrideSimulationDistance);
        for (ServerLevel level : server.getAllLevels()) {
            applyOverride(level.getChunkSource(), target);
        }
    }

    private void applyOverride(ServerChunkCache chunkSource, int target) {
        chunkSource.setSimulationDistance(target);
    }
}
