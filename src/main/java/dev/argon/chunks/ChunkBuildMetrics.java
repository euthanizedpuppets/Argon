package dev.argon.chunks;

import dev.argon.performance.FrameTimeTracker;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Low-overhead, session-local timing for the client section-mesh pipeline.
 * Measurements are diagnostic and never influence the game's scheduling.
 */
public final class ChunkBuildMetrics {
    private final FrameTimeTracker queueWaitNanos;
    private final FrameTimeTracker compileDurationNanos;
    private final FrameTimeTracker uploadPassDurationNanos;

    /*
     * Weak keys prevent tasks discarded internally by vanilla or the optional
     * cancellation pruner from being retained indefinitely if they never poll.
     * WeakHashMap is safe here because all access is synchronized.
     */
    private final Map<Object, Long> enqueuedAtNanos = new WeakHashMap<>();
    private final AtomicLong queueWaitSamples = new AtomicLong();
    private final AtomicLong compileSamples = new AtomicLong();
    private final AtomicLong uploadPassSamples = new AtomicLong();

    public ChunkBuildMetrics(int sampleWindow) {
        queueWaitNanos = new FrameTimeTracker(sampleWindow);
        compileDurationNanos = new FrameTimeTracker(sampleWindow);
        uploadPassDurationNanos = new FrameTimeTracker(sampleWindow);
    }

    public synchronized void recordEnqueued(Object task, long timestampNanos) {
        if (task != null) {
            enqueuedAtNanos.put(task, timestampNanos);
        }
    }

    /** Records time spent in the native section-task queue before a task is returned by poll. */
    public synchronized void recordDequeued(Object task, long timestampNanos) {
        Long startedAt = enqueuedAtNanos.remove(task);
        if (startedAt == null) {
            return;
        }

        long elapsed = timestampNanos - startedAt;
        if (elapsed > 0L) {
            queueWaitNanos.recordFrame(elapsed);
            queueWaitSamples.incrementAndGet();
        }
    }

    /** Forget tasks after vanilla clears its pending queue. */
    public synchronized void clearPendingTasks() {
        enqueuedAtNanos.clear();
    }

    public void recordCompileDuration(long durationNanos) {
        if (durationNanos > 0L) {
            compileDurationNanos.recordFrame(durationNanos);
            compileSamples.incrementAndGet();
        }
    }

    /** This is CPU wall time in the upload method, not a GPU completion timer. */
    public void recordUploadPassDuration(long durationNanos) {
        if (durationNanos > 0L) {
            uploadPassDurationNanos.recordFrame(durationNanos);
            uploadPassSamples.incrementAndGet();
        }
    }

    public FrameTimeTracker queueWaitNanos() {
        return queueWaitNanos;
    }

    public FrameTimeTracker compileDurationNanos() {
        return compileDurationNanos;
    }

    public FrameTimeTracker uploadPassDurationNanos() {
        return uploadPassDurationNanos;
    }

    public long totalQueueWaitSamples() {
        return queueWaitSamples.get();
    }

    public long totalCompileSamples() {
        return compileSamples.get();
    }

    public long totalUploadPassSamples() {
        return uploadPassSamples.get();
    }

    public synchronized int pendingTaskTimestamps() {
        return enqueuedAtNanos.size();
    }
}
