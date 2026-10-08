package com.chlorine.client.mixin;

import net.minecraft.client.Options;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Options.class)
abstract class SimulationDistanceRangeMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance$IntRange;<init>(IIZ)V"
            ),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.simulationDistance"),
                    to = @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/Options;simulationDistance:Lnet/minecraft/client/OptionInstance;",
                            opcode = Opcodes.PUTFIELD
                    )
            ),
            index = 0
    )
    private int chlorine$allowSimulationDistanceOfOne(int minimum) {
        return 1;
    }
}
