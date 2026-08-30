package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the optional distance pre-cull after SoundEngine has resolved the
 * SoundInstance, so its volume is safe to read.
 */
@Mixin(SoundEngine.class)
public abstract class SoundPreCullMixin {
    @Inject(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"
            ),
            cancellable = true
    )
    private void chlorine$preCullSound(
            SoundInstance sound,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir
    ) {
        if (!Chlorine.CONFIG.enableSoundPreCull) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || sound.getAttenuation() != SoundInstance.Attenuation.LINEAR) {
            return;
        }

        double dx = mc.player.getX() - sound.getX();
        double dy = mc.player.getY() - sound.getY();
        double dz = mc.player.getZ() - sound.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double effectiveVolume = sound.getVolume() * Math.max(0.0, 1.0 - dist / 16.0);
        if (effectiveVolume < Chlorine.CONFIG.soundPreCullVolumeThreshold) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
