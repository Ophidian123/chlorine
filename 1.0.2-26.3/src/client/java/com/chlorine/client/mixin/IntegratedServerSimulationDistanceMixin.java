package com.chlorine.client.mixin;

import com.chlorine.client.SimulationDistanceSync;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IntegratedServer.class)
abstract class IntegratedServerSimulationDistanceMixin {
    @Redirect(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 1
            )
    )
    private Object chlorine$readPublishedSimulationDistance(OptionInstance<?> option) {
        return Integer.valueOf(SimulationDistanceSync.getOr((Integer) option.get()));
    }
}
