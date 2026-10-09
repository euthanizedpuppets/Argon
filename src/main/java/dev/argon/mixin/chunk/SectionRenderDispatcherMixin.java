package dev.argon.mixin.chunk;

import dev.argon.ArgonClient;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Measures CPU wall time spent in Minecraft's terrain-buffer upload pass.
 * It is not a GPU completion timer and does not alter uploads.
 */
@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherMixin {
    @Unique
    private long argon$uploadPassStartedAtNanos;

    @Unique
    private boolean argon$uploadPassTimerActive;

    @Inject(method = "uploadTerrainBuffersToGpu", at = @At("HEAD"))
    private void argon$startUploadPassTimer(CallbackInfo ci) {
        this.argon$uploadPassTimerActive = ArgonClient.config().telemetryEnabled();
        if (this.argon$uploadPassTimerActive) {
            this.argon$uploadPassStartedAtNanos = System.nanoTime();
        }
    }

    @Inject(method = "uploadTerrainBuffersToGpu", at = @At("RETURN"))
    private void argon$finishUploadPassTimer(CallbackInfo ci) {
        if (this.argon$uploadPassTimerActive) {
            ArgonClient.recordTerrainUploadPassDuration(
                    System.nanoTime() - this.argon$uploadPassStartedAtNanos);
        }
        this.argon$uploadPassTimerActive = false;
        this.argon$uploadPassStartedAtNanos = 0L;
    }
}
