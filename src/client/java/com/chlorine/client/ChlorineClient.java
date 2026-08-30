package com.chlorine.client;

import com.chlorine.Chlorine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ChlorineClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PerformanceScaler scaler = new PerformanceScaler();
        PowerSaver powerSaver = new PowerSaver();
        ChunkGenGovernor governor = new ChunkGenGovernor();
        SimDistanceCap cap = new SimDistanceCap();
        // Wrapped in an array since it needs to be mutated from inside the
        // tick lambda below (effectively-final capture won't allow a plain
        // boolean local here).
        boolean[] autoTuneApplied = {false};

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!autoTuneApplied[0]) {
                LowEndAutoTune.applyIfNeeded(client);
                autoTuneApplied[0] = true;
            }
            if (Chlorine.CONFIG.enableAdaptiveSimulationDistance) {
                scaler.tick(client);
            }
            if (Chlorine.CONFIG.enablePowerSaver) {
                powerSaver.tick(client);
            }
            if (Chlorine.CONFIG.enableChunkGenGovernor) {
                governor.tick(client);
            }
            // Runs last so it always gets the final say, regardless of
            // what either system above just tried to set.
            cap.tick(client);
            // NOTE: SoundBudgetMixin/ParticleBudgetMixin used to be reset
            // from here via a public static method each of them exposed.
            // That crashed the game outright — Mixin doesn't allow a
            // mixin class to add a new non-private static method to its
            // target, since that's effectively grafting a new public API
            // onto a vanilla class. Both mixins now reset themselves
            // internally on a rolling ~50ms real-time window instead, so
            // no external call is needed here anymore.
        });

        Chlorine.LOGGER.info("Chlorine client-side laptop tuning active");
    }
}
