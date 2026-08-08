package com.chlorine;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chlorine — a laptop-friendly performance mod for Fabric 26.2.
 *
 * Chlorine is designed to sit *alongside* the usual performance stack
 * (Sodium, Lithium, FerriteCore, EntityCulling, ImmediatelyFast, Iris)
 * rather than compete with it. It doesn't touch chunk rendering, memory
 * layout, or immediate-mode batching — those are already handled well by
 * that stack. Instead it targets things that stack tends not to cover:
 *
 *   - Adaptive render/simulation distance that reacts to live FPS
 *     (client-side, see ChlorineClient / PerformanceScaler)
 *   - Reduced GPU/CPU load and battery drain while the window is
 *     unfocused — a distinctly "laptop" problem (see PowerSaver)
 *   - Throttled AI goal-selector ticking for mobs far from any player
 *     (see MobAiThrottleMixin), similar in spirit to what Lithium does
 *     for tick logic, scoped narrowly so it won't fight with it
 *
 * None of this requires Sodium/Iris/etc. to be installed — Chlorine works
 * standalone too — but it's written to complement them, not replace them.
 */
public class Chlorine implements ModInitializer {
    public static final String MOD_ID = "chlorine";
    public static final Logger LOGGER = LoggerFactory.getLogger("Chlorine");

    public static ChlorineConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = ChlorineConfig.load();
        ItemMerger.register();
        XpOrbMerger.register();

        LOGGER.info("Chlorine loaded — laptop performance tuning alongside Sodium & friends");
        if (CONFIG.enableDistantMobAiThrottle) {
            LOGGER.info(
                "Distant mob AI throttling enabled (radius={} blocks, interval={} ticks)",
                CONFIG.aiActiveRadius,
                CONFIG.aiThrottleInterval
            );
        }
    }
}
