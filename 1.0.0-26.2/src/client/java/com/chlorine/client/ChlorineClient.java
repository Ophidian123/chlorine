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
            // Reset at the end of each tick so the next tick's sound and
            // particle budgets start clean.
            ClientEffectBudgets.reset();
        });

        Chlorine.LOGGER.info("Chlorine client-side laptop tuning active");
    }
}
