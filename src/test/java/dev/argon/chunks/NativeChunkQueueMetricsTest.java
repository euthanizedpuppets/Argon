package dev.argon.chunks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class NativeChunkQueueMetricsTest {
    @Test
    void tracksQueuePressureAndSuccessfulPolls() {
        NativeChunkQueueMetrics metrics = new NativeChunkQueueMetrics();

        metrics.recordAdd(1);
        metrics.recordAdd(4);
        metrics.recordPoll(true, 3);
        metrics.recordPoll(false, 3);

        assertEquals(new NativeChunkQueueMetrics.Snapshot(2, 2, 1, 0, 0, 3, 4),
                metrics.snapshot());
    }

    @Test
    void recordsClearsAndResetsCurrentDepthWithoutLosingPeak() {
        NativeChunkQueueMetrics metrics = new NativeChunkQueueMetrics();
        metrics.recordAdd(7);

        metrics.recordClear(7);

        assertEquals(new NativeChunkQueueMetrics.Snapshot(1, 0, 0, 1, 7, 0, 7),
                metrics.snapshot());
    }

    @Test
    void emptyMetricsStartAtZero() {
        NativeChunkQueueMetrics metrics = new NativeChunkQueueMetrics();

        assertEquals(new NativeChunkQueueMetrics.Snapshot(0, 0, 0, 0, 0, 0, 0),
                metrics.snapshot());
    }

    @Test
    void resetClearsCountersAndDepths() {
        NativeChunkQueueMetrics metrics = new NativeChunkQueueMetrics();
        metrics.recordAdd(9);
        metrics.recordPoll(true, 8);
        metrics.recordClear(8);

        metrics.reset();

        assertEquals(new NativeChunkQueueMetrics.Snapshot(0, 0, 0, 0, 0, 0, 0),
                metrics.snapshot());
    }

    @Test
    void rejectsNegativeDepthsAndClearCounts() {
        NativeChunkQueueMetrics metrics = new NativeChunkQueueMetrics();

        assertThrows(IllegalArgumentException.class, () -> metrics.recordAdd(-1));
        assertThrows(IllegalArgumentException.class, () -> metrics.recordPoll(false, -1));
        assertThrows(IllegalArgumentException.class, () -> metrics.recordClear(-1));
    }
}
