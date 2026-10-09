package dev.argon.performance;

import java.util.Arrays;

/**
 * Fixed-size rolling frame-time window. Values are nanoseconds.
 *
 * This class deliberately knows nothing about Minecraft or a graphics API.
 * Integrate it at a verified frame boundary after the target runtime is tested.
 */
public final class FrameTimeTracker {
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
        int index = Math.max(0, (int) Math.ceil(bounded * sorted.length) - 1);
        return sorted[index];
    }

    public synchronized void reset() {
        Arrays.fill(samples, 0L);
        next = 0;
        size = 0;
    }
}
