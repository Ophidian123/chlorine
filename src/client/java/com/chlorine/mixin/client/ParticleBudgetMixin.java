package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Caps how many new particles can spawn within the same client tick.
 *
 * Same reasoning as SoundBudgetMixin: not distance-based (vanilla already
 * skips particles outside relevant ranges), this targets bursts —
 * fireworks, a big potion cloud, a large explosion — that can spike frame
 * time all at once regardless of any individual particle's cost.
 * Overflow particles for that tick are simply dropped, not queued.
 *
 * The budget resets on its own every ~50ms (one tick's worth of real
 * time) — see the self-contained windowing logic below. An earlier
 * version used a public static reset method called externally from
 * ChlorineClient's tick hook, which Mixin rejects outright: a mixin
 * class may not add a new non-private static method to its target,
 * since that would silently graft a whole new externally-callable API
 * onto a vanilla class. This crashed the game on startup
 * ("InvalidMixinException: ... contains non-private static method") —
 * same bug as SoundBudgetMixin had, fixed the same way.
 *
 * === RISK NOTE ===
 * Targets `net.minecraft.client.particle.ParticleEngine#add(Particle)`.
 * This has been the particle engine's core "register this particle for
 * rendering" entry point for a long time (both direct adds and the
 * ParticleOptions-based creation path funnel through it), so it's a
 * lower-risk guess than most of this project — but still unverified
 * against 26.2. If Mixin fails to apply this at startup, open
 * net.minecraft.client.particle.ParticleEngine in your IDE after a
 * Gradle sync and confirm the method name/signature.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleBudgetMixin {
    private static int chlorine$particlesThisWindow = 0;
    private static long chlorine$windowStartMs = 0L;

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void chlorine$limitParticleBudget(Particle particle, CallbackInfo ci) {
        if (!Chlorine.CONFIG.enableParticleBudget) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - chlorine$windowStartMs >= 50) {
            chlorine$windowStartMs = now;
            chlorine$particlesThisWindow = 0;
        }

        int budget = Math.max(1, Chlorine.CONFIG.maxNewParticlesPerTick);
        if (chlorine$particlesThisWindow >= budget) {
            ci.cancel();
            return;
        }

        chlorine$particlesThisWindow++;
    }
}
