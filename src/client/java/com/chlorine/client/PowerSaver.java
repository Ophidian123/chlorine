package com.chlorine.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;

/**
 * Caps the framerate while the game window is unfocused (alt-tabbed,
 * minimized, or just sitting behind another window).
 *
 * This is a distinctly laptop-shaped problem that none of Sodium, Iris,
 * FerriteCore, EntityCulling or ImmediatelyFast address: they all care
 * about making the frame you're looking at faster, not about the fact
 * that a laptop GPU/fan doesn't need to render 200 unfocused frames a
 * second while you're reading something else in another window.
 *
 * === RISK NOTE ===
 * Cloud hiding targets `options.cloudStatus()`, an OptionInstance<CloudStatus>
 * with an OFF value — if that name/enum has moved in 26.2, open
 * net.minecraft.client.Options / CloudStatus in your IDE and adjust.
 */
public class PowerSaver {
    private boolean throttled = false;
    private int savedFramerateLimit = -1;
    private net.minecraft.client.CloudStatus savedCloudStatus = null;

    public void tick(Minecraft client) {
        boolean focused = client.isWindowActive();

        if (!focused && !throttled) {
            var fpsOption = client.options.framerateLimit();
            savedFramerateLimit = fpsOption.get();
            fpsOption.set(Math.min(savedFramerateLimit, Chlorine.CONFIG.unfocusedFramerateLimit));

            if (Chlorine.CONFIG.hideCloudsWhenUnfocused) {
                var cloudOption = client.options.cloudStatus();
                savedCloudStatus = cloudOption.get();
                cloudOption.set(net.minecraft.client.CloudStatus.OFF);
            }

            throttled = true;
            Chlorine.LOGGER.debug("Window unfocused, capping framerate to {}", Chlorine.CONFIG.unfocusedFramerateLimit);
        } else if (focused && throttled) {
            if (savedFramerateLimit > 0) {
                client.options.framerateLimit().set(savedFramerateLimit);
            }
            if (savedCloudStatus != null) {
                client.options.cloudStatus().set(savedCloudStatus);
                savedCloudStatus = null;
            }
            throttled = false;
            Chlorine.LOGGER.debug("Window focused again, restoring framerate limit");
        }
    }
}
