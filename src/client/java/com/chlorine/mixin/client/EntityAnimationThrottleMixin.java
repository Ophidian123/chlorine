package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles skeletal animation recalculation for visible entities that are
 * far from the camera.
 *
 * Entities beyond {@code entityAnimThrottleDistance} still render — on
 * skipped frames, this cancels the render-state extraction for that frame,
 * which leaves the previously-extracted state (last frame's pose) in
 * place rather than recalculating limb swing, head rotation, and body yaw.
 * At 48+ blocks this is imperceptible but saves meaningful CPU time when
 * many mobs are on-screen (mob farms visible from a distance, village
 * squares, etc.).
 *
 * NOTE: an earlier version of this mixin tried to "freeze" the pose by
 * directly calling entity.walkAnimation.setSpeed(0.0f) on skip-frames.
 * That was removed — mutating that shared, mutable entity state from a
 * per-render-frame hook can fight with the entity's own per-tick
 * animation update, risking a genuinely stuck/broken-looking pose rather
 * than the intended subtle effect. Simply not re-extracting state on
 * skipped frames achieves the same visual goal more safely, by relying on
 * the render state object naturally retaining its previous values.
 *
 * This is orthogonal to EntityCulling (which skips entities behind
 * walls entirely) and ImmediatelyFast (which batches draw calls) —
 * neither of them touch per-entity animation math. That said, both hook
 * similar entity-render entry points, so test with EntityCulling enabled
 * specifically before shipping this as stable.
 *
 * === RISK NOTE ===
 * Targets {@code LivingEntityRenderer#extractRenderState}. This is the
 * per-frame render-state population entry point for all LivingEntity
 * subclasses. If the method name/signature has changed in 26.2, open
 * {@code net.minecraft.client.renderer.entity.LivingEntityRenderer} in
 * your IDE and adjust the {@code method} value below.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class EntityAnimationThrottleMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void chlorine$throttleDistantAnimation(
            LivingEntity entity,
            LivingEntityRenderState state,
            float partialTick,
            CallbackInfo ci) {
        if (!Chlorine.CONFIG.enableEntityAnimationThrottle) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        double distSq = mc.player.distanceToSqr(entity);
        double thresholdSq = Chlorine.CONFIG.entityAnimThrottleDistance
                           * Chlorine.CONFIG.entityAnimThrottleDistance;

        if (distSq <= thresholdSq) {
            return; // Close enough — extract/animate normally every frame
        }

        int interval = Math.max(1, Chlorine.CONFIG.entityAnimThrottleInterval);
        long frame = mc.level != null ? mc.level.getGameTime() : 0;
        if ((frame + entity.getId()) % interval != 0) {
            // Zero the walk animation speed on skip-frames for distant entities.
            // This skips limb swing calculations during model setup without
            // leaving the newly-created LivingEntityRenderState uninitialized
            // or causing a NullPointerException in EntityRenderDispatcher.
            state.walkAnimationSpeed = 0.0f;
        }
    }
}
