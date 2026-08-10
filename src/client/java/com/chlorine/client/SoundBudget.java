package com.chlorine.client;

import com.chlorine.Chlorine;

/** Shared client state for the number of sounds started this tick. */
public final class SoundBudget {
    private static int soundsThisTick;

    private SoundBudget() {
    }

    public static void reset() {
        soundsThisTick = 0;
    }

    /** Returns whether another sound may be started this tick. */
    public static boolean tryStartSound() {
        if (!Chlorine.CONFIG.enableSoundBudget) {
            return true;
        }

        int budget = Math.max(1, Chlorine.CONFIG.maxNewSoundsPerTick);
        if (soundsThisTick >= budget) {
            return false;
        }

        soundsThisTick++;
        return true;
    }
}
