package dev.argon.mixin.chunk;

import java.util.concurrent.atomic.AtomicBoolean;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access to vanilla's cancellation flag for diagnostic queue cleanup. */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$SectionTask")
public interface SectionTaskCancellationAccessor {
    @Accessor("isCancelled")
    AtomicBoolean argon$getIsCancelled();
}
