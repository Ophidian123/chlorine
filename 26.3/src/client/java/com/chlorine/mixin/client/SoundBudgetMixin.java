package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import com.chlorine.client.ClientEffectBudgets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caps how many new sounds can start within the same client tick.
 *
 * Not distance-based — vanilla already attenuates and culls sounds too
 * quiet/far to matter, so redoing that would be redundant. This targets a
 * different problem: a burst of many sounds starting in the same instant
 * (a mob farm killing dozens of mobs at once, a big explosion triggering
 * many block-break sounds) can momentarily overload the audio engine
 * regardless of any individual sound's distance. Overflow sounds for
 * that tick are simply dropped, not queued or delayed to a later tick —
 * a delayed hurt/death sound would be more jarring than a dropped one.
 *
 * The budget resets every client tick through ClientEffectBudgets.
 *
 * === RISK NOTE ===
 * Targets `net.minecraft.client.sounds.SoundManager#play(SoundInstance)`
 * and `net.minecraft.client.resources.sounds.SoundInstance` — both
 * long-standing, frequently-referenced Mojmap locations (the sound system
 * hasn't changed much since its 1.13 rewrite), so this is lower risk than
 * most of the other guesses in this project. If Mixin fails to apply this
 * at startup, open net.minecraft.client.sounds.SoundManager in your IDE
 * after a Gradle sync and confirm the method name/signature.
 */
@Mixin(SoundManager.class)
public abstract class SoundBudgetMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void chlorine$limitSoundBudget(
            SoundInstance sound,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir
    ) {
        // --- Distance-based pre-cull ---
        if (Chlorine.CONFIG.enableSoundPreCull) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null
                    && sound.getSound() != null
                    && sound.getAttenuation() == SoundInstance.Attenuation.LINEAR) {
                double dx = mc.player.getX() - sound.getX();
                double dy = mc.player.getY() - sound.getY();
                double dz = mc.player.getZ() - sound.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                // Vanilla linear attenuation: volume drops to 0 at 16 blocks
                // by default. Estimate effective volume as:
                //   volume * max(0, 1 - sqrt(distSq) / 16)
                // If that's below the threshold, drop before OpenAL.
                double dist = Math.sqrt(distSq);
                double effectiveVolume = sound.getVolume() * Math.max(0.0, 1.0 - dist / 16.0);
                if (effectiveVolume < Chlorine.CONFIG.soundPreCullVolumeThreshold) {
                    cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                    return;
                }
            }
        }

        if (!Chlorine.CONFIG.enableSoundBudget) {
            return;
        }

        if (!ClientEffectBudgets.tryConsumeSound(Chlorine.CONFIG.maxNewSoundsPerTick)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
    }
}
