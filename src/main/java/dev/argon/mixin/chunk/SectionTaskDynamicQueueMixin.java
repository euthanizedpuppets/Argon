package dev.argon.mixin.chunk;

import dev.argon.ArgonClient;
import dev.argon.chunks.CancelledChunkTaskPruner;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection.SectionTask;
import net.minecraft.client.renderer.chunk.SectionTaskDynamicQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        int removed = CancelledChunkTaskPruner.pruneCancelled(
                this.tasks,
                task -> ((SectionTaskCancellationAccessor) (Object) task)
                        .argon$getIsCancelled().get());
        ArgonClient.recordCancelledChunkTasksPruned(removed);
    }
}
