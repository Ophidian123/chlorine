package com.chlorine.mixin.client;

import com.chlorine.Chlorine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses nether portal ambient particle emission when the portal block
 * is far from the camera.
 *
 * Nether portal blocks call {@code animateTick()} every client tick to
 * spawn dense purple particles. This is purely cosmetic at distance and
 * the particles themselves are lightweight individually, but a portal
 * room with many portal blocks (or a portal-based farm) generates a
 * continuous stream of particle entities that add up.
 *
 * === RISK NOTE ===
 * Targets {@code NetherPortalBlock#animateTick(BlockState, Level,
 * BlockPos, RandomSource)}. This is a standard Block override that the
 * client calls for ambient visual effects — it's been stable for many
 * versions. If it's moved or renamed in 26.2, check
 * {@code net.minecraft.world.level.block.NetherPortalBlock}.
 */
@Mixin(NetherPortalBlock.class)
public abstract class PortalParticleThrottleMixin {

    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void chlorine$throttleDistantPortalParticles(
            BlockState state, Level level, BlockPos pos, RandomSource random,
            CallbackInfo ci) {
        if (!Chlorine.CONFIG.enablePortalParticleThrottle) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        double dx = mc.player.getX() - pos.getX();
        double dy = mc.player.getY() - pos.getY();
        double dz = mc.player.getZ() - pos.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        double thresholdSq = Chlorine.CONFIG.portalParticleThrottleDistance
                           * Chlorine.CONFIG.portalParticleThrottleDistance;

        if (distSq > thresholdSq) {
            ci.cancel();
        }
    }
}
