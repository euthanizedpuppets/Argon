package dev.argon.mixin.chunk;

import dev.argon.ArgonClient;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Times actual section mesh compile work, excluding time waiting in the queue.
 * This observes the worker-side operation and never changes its result.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
public abstract class SectionCompileTaskMixin {
    @Unique
    private long argon$compileStartedAtNanos;

    @Unique
    private boolean argon$compileTimerActive;

    @Inject(method = "doTask", at = @At("HEAD"))
    private void argon$startCompileTimer(
            SectionBufferBuilderPack buffers,
            CallbackInfoReturnable<?> cir) {
        this.argon$compileTimerActive = ArgonClient.config().telemetryEnabled();
        if (this.argon$compileTimerActive) {
            this.argon$compileStartedAtNanos = System.nanoTime();
        }
    }

    @Inject(method = "doTask", at = @At("RETURN"))
    private void argon$finishCompileTimer(
            SectionBufferBuilderPack buffers,
            CallbackInfoReturnable<?> cir) {
        if (this.argon$compileTimerActive) {
            ArgonClient.recordSectionCompileDuration(
                    System.nanoTime() - this.argon$compileStartedAtNanos);
        }
        this.argon$compileTimerActive = false;
        this.argon$compileStartedAtNanos = 0L;
    }
}
