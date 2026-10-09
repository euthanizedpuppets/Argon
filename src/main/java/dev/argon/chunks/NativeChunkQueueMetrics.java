package dev.argon.chunks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Session-local diagnostics for Minecraft's native section-task queue.
 *
 * Measurements observe queue operations; they do not change task ordering,
 * cancellation behavior, worker scheduling, or task execution.
 */
public final class NativeChunkQueueMetrics {
    public record Snapshot(
            long tasksAdded,
            long pollCalls,
            long tasksPolled,
            long clearCalls,
            long tasksCleared,
            int currentDepth,
            int peakDepth) {
    }

    private final AtomicLong tasksAdded = new AtomicLong();
    private final AtomicLong pollCalls = new AtomicLong();
    private final AtomicLong tasksPolled = new AtomicLong();
    private final AtomicLong clearCalls = new AtomicLong();
    private final AtomicLong tasksCleared = new AtomicLong();
    private final AtomicInteger currentDepth = new AtomicInteger();
    private final AtomicInteger peakDepth = new AtomicInteger();

    public void recordAdd(int depthAfterAdd) {
        validateDepth(depthAfterAdd);
        tasksAdded.incrementAndGet();
        recordDepth(depthAfterAdd);
    }

    public void recordPoll(boolean taskReturned, int depthAfterPoll) {
        validateDepth(depthAfterPoll);
        pollCalls.incrementAndGet();
        if (taskReturned) {
            tasksPolled.incrementAndGet();
        }
        recordDepth(depthAfterPoll);
    }

    public void recordClear(int entriesCleared) {
        if (entriesCleared < 0) {
            throw new IllegalArgumentException("entriesCleared must not be negative");
        }
        clearCalls.incrementAndGet();
        tasksCleared.addAndGet(entriesCleared);
        currentDepth.set(0);
    }

    private void recordDepth(int depth) {
        currentDepth.set(depth);
        peakDepth.accumulateAndGet(depth, Math::max);
    }

    private static void validateDepth(int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("queue depth must not be negative");
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(
                tasksAdded.get(),
                pollCalls.get(),
                tasksPolled.get(),
                clearCalls.get(),
                tasksCleared.get(),
                currentDepth.get(),
                peakDepth.get());
    }

    public void reset() {
        tasksAdded.set(0L);
        pollCalls.set(0L);
        tasksPolled.set(0L);
        clearCalls.set(0L);
        tasksCleared.set(0L);
        currentDepth.set(0);
        peakDepth.set(0);
    }
}
