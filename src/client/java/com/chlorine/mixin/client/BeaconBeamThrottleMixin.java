package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips rendering beacon beam overlays when the beacon block entity is
 * far from the camera.
 */
@Mixin(BeaconRenderer.class)
public abstract class BeaconBeamThrottleMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BeaconRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void chlorine$throttleDistantBeaconBeam(
            BeaconRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (!Chlorine.CONFIG.enableBeaconThrottle) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || state.blockPos == null) {
            return;
        }

        double dx = mc.player.getX() - state.blockPos.getX();
        double dy = mc.player.getY() - state.blockPos.getY();
        double dz = mc.player.getZ() - state.blockPos.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        double thresholdSq = Chlorine.CONFIG.beaconThrottleDistance
                           * Chlorine.CONFIG.beaconThrottleDistance;

        if (distSq > thresholdSq) {
            ci.cancel();
        }
    }
}
