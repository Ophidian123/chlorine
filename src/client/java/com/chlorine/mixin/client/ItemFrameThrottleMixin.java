package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles render-state extraction (held item/map contents) for item
 * frames beyond a configurable distance from the camera.
 *
 * Item frames — especially those holding maps — are individually expensive
 * to render: each one involves its own item model or map texture upload
 * during state extraction. Storage rooms and trading halls with hundreds
 * of item frames cause frametime spikes that neither Sodium (chunk
 * meshes) nor EntityCulling (occlusion, not distance) cover.
 *
 * NOTE: an earlier version of this mixin permanently cancelled state
 * extraction the whole time a frame was beyond the distance threshold,
 * with a javadoc claiming this "cancels the entire render, making the
 * frame invisible." Neither part of that held up: cancelling
 * extractRenderState doesn't cancel the separate render/submit call that
 * consumes the extracted state, so the frame likely kept rendering
 * anyway using stale data — and worse, a frame that had *never* been
 * within range even once would have its render state permanently
 * unpopulated, which risks a crash wherever that state's fields are
 * assumed non-null. Fixed by switching to the same staggered-interval
 * pattern used elsewhere in this project: extraction still happens
 * periodically even at distance, so state is never permanently stale or
 * unpopulated, while still skipping the bulk of frames.
 *
 * === RISK NOTE ===
 * Targets {@code ItemFrameRenderer#extractRenderState}. ItemFrameRenderer
 * is the dedicated entity renderer for both ItemFrame and GlowItemFrame
 * (which extends ItemFrame). If the method name/signature has changed in
 * 26.2, open {@code net.minecraft.client.renderer.entity.ItemFrameRenderer}
 * in your IDE and adjust the {@code method} value below.
 */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameThrottleMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At("HEAD"), cancellable = true)
    private void chlorine$throttleDistantItemFrames(
            ItemFrame entity,
            ItemFrameRenderState state,
            float partialTick,
            CallbackInfo ci) {
        if (!Chlorine.CONFIG.enableItemFrameThrottle) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        double distSq = mc.player.distanceToSqr(entity);
        double thresholdSq = Chlorine.CONFIG.itemFrameRenderDistance
                           * Chlorine.CONFIG.itemFrameRenderDistance;

        if (distSq <= thresholdSq) {
            return; // Close enough — extract normally every frame
        }

        int interval = Math.max(1, Chlorine.CONFIG.itemFrameThrottleInterval);
        long frame = mc.level != null ? mc.level.getGameTime() : 0;
        if ((frame + entity.getId()) % interval != 0) {
            ci.cancel();
        }
    }
}
