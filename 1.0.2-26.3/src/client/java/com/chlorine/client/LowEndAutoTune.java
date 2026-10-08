package com.chlorine.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Runs once, on the first client tick, and nudges a handful of vanilla
 * visual options down if the system looks memory- or CPU-constrained.
 * Every option touched here is a normal, supported Options field — this
 * is just setting them programmatically instead of you finding them in
 * Video Settings yourself.
 *
 * Particles are set via OptionsCompat's reflection-based enum lookup
 * rather than importing the particle status enum directly, since its
 * exact package moved in 26.1/26.2 and couldn't be confirmed from
 * outside a working build environment.
 *
 * A "graphics mode" (Fast/Fancy/Fabulous) tweak was dropped from this
 * list entirely — `Options#graphicsMode()` doesn't appear to exist
 * anymore, likely reworked as part of the 26.1/26.2 rendering pipeline
 * rewrite (Vulkan support, RenderType overhaul). Rather than guess at a
 * replacement blind, the other tweaks here still deliver real value on
 * their own.
 *
 * === RISK NOTE ===
 * `ambientOcclusion()` is the least certain accessor left — it may turn
 * out to be an enum (AmbientOcclusionStatus: OFF/MIN/MAX) rather than a
 * boolean in 26.2. If it fails to compile, open net.minecraft.client.Options
 * in your IDE and fix the accessor/type to match.
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
        client.options.ambientOcclusion().set(false);

        @SuppressWarnings({"unchecked", "rawtypes"})
        OptionInstance particleOption = client.options.particles();
        OptionsCompat.setEnumByName(particleOption, "MINIMAL");

        client.options.save();
    }
}
