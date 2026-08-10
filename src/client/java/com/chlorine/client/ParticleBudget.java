package com.chlorine.client;

import com.chlorine.Chlorine;

/** Shared client state for the number of particles created this tick. */
public final class ParticleBudget {
    private static int particlesThisTick;

    private ParticleBudget() {
    }

    public static void reset() {
        particlesThisTick = 0;
    }

    /** Returns whether another particle may be created this tick. */
    public static boolean tryStartParticle() {
        if (!Chlorine.CONFIG.enableParticleBudget) {
            return true;
        }

        int budget = Math.max(1, Chlorine.CONFIG.maxNewParticlesPerTick);
        if (particlesThisTick >= budget) {
            return false;
        }

        particlesThisTick++;
        return true;
    }
}
