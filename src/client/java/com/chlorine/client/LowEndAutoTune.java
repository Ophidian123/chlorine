package com.chlorine.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;

/**
 * Runs once, on the first client tick, and nudges a handful of vanilla
 * visual options down if the system looks memory- or CPU-constrained.
 * Every option touched here is a normal, supported Options field — this
 * is just setting them programmatically instead of you finding them in
 * Video Settings yourself.
 *
 * === RISK NOTE ===
 * `graphicsMode()` (GraphicsStatus: FAST/FANCY/FABULOUS) and
 * `ambientOcclusion()` are the two least certain accessors here —
 * ambientOcclusion in particular may turn out to be an enum
 * (AmbientOcclusionStatus: OFF/MIN/MAX) rather than a boolean in 26.2. If
 * either fails to compile, open net.minecraft.client.Options in your IDE
 * and fix the accessor/type to match.
 */
public final class LowEndAutoTune {
    private LowEndAutoTune() {
    }

    public static void applyIfNeeded(Minecraft client) {
        if (!Chlorine.CONFIG.enableLowEndAutoTune) {
            return;
        }

        long maxMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        int cores = Runtime.getRuntime().availableProcessors();

        boolean lowMemory = maxMemMb < Chlorine.CONFIG.autoTuneMaxMemoryMb;
        boolean lowCoreCount = cores < Chlorine.CONFIG.autoTuneMinCores;

        if (!lowMemory && !lowCoreCount) {
            Chlorine.LOGGER.info(
                "System looks fine ({} MB heap, {} cores) — skipping low-end auto-tune",
                maxMemMb, cores
            );
            return;
        }

        Chlorine.LOGGER.info(
            "Detected constrained system ({} MB heap, {} cores) — applying low-end auto-tune",
            maxMemMb, cores
        );

        client.options.entityShadows().set(false);
        client.options.biomeBlendRadius().set(0);
        client.options.mipmapLevels().set(0);
        client.options.particles().set(ParticleStatus.MINIMAL);
        client.options.graphicsMode().set(net.minecraft.client.GraphicsStatus.FAST);
        client.options.ambientOcclusion().set(false);
        client.options.save();
    }
}
