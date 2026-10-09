package dev.argon.chunks;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Low-frequency counters for the optional native cancelled-task cleanup pass.
 * Records one sample per scan, not one sample per chunk task.
 */
public final class ChunkCleanupMetrics {
    public record Snapshot(
            long scanCount,
            long entriesInspected,
            long entriesPruned,
            long totalDurationNanos,
            long maximumDurationNanos) {
        public double averageDurationNanos() {
            return scanCount == 0 ? 0.0 : (double) totalDurationNanos / scanCount;
        }
    }

    private final AtomicLong scanCount = new AtomicLong();
    private final AtomicLong entriesInspected = new AtomicLong();
    private final AtomicLong entriesPruned = new AtomicLong();
    private final AtomicLong totalDurationNanos = new AtomicLong();
    private final AtomicLong maximumDurationNanos = new AtomicLong();

    public void recordScan(int inspected, int pruned, long durationNanos) {
        if (inspected < 0 || pruned < 0 || pruned > inspected || durationNanos < 0) {
            throw new IllegalArgumentException("invalid chunk-cleanup scan measurements");
        }

        scanCount.incrementAndGet();
        entriesInspected.addAndGet(inspected);
        entriesPruned.addAndGet(pruned);
        totalDurationNanos.addAndGet(durationNanos);
        maximumDurationNanos.accumulateAndGet(durationNanos, Math::max);
    }

    public Snapshot snapshot() {
        return new Snapshot(
                scanCount.get(),
                entriesInspected.get(),
                entriesPruned.get(),
                totalDurationNanos.get(),
                maximumDurationNanos.get());
    }

    public void reset() {
        scanCount.set(0L);
        entriesInspected.set(0L);
        entriesPruned.set(0L);
        totalDurationNanos.set(0L);
        maximumDurationNanos.set(0L);
    }
}
