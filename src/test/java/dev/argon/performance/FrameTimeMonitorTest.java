package dev.argon.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FrameTimeMonitorTest {
    @Test
    void firstBoundaryEstablishesBaselineAndLaterBoundariesRecordIntervals() {
        FrameTimeTracker tracker = new FrameTimeTracker(4);
        FrameTimeMonitor monitor = new FrameTimeMonitor(tracker);

        monitor.recordFrameBoundary(1_000L);
        assertEquals(0, tracker.sampleCount());

        monitor.recordFrameBoundary(16_667_000L);
        monitor.recordFrameBoundary(33_334_000L);

        assertArrayEquals(new long[]{16_666_000L, 16_667_000L}, tracker.snapshot());
    }

    @Test
    void ignoresDuplicateBackwardAndPauseSizedIntervals() {
        FrameTimeTracker tracker = new FrameTimeTracker(5);
        FrameTimeMonitor monitor = new FrameTimeMonitor(tracker);

        monitor.recordFrameBoundary(100L);
        monitor.recordFrameBoundary(100L);
        monitor.recordFrameBoundary(90L);
        monitor.recordFrameBoundary(200L);
        monitor.recordFrameBoundary(200L + FrameTimeMonitor.MAX_SAMPLE_INTERVAL_NANOS + 1L);
        monitor.recordFrameBoundary(300L + FrameTimeMonitor.MAX_SAMPLE_INTERVAL_NANOS + 1L);

        assertArrayEquals(new long[]{110L, 100L}, tracker.snapshot());
    }

    @Test
    void resetClearsSamplesAndBoundaryBaseline() {
        FrameTimeTracker tracker = new FrameTimeTracker(4);
        FrameTimeMonitor monitor = new FrameTimeMonitor(tracker);

        monitor.recordFrameBoundary(10L);
        monitor.recordFrameBoundary(20L);
        monitor.reset();

        monitor.recordFrameBoundary(500L);
        assertEquals(0, tracker.sampleCount());

        monitor.recordFrameBoundary(700L);
        assertArrayEquals(new long[]{200L}, tracker.snapshot());
    }

    @Test
    void rejectsNullTracker() {
        assertThrows(NullPointerException.class, () -> new FrameTimeMonitor(null));
    }
}
