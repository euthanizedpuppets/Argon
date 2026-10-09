package dev.argon.chunks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ChunkCleanupMetricsTest {
    @Test
    void aggregatesScanCountsDurationsAndPrunedEntries() {
        ChunkCleanupMetrics metrics = new ChunkCleanupMetrics();

        metrics.recordScan(40, 3, 1_000L);
        metrics.recordScan(64, 8, 3_000L);

        assertEquals(new ChunkCleanupMetrics.Snapshot(2, 104, 11, 4_000L, 3_000L),
                metrics.snapshot());
        assertEquals(2_000.0, metrics.snapshot().averageDurationNanos());
    }

    @Test
    void emptyMetricsHaveZeroAverage() {
        ChunkCleanupMetrics metrics = new ChunkCleanupMetrics();

        assertEquals(0.0, metrics.snapshot().averageDurationNanos());
        assertEquals(0L, metrics.snapshot().scanCount());
    }

    @Test
    void resetClearsAllMeasurements() {
        ChunkCleanupMetrics metrics = new ChunkCleanupMetrics();
        metrics.recordScan(100, 20, 5_000L);

        metrics.reset();

        assertEquals(new ChunkCleanupMetrics.Snapshot(0, 0, 0, 0, 0), metrics.snapshot());
    }

    @Test
    void rejectsImpossibleMeasurements() {
        ChunkCleanupMetrics metrics = new ChunkCleanupMetrics();

        assertThrows(IllegalArgumentException.class, () -> metrics.recordScan(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> metrics.recordScan(2, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> metrics.recordScan(2, 0, -1));
    }
}
