package com.chlorine.client;

/**
 * Shared "what was the user's real simulation distance before any Chlorine
 * system touched it" baseline, used by both PerformanceScaler (FPS-based
 * scaling) and ChunkGenGovernor (speed-based scaling) since they both
 * adjust the same underlying option independently of each other.
 *
 * Without this, each system captured "current value" as its own notion of
 * "original" the first time it acted — if the other system had already
 * lowered simulation distance first, that already-lowered value got
 * captured as the "original" to restore to later, instead of the user's
 * real setting. Sharing one baseline, captured once by whichever system
 * needs it first and cleared once simulation distance is back to normal,
 * fixes that without needing the two systems to know about each other's
 * internal state machines.
 */
final class SimDistanceBaseline {
    private static final int MIN_OPTION_VALUE = 1;
    private static int value = -1;
    private static int lastManagedValue = -1;

    private SimDistanceBaseline() {
    }

    /** Returns the shared baseline, capturing it from currentValue if one isn't already set. */
    static int getOrCapture(int currentValue) {
        if (value < 0) {
            value = currentValue;
        }
        if (lastManagedValue < 0) {
            lastManagedValue = currentValue;
        }
        return value;
    }

    /**
     * Treat option changes not made by Chlorine as the user's new preferred
     * value, so an old startup setting is never restored over a manual edit.
     */
    static int observeCurrentValue(int currentValue) {
        if (lastManagedValue >= 0 && currentValue != lastManagedValue) {
            value = currentValue;
        }
        lastManagedValue = currentValue;
        return getOrCapture(currentValue);
    }

    static void recordManagedValue(int currentValue) {
        lastManagedValue = currentValue;
    }

    /** Call once a system has fully restored simulation distance back to (or above) the baseline, so a fresh baseline gets captured next time — e.g. if the user manually changes the setting later. */
    static void clearIfAtOrAboveBaseline(int currentValue) {
        if (value >= 0 && currentValue >= value) {
            value = -1;
        }
        lastManagedValue = currentValue;
    }

    static int optionFloor(int configuredMinimum) {
        return Math.max(MIN_OPTION_VALUE, configuredMinimum);
    }
}
