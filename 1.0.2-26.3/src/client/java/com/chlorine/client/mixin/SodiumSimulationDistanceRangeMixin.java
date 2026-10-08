package com.chlorine.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Pseudo
@Mixin(
        targets = "net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder",
        remap = false
)
abstract class SodiumSimulationDistanceRangeMixin {
    @ModifyArg(
            method = "buildGeneralPage(Lnet/caffeinemc/mods/sodium/api/config/structure/ConfigBuilder;)Lnet/caffeinemc/mods/sodium/api/config/structure/OptionPageBuilder;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/api/config/structure/IntegerOptionBuilder;setRange(III)Lnet/caffeinemc/mods/sodium/api/config/structure/IntegerOptionBuilder;",
                    ordinal = 0,
                    remap = false
            ),
            slice = @Slice(
                    from = @At(
                            value = "CONSTANT",
                            args = "stringValue=sodium:general.simulation_distance",
                            remap = false
                    )
            ),
            index = 0,
            remap = false
    )
    private int chlorine$allowOneChunkMinimum(int minimum) {
        return 1;
    }
}
