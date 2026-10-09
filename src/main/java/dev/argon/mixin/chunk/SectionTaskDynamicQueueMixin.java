package dev.argon.mixin.chunk;

import dev.argon.ArgonClient;
import dev.argon.chunks.CancelledChunkTaskPruner;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection.SectionTask;
import net.minecraft.client.renderer.chunk.SectionTaskDynamicQueue;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Optional queue hygiene: periodically discard cancelled tasks before adding
 * another task. Vanilla's poll selection, distance ordering, and recompile
 * quota remain completely unchanged.
 */
@Mixin(SectionTaskDynamicQueue.class)
public abstract class SectionTaskDynamicQueueMixin {
    @Unique
    private static final int argon$minimumQueueSize = 32;
    @Unique
    private static final int argon$additionsBetweenScans = 16;

    @Shadow
    @Final
    private List<SectionTask> tasks;

    @Unique
    private int argon$additionsSinceScan;

    @Inject(method = "add", at = @At("HEAD"))
    private void argon$pruneCancelledTasksBeforeAdd(SectionTask newTask, CallbackInfo ci) {
        if (!ArgonClient.config().cancelledChunkTaskCleanupEnabled()) {
            return;
        }
        if (this.tasks.size() < argon$minimumQueueSize) {
            this.argon$additionsSinceScan = 0;
            return;
        }
        if (++this.argon$additionsSinceScan < argon$additionsBetweenScans) {
            return;
        }
        this.argon$additionsSinceScan = 0;

        boolean measure = ArgonClient.config().telemetryEnabled();
        long startedNanos = measure ? System.nanoTime() : 0L;
        int inspected = this.tasks.size();
        int removed = CancelledChunkTaskPruner.pruneCancelled(
                this.tasks,
                task -> ((SectionTaskCancellationAccessor) (Object) task)
                        .argon$getIsCancelled().get());

        if (measure) {
            ArgonClient.recordChunkCleanupScan(
                    inspected, removed, System.nanoTime() - startedNanos);
        }
    }
    @Unique
    private int argon$entriesBeforeClear;

    @Inject(method = "add", at = @At("TAIL"))
    private void argon$recordQueueAdd(SectionTask task, CallbackInfo ci) {
        if (ArgonClient.config().telemetryEnabled()) {
            ArgonClient.recordNativeChunkQueueAdd(task, this.tasks.size());
        }
    }

    @Inject(method = "poll", at = @At("RETURN"))
    private void argon$recordQueuePoll(
            Vec3 cameraPos, CallbackInfoReturnable<SectionTask> cir) {
        if (ArgonClient.config().telemetryEnabled()) {
            ArgonClient.recordNativeChunkQueuePoll(
                    cir.getReturnValue(), cir.getReturnValue() != null, this.tasks.size());
        }
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void argon$rememberQueueDepthBeforeClear(CallbackInfo ci) {
        this.argon$entriesBeforeClear = ArgonClient.config().telemetryEnabled()
                ? this.tasks.size() : 0;
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void argon$recordQueueClear(CallbackInfo ci) {
        if (ArgonClient.config().telemetryEnabled()) {
            ArgonClient.recordNativeChunkQueueClear(this.argon$entriesBeforeClear);
        }
        this.argon$entriesBeforeClear = 0;
    }

}
