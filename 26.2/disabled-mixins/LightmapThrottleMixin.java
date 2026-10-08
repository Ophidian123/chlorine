package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles lightmap texture re-uploads to every Nth frame.
 *
 * The lightmap is a small (16×16) lookup texture mapping (block-light,
 * sky-light) pairs to final display colors. Vanilla recalculates and
 * re-uploads it to the GPU every single frame, even though its contents
 * change slowly (day/night cycle, walking past torches). Skipping a few
 * frames of re-upload saves a GPU texture upload per skipped frame —
 * invisible at 60+ FPS but measurable on integrated GPUs with limited
 * memory bandwidth.
 *
 * === RISK NOTE ===
 * Targets {@code LightTexture#updateLightTexture(float)}. This has been
 * the lightmap update entry point since at least 1.14. If 26.2 has
 * renamed or moved it, open {@code net.minecraft.client.renderer.LightTexture}
 * in your IDE.
 */
@Mixin(LightTexture.class)
public abstract class LightmapThrottleMixin {

    @Unique
    private int chlorine$frameCounter = 0;

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void chlorine$throttleLightmapUpdate(float partialTick, CallbackInfo ci) {
        if (!Chlorine.CONFIG.enableLightmapThrottle) {
            return;
        }

        int interval = Math.max(1, Chlorine.CONFIG.lightmapUpdateInterval);
        if (++chlorine$frameCounter % interval != 0) {
            ci.cancel();
        }
    }
}
