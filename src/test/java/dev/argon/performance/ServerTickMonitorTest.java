package dev.argon.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ServerTickMonitorTest {
    @Test
    void recordsDurationsBetweenStartAndEndCallbacks() {
        FrameTimeTracker tracker = new FrameTimeTracker(4);
        ServerTickMonitor monitor = new ServerTickMonitor(tracker);

        monitor.beginTick(1_000L);
        monitor.finishTick(50_001_000L);
        monitor.beginTick(60_000_000L);
        monitor.finishTick(80_000_000L);

        assertArrayEquals(new long[]{50_000_000L, 20_000_000L}, tracker.snapshot());
        assertEquals(2, tracker.sampleCount());
    }

    @Test
    void retainsVeryLongTicksForStallDiagnosis() {
        FrameTimeTracker tracker = new FrameTimeTracker(3);
        ServerTickMonitor monitor = new ServerTickMonitor(tracker);

        monitor.beginTick(100L);
        monitor.finishTick(8_000_000_100L);

        assertArrayEquals(new long[]{8_000_000_000L}, tracker.snapshot());
    }

    @Test
    void ignoresEndWithoutStartAndNonPositiveDurations() {
        FrameTimeTracker tracker = new FrameTimeTracker(3);
        ServerTickMonitor monitor = new ServerTickMonitor(tracker);

        monitor.finishTick(100L);
        monitor.beginTick(200L);
        monitor.finishTick(200L);
        monitor.beginTick(300L);
        monitor.finishTick(250L);

        assertEquals(0, tracker.sampleCount());
    }

    @Test
    void startWithoutPriorEndReplacesTheOldStart() {
        FrameTimeTracker tracker = new FrameTimeTracker(3);
        ServerTickMonitor monitor = new ServerTickMonitor(tracker);

        monitor.beginTick(100L);
        monitor.beginTick(500L);
        monitor.finishTick(700L);

        assertArrayEquals(new long[]{200L}, tracker.snapshot());
    }

    @Test
    void rejectsNullTracker() {
        assertThrows(NullPointerException.class, () -> new ServerTickMonitor(null));
    }
}
