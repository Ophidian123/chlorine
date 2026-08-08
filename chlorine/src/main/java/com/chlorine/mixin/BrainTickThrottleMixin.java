package com.chlorine.mixin;

import com.chlorine.Chlorine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles Brain.tick() for Brain-driven mobs (villagers, piglins,
 * hoglins, allays, frogs, and anything else using the Brain/behavior
 * system) that have no player nearby.
 *
 * This exists as a separate mixin from MobAiThrottleMixin because Brain
 * ticking doesn't go through Mob.customServerAiStep() the way classic
 * goal-selector AI does — Brain-owning mobs override that method
 * entirely (rather than calling super()) to instead call
 * `this.brain.tick(level, this)`, which means MobAiThrottleMixin's
 * injection into Mob's version never actually fires for them. Targeting
 * Brain.tick() directly instead means one mixin covers every Brain-based
 * mob uniformly, regardless of species.
 *
 * As with the classic AI throttle: skipping Brain.tick() only skips
 * memory/sensor updates and behavior (re-)selection for this tick, not
 * physics — a mob mid-path continues moving via its already-computed
 * navigation, which ticks independently in LivingEntity/Mob's normal
 * tick. It just doesn't re-evaluate what it should be doing as often
 * while nobody's around to notice.
 *
 * === RISK NOTE ===
 * Targets `net.minecraft.world.entity.ai.Brain#tick(ServerLevel,
 * LivingEntity)` — a long-standing Mojmap location/signature for the
 * Brain system (introduced in 1.14's villager rework), and 26.2's
 * changelog focused on rendering/registration rather than AI, so this is
 * a reasonable bet. If Mixin fails to apply this at startup, open
 * net.minecraft.world.entity.ai.Brain in your IDE after a Gradle sync
 * and confirm the exact method signature.
 */
@Mixin(Brain.class)
public abstract class BrainTickThrottleMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void chlorine$throttleDistantBrain(ServerLevel level, LivingEntity owner, CallbackInfo ci) {
        if (!Chlorine.CONFIG.enableBrainThrottle) {
            return;
        }

        Player nearest = level.getNearestPlayer(owner, Chlorine.CONFIG.aiActiveRadius);
        if (nearest != null) {
            // Someone's close enough to notice — always tick normally.
            return;
        }

        int interval = Math.max(1, Chlorine.CONFIG.brainThrottleInterval);
        long gameTime = level.getGameTime();

        // Same entity-ID offset trick as MobAiThrottleMixin, so throttled
        // mobs don't all "pause" on the same tick.
        if ((gameTime + owner.getId()) % interval != 0) {
            ci.cancel();
        }
    }
}
