package dev.argon.performance;

import java.util.Objects;

/**
 * Measures the amount of time spent between start/end server-tick callbacks.
 *
 * This is tick-work duration, not the wall-clock interval between scheduled
 * ticks and not the time spent saving a world during shutdown.
 */
public final class ServerTickMonitor {
    private final FrameTimeTracker tracker;
    private boolean hasStart;
    private long startNanos;

    public ServerTickMonitor(FrameTimeTracker tracker) {
        this.tracker = Objects.requireNonNull(tracker, "tracker must not be null");
    }

    /** Call from the server thread at the start of each tick. */
    public void beginTick(long timestampNanos) {
        startNanos = timestampNanos;
        hasStart = true;
    }

    /**
     * Call from the server thread at the end of each tick. Long tick durations
     * are intentionally retained because they are useful lag diagnostics.
     */
    public void finishTick(long timestampNanos) {
        if (!hasStart) {
            return;
        }

        long duration = timestampNanos - startNanos;
        hasStart = false;
        if (duration > 0) {
            tracker.recordFrame(duration);
        }
    }
}
