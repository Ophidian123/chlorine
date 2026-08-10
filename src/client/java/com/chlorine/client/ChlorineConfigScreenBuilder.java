package com.chlorine.client;

import com.chlorine.Chlorine;
import com.chlorine.ChlorineConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Builds Chlorine's in-game settings screen — reachable via Mod Menu's
 * "Config" button, the same way Sodium/Sodium Extra/Iris surface theirs.
 *
 * This uses Cloth Config's fluent builder API rather than hand-rolling
 * vanilla widgets directly. Cloth Config's own API (ConfigBuilder,
 * startBooleanToggle, startIntSlider, etc.) has stayed close to identical
 * across years of Minecraft versions — that's the whole point of the
 * library — so it's a much smaller compile-risk surface than building
 * against vanilla's Screen/widget classes directly, especially given
 * 26.1/26.2 reorganized a large chunk of the Gui/Hud/Screen code.
 *
 * === RISK NOTE ===
 * The two imports most likely to need adjusting if this doesn't compile:
 * `net.minecraft.client.gui.screens.Screen` (Mod Menu's ConfigScreenFactory
 * is generic over whatever Screen class it was built against, so this
 * must match) and `net.minecraft.network.chat.Component` (for the
 * `.literal(String)` text factory). Both have been stable Mojmap
 * locations for a long time, but 26.1/26.2 specifically reorganized GUI
 * code, so double-check against your IDE if either fails.
 */
public final class ChlorineConfigScreenBuilder {
    private ChlorineConfigScreenBuilder() {
    }

    public static Screen build(Screen parent) {
        ChlorineConfig cfg = Chlorine.CONFIG;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Chlorine"))
                .setSavingRunnable(cfg::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory scaler = builder.getOrCreateCategory(Component.literal("Adaptive Scaler"));
        scaler.addEntry(eb.startBooleanToggle(Component.literal("Enable adaptive simulation distance"), cfg.enableAdaptiveSimulationDistance)
                .setTooltip(Component.literal("Lowers/raises simulation distance based on FPS. No render-graph rebuild, unlike render distance."))
                .setSaveConsumer(v -> cfg.enableAdaptiveSimulationDistance = v)
                .build());
        scaler.addEntry(eb.startIntSlider(Component.literal("Min simulation distance"), cfg.minSimulationDistance, 1, 32)
                .setSaveConsumer(v -> cfg.minSimulationDistance = v)
                .build());
        scaler.addEntry(eb.startIntSlider(Component.literal("Low FPS threshold"), cfg.lowFpsThreshold, 5, 200)
                .setSaveConsumer(v -> cfg.lowFpsThreshold = v)
                .build());
        scaler.addEntry(eb.startIntSlider(Component.literal("Target FPS"), cfg.targetFps, 5, 500)
                .setSaveConsumer(v -> cfg.targetFps = v)
                .build());
        scaler.addEntry(eb.startIntField(Component.literal("FPS averaging window (ticks)"), cfg.fpsCheckWindowTicks)
                .setMin(20)
                .setSaveConsumer(v -> cfg.fpsCheckWindowTicks = v)
                .build());
        scaler.addEntry(eb.startIntSlider(Component.literal("Simulation distance step"), cfg.simulationDistanceStep, 1, 8)
                .setSaveConsumer(v -> cfg.simulationDistanceStep = v)
                .build());
        scaler.addEntry(eb.startIntField(Component.literal("Lower cooldown (ticks)"), cfg.lowerCooldownTicks)
                .setMin(1)
                .setSaveConsumer(v -> cfg.lowerCooldownTicks = v)
                .build());
        scaler.addEntry(eb.startIntField(Component.literal("Raise cooldown (ticks)"), cfg.raiseCooldownTicks)
                .setMin(1)
                .setSaveConsumer(v -> cfg.raiseCooldownTicks = v)
                .build());
        scaler.addEntry(eb.startBooleanToggle(Component.literal("Also scale entity distance"), cfg.enableEntityDistanceScaling)
                .setSaveConsumer(v -> cfg.enableEntityDistanceScaling = v)
                .build());
        scaler.addEntry(eb.startDoubleField(Component.literal("Min entity distance scaling"), cfg.minEntityDistanceScaling)
                .setMin(0.1)
                .setMax(5.0)
                .setSaveConsumer(v -> cfg.minEntityDistanceScaling = v)
                .build());
        scaler.addEntry(eb.startDoubleField(Component.literal("Entity distance scaling step"), cfg.entityDistanceScalingStep)
                .setMin(0.05)
                .setMax(1.0)
                .setSaveConsumer(v -> cfg.entityDistanceScalingStep = v)
                .build());
        scaler.addEntry(eb.startBooleanToggle(Component.literal("Drop particles as last resort"), cfg.enableParticleScaling)
                .setSaveConsumer(v -> cfg.enableParticleScaling = v)
                .build());

        ConfigCategory power = builder.getOrCreateCategory(Component.literal("Power Saver"));
        power.addEntry(eb.startBooleanToggle(Component.literal("Enable power saver"), cfg.enablePowerSaver)
                .setTooltip(Component.literal("Caps framerate (and optionally hides clouds) while the window is unfocused."))
                .setSaveConsumer(v -> cfg.enablePowerSaver = v)
                .build());
        power.addEntry(eb.startIntSlider(Component.literal("Unfocused framerate limit"), cfg.unfocusedFramerateLimit, 5, 60)
                .setSaveConsumer(v -> cfg.unfocusedFramerateLimit = v)
                .build());
        power.addEntry(eb.startBooleanToggle(Component.literal("Hide clouds when unfocused"), cfg.hideCloudsWhenUnfocused)
                .setSaveConsumer(v -> cfg.hideCloudsWhenUnfocused = v)
                .build());

        ConfigCategory ai = builder.getOrCreateCategory(Component.literal("Mob AI Throttle"));
        ai.addEntry(eb.startBooleanToggle(Component.literal("Enable distant mob AI throttle"), cfg.enableDistantMobAiThrottle)
                .setTooltip(Component.literal("Skips goal-selector AI ticking (not physics) for mobs with no player nearby."))
                .setSaveConsumer(v -> cfg.enableDistantMobAiThrottle = v)
                .build());
        ai.addEntry(eb.startDoubleField(Component.literal("Active radius (blocks)"), cfg.aiActiveRadius)
                .setMin(4.0)
                .setSaveConsumer(v -> cfg.aiActiveRadius = v)
                .build());
        ai.addEntry(eb.startIntSlider(Component.literal("Throttle interval (ticks)"), cfg.aiThrottleInterval, 1, 40)
                .setSaveConsumer(v -> cfg.aiThrottleInterval = v)
                .build());
        ai.addEntry(eb.startBooleanToggle(Component.literal("Enable Brain-based mob throttle"), cfg.enableBrainThrottle)
                .setTooltip(Component.literal("Same idea, for villagers/piglins/etc. — they use Brain.tick() instead of the goal selector."))
                .setSaveConsumer(v -> cfg.enableBrainThrottle = v)
                .build());
        ai.addEntry(eb.startIntSlider(Component.literal("Brain throttle interval (ticks)"), cfg.brainThrottleInterval, 1, 40)
                .setSaveConsumer(v -> cfg.brainThrottleInterval = v)
                .build());

        ConfigCategory items = builder.getOrCreateCategory(Component.literal("Item Merging"));
        items.addEntry(eb.startBooleanToggle(Component.literal("Enable item merging"), cfg.enableItemMerging)
                .setTooltip(Component.literal("Periodically merges nearby stackable item drops near each player."))
                .setSaveConsumer(v -> cfg.enableItemMerging = v)
                .build());
        items.addEntry(eb.startIntField(Component.literal("Merge interval (ticks)"), cfg.itemMergeIntervalTicks)
                .setMin(20)
                .setSaveConsumer(v -> cfg.itemMergeIntervalTicks = v)
                .build());
        items.addEntry(eb.startDoubleField(Component.literal("Scan radius (blocks)"), cfg.itemMergeScanRadius)
                .setMin(4.0)
                .setSaveConsumer(v -> cfg.itemMergeScanRadius = v)
                .build());
        items.addEntry(eb.startDoubleField(Component.literal("Merge radius (blocks)"), cfg.itemMergeRadius)
                .setMin(0.5)
                .setSaveConsumer(v -> cfg.itemMergeRadius = v)
                .build());

        ConfigCategory xp = builder.getOrCreateCategory(Component.literal("XP Orb Merging"));
        xp.addEntry(eb.startBooleanToggle(Component.literal("Enable XP orb merging"), cfg.enableXpOrbMerging)
                .setTooltip(Component.literal("Periodically merges nearby XP orbs near each player. Disables itself automatically (no XP lost) if it can't verify merging is safe."))
                .setSaveConsumer(v -> cfg.enableXpOrbMerging = v)
                .build());
        xp.addEntry(eb.startIntField(Component.literal("Merge interval (ticks)"), cfg.xpMergeIntervalTicks)
                .setMin(20)
                .setSaveConsumer(v -> cfg.xpMergeIntervalTicks = v)
                .build());
        xp.addEntry(eb.startDoubleField(Component.literal("Scan radius (blocks)"), cfg.xpMergeScanRadius)
                .setMin(4.0)
                .setSaveConsumer(v -> cfg.xpMergeScanRadius = v)
                .build());
        xp.addEntry(eb.startDoubleField(Component.literal("Merge radius (blocks)"), cfg.xpMergeRadius)
                .setMin(0.5)
                .setSaveConsumer(v -> cfg.xpMergeRadius = v)
                .build());

        ConfigCategory autoTune = builder.getOrCreateCategory(Component.literal("Low-End Auto-Tune"));
        autoTune.addEntry(eb.startBooleanToggle(Component.literal("Enable low-end auto-tune"), cfg.enableLowEndAutoTune)
                .setTooltip(Component.literal("On first launch, lowers a few visual options if your system looks constrained."))
                .setSaveConsumer(v -> cfg.enableLowEndAutoTune = v)
                .build());
        autoTune.addEntry(eb.startLongField(Component.literal("Max memory threshold (MB)"), cfg.autoTuneMaxMemoryMb)
                .setMin(256L)
                .setSaveConsumer(v -> cfg.autoTuneMaxMemoryMb = v)
                .build());
        autoTune.addEntry(eb.startIntSlider(Component.literal("Min core count threshold"), cfg.autoTuneMinCores, 1, 32)
                .setSaveConsumer(v -> cfg.autoTuneMinCores = v)
                .build());

        return builder.build();
    }
}
