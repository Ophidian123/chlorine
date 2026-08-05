package com.chlorine.mixin;

import com.chlorine.Chlorine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles goal-selector AI updates for mobs that have no player nearby.
 *
 * Sodium/Iris don't touch this at all (rendering-only), and it's scoped
 * narrowly enough that it should coexist fine with Lithium if you also run
 * that — this only skips *this* method's body on off-ticks, it doesn't
 * change how the method itself works.
 *
 * A mob that's far from every player still falls, swims, collides and
 * takes damage normally; it just doesn't re-run goal/target selection on
 * every single tick while nobody's around to notice.
 *
 * === RISK NOTE ===
 * `customServerAiStep()` has been Mob's stable per-tick AI entry point
 * across many Minecraft versions, and 26.2's changelog says it's focused
 * on rendering and registration, not entity AI — so this target is a
 * reasonable bet. Still, 26.2 is a brand-new, freshly-unobfuscated
 * version I don't have first-hand build feedback on. If Mixin fails to
 * apply this at startup (check the crash log for "MobAiThrottleMixin"),
 * open net.minecraft.world.entity.Mob in your IDE after a Gradle sync
 * and confirm the exact method name/signature, then fix the `method =`
 * value below to match.
 */
@Mixin(Mob.class)
public abstract class MobAiThrottleMixin {

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void chlorine$throttleDistantAi(CallbackInfo ci) {
        if (!Chlorine.CONFIG.enableDistantMobAiThrottle) {
            return;
        }

        Mob self = (Mob) (Object) this;

        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }

        Player nearest = level.getNearestPlayer(self, Chlorine.CONFIG.aiActiveRadius);
        if (nearest != null) {
            // Someone's close enough to notice — always tick AI normally.
            return;
        }

        int interval = Math.max(1, Chlorine.CONFIG.aiThrottleInterval);
        long gameTime = level.getGameTime();

        // Offset by entity ID so throttled mobs don't all "pause" on the
        // same tick — that would look like synchronized lag/stutter.
        if ((gameTime + self.getId()) % interval != 0) {
            ci.cancel();
        }
    }
}
