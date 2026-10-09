package dev.argon.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FrameTimeTrackerTest {
    @Test
    void ignoresInvalidSamplesAndTracksPercentiles() {
        FrameTimeTracker tracker = new FrameTimeTracker(4);
        tracker.recordFrame(0);
        tracker.recordFrame(-1);
        tracker.recordFrame(40);
        tracker.recordFrame(10);
        tracker.recordFrame(30);
        tracker.recordFrame(20);

        assertEquals(4, tracker.sampleCount());
        assertArrayEquals(new long[]{40, 10, 30, 20}, tracker.snapshot());
        assertEquals(10, tracker.percentile(0.25));
        assertEquals(20, tracker.percentile(0.50));
        assertEquals(40, tracker.percentile(1.0));
        assertEquals(10, tracker.percentile(-1.0));
    }

    @Test
    void snapshotRemainsChronologicalAfterRingWraps() {
        FrameTimeTracker tracker = new FrameTimeTracker(3);
        tracker.recordFrame(10);
        tracker.recordFrame(20);
        tracker.recordFrame(30);
        tracker.recordFrame(40);

        assertArrayEquals(new long[]{20, 30, 40}, tracker.snapshot());
    }

    @Test
    void emptyTrackerReturnsZero() {
        FrameTimeTracker tracker = new FrameTimeTracker(3);
        assertEquals(0, tracker.percentile(0.95));
        assertEquals(0, tracker.sampleCount());
    }

    @Test
    void resetClearsSamples() {
        FrameTimeTracker tracker = new FrameTimeTracker(2);
        tracker.recordFrame(100);
        tracker.reset();

        assertEquals(0, tracker.sampleCount());
        assertArrayEquals(new long[0], tracker.snapshot());
    }

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new FrameTimeTracker(0));
    }
}