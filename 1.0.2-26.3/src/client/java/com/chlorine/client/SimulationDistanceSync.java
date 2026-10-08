package com.chlorine.client;

public final class SimulationDistanceSync {
    private static volatile int currentValue = -1;

    private SimulationDistanceSync() {
    }

    public static void publish(int value) {
        currentValue = value;
    }

    public static int getOr(int fallback) {
        int value = currentValue;
        return value > 0 ? value : fallback;
    }
}
