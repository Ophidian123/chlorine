package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips rendering item frame contents and frame models beyond a
 * configurable distance from the camera.
 *
 * In modern Minecraft (26.2+), canceling extractRenderState leaves the
 * newly allocated ItemFrameRenderState unpopulated (causing state.entityType
 * to be null, leading to an NPE in EntityRenderDispatcher.submit).
 * Canceling submit() directly avoids all draw calls and map/item model
 * processing for distant item frames without corrupting the render pipeline.
 */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameThrottleMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void chlorine$throttleDistantItemFrames(
            ItemFrameRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (!Chlorine.CONFIG.enableItemFrameThrottle) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        double dx = mc.player.getX() - state.x;
        double dy = mc.player.getY() - state.y;
        double dz = mc.player.getZ() - state.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double thresholdSq = Chlorine.CONFIG.itemFrameRenderDistance
                           * Chlorine.CONFIG.itemFrameRenderDistance;

        if (distSq > thresholdSq) {
            ci.cancel();
        }
    }
}
