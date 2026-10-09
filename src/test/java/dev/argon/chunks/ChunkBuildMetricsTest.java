package dev.argon.chunks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ChunkBuildMetricsTest {
    @Test
    void recordsQueueResidenceTimeForTasksPolledByVanilla() {
        ChunkBuildMetrics metrics = new ChunkBuildMetrics(30);
        Object first = new Object();
        Object second = new Object();

        metrics.recordEnqueued(first, 1_000L);
        metrics.recordEnqueued(second, 2_000L);
        metrics.recordDequeued(first, 4_000L);
        metrics.recordDequeued(second, 9_000L);

        assertEquals(2L, metrics.totalQueueWaitSamples());
        assertEquals(3_000.0, metrics.queueWaitNanos().summary().averageNanos());
        assertEquals(7_000L, metrics.queueWaitNanos().summary().maximumNanos());
        assertEquals(0, metrics.pendingTaskTimestamps());
    }

    @Test
    void recordsCompileAndUploadDurationsIndependently() {
        ChunkBuildMetrics metrics = new ChunkBuildMetrics(30);
        metrics.recordCompileDuration(2_500_000L);
        metrics.recordCompileDuration(5_500_000L);
        metrics.recordUploadPassDuration(600_000L);

        assertEquals(2L, metrics.totalCompileSamples());
        assertEquals(4_000_000.0, metrics.compileDurationNanos().summary().averageNanos());
        assertEquals(1L, metrics.totalUploadPassSamples());
        assertEquals(600_000L, metrics.uploadPassDurationNanos().summary().maximumNanos());
    }

    @Test
    void clearDropsPendingQueueTimestampsWithoutRecordingFalseSamples() {
        ChunkBuildMetrics metrics = new ChunkBuildMetrics(30);
        Object task = new Object();
        metrics.recordEnqueued(task, 100L);

        metrics.clearPendingTasks();
        metrics.recordDequeued(task, 500L);

        assertEquals(0, metrics.pendingTaskTimestamps());
        assertEquals(0L, metrics.totalQueueWaitSamples());
        assertEquals(0, metrics.queueWaitNanos().sampleCount());
    }

    @Test
    void missingAndNonPositiveTimingSamplesAreIgnored() {
        ChunkBuildMetrics metrics = new ChunkBuildMetrics(30);
        Object task = new Object();

        metrics.recordDequeued(task, 100L);
        metrics.recordEnqueued(task, 100L);
        metrics.recordDequeued(task, 100L);
        metrics.recordCompileDuration(0L);
        metrics.recordCompileDuration(-2L);
        metrics.recordUploadPassDuration(0L);

        assertEquals(0L, metrics.totalQueueWaitSamples());
        assertEquals(0L, metrics.totalCompileSamples());
        assertEquals(0L, metrics.totalUploadPassSamples());
    }

    @Test
    void usesTheRequestedRollingWindow() {
        ChunkBuildMetrics metrics = new ChunkBuildMetrics(2);
        metrics.recordCompileDuration(1L);
        metrics.recordCompileDuration(2L);
        metrics.recordCompileDuration(3L);

        assertEquals(3L, metrics.totalCompileSamples());
        assertEquals(2, metrics.compileDurationNanos().sampleCount());
        assertEquals(2.5, metrics.compileDurationNanos().summary().averageNanos());
    }
}
