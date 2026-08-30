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
    private static int value = -1;

    private SimDistanceBaseline() {
    }

    /** Returns the shared baseline, capturing it from currentValue if one isn't already set. */
    static int getOrCapture(int currentValue) {
        if (value < 0) {
            value = currentValue;
        }
        return value;
    }

    /** Call once a system has fully restored simulation distance back to (or above) the baseline, so a fresh baseline gets captured next time — e.g. if the user manually changes the setting later. */
    static void clearIfAtOrAboveBaseline(int currentValue) {
        if (value >= 0 && currentValue >= value) {
            value = -1;
        }
    }
}
