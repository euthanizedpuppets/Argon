package dev.argon.mixin.chunk;

import dev.argon.ArgonClient;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Diagnostic-only timing for the JFR-identified GUI intersection search.
 * The original return value and iteration logic are untouched.
 */
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
    @Inject(method = "hasIntersection", at = @At("HEAD"))
    private void argon$beginGuiIntersectionSample(
            ScreenRectangle bounds,
            List<?> states,
            CallbackInfoReturnable<Boolean> cir) {
        if (ArgonClient.config().telemetryEnabled()
                && ArgonClient.config().guiIntersectionProfilingEnabled()) {
            ArgonClient.guiIntersectionMetrics().begin(states == null ? 0 : states.size());
        }
    }

    @Inject(method = "hasIntersection", at = @At("RETURN"))
    private void argon$finishGuiIntersectionSample(
            ScreenRectangle bounds,
            List<?> states,
            CallbackInfoReturnable<Boolean> cir) {
        if (ArgonClient.config().telemetryEnabled()
                && ArgonClient.config().guiIntersectionProfilingEnabled()) {
            ArgonClient.guiIntersectionMetrics().finish(cir.getReturnValueZ());
        }
    }
}