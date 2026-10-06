package com.chlorine.client;

public final class ClientEffectBudgets {
    private static int soundsThisTick;
    private static int particlesThisTick;

    private ClientEffectBudgets() {
    }

    public static void reset() {
        soundsThisTick = 0;
        particlesThisTick = 0;
    }

    public static boolean tryConsumeSound(int limit) {
        if (soundsThisTick >= Math.max(1, limit)) {
            return false;
        }
        soundsThisTick++;
        return true;
    }

    public static boolean tryConsumeParticle(int limit) {
        if (particlesThisTick >= Math.max(1, limit)) {
            return false;
        }
        particlesThisTick++;
        return true;
    }
}
