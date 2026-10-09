package dev.argon.performance;

import java.util.Arrays;

/**
 * Fixed-size rolling render-interval window. Values are nanoseconds.
 *
 * This class deliberately knows nothing about Minecraft or a graphics API.
 * Values represent whatever stable timing boundary its caller supplies.
 */
public final class FrameTimeTracker {
    /** Immutable aggregate; averageNanos is a double to avoid sum overflow. */
    public record Summary(
            int sampleCount,
            long minimumNanos,
            double averageNanos,
            long p50Nanos,
            long p95Nanos,
            long maximumNanos) {
    }

    private final long[] samples;
    private int next;
    private int size;

    public FrameTimeTracker(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        samples = new long[capacity];
    }

    public synchronized void recordFrame(long frameTimeNanos) {
        if (frameTimeNanos <= 0) {
            return;
        }
        samples[next] = frameTimeNanos;
        next = (next + 1) % samples.length;
        if (size < samples.length) {
            size++;
        }
    }

    public synchronized int sampleCount() {
        return size;
    }

    public synchronized int capacity() {
        return samples.length;
    }

    public synchronized long[] snapshot() {
        long[] copy = new long[size];
        if (size == 0) {
            return copy;
        }

        int start = size == samples.length ? next : 0;
        for (int i = 0; i < size; i++) {
            copy[i] = samples[(start + i) % samples.length];
        }
        return copy;
    }

    /**
     * Computes the common diagnostics in one snapshot and one sort. An empty
     * window returns zero for every numeric field.
     */
    public synchronized Summary summary() {
        if (size == 0) {
            return new Summary(0, 0L, 0.0, 0L, 0L, 0L);
        }

        long[] sorted = snapshot();
        Arrays.sort(sorted);

        double sum = 0.0;
        for (long sample : sorted) {
            sum += sample;
        }

        return new Summary(
                sorted.length,
                sorted[0],
                sum / sorted.length,
                percentileFromSorted(sorted, 0.50),
                percentileFromSorted(sorted, 0.95),
                sorted[sorted.length - 1]);
    }

    /**
     * Returns a nearest-rank percentile in nanoseconds, or zero with no data.
     * The requested percentile is clamped to [0, 1].
     */
    public synchronized long percentile(double percentile) {
        if (size == 0) {
            return 0L;
        }
        double bounded = Math.max(0.0, Math.min(1.0, percentile));
        long[] sorted = snapshot();
        Arrays.sort(sorted);
        return percentileFromSorted(sorted, bounded);
    }

    private static long percentileFromSorted(long[] sorted, double percentile) {
        double bounded = Math.max(0.0, Math.min(1.0, percentile));
        int index = Math.max(0, (int) Math.ceil(bounded * sorted.length) - 1);
        return sorted[index];
    }

    public synchronized void reset() {
        Arrays.fill(samples, 0L);
        next = 0;
        size = 0;
    }
}
