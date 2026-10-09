package dev.argon.performance;

public final class FrameTimeMonitor {
    /** Gaps longer than this are treated as pauses/stalls, not normal frame intervals. */
    public static final long MAX_SAMPLE_INTERVAL_NANOS = 5_000_000_000L;

    private final FrameTimeTracker tracker;
    private boolean hasPreviousBoundary;
    private long previousBoundaryNanos;

    public FrameTimeMonitor(FrameTimeTracker tracker) {
        if (tracker == null) {
            throw new NullPointerException("tracker must not be null");
        }
        this.tracker = tracker;
    }

    /**
     * Records a stable render-phase boundary. The first boundary establishes
     * a baseline; later boundaries contribute elapsed intervals.
     */
    public synchronized void recordFrameBoundary(long timestampNanos) {
        if (hasPreviousBoundary) {
            long interval = timestampNanos - previousBoundaryNanos;
            if (interval > 0 && interval <= MAX_SAMPLE_INTERVAL_NANOS) {
                tracker.recordFrame(interval);
            }
        }

        previousBoundaryNanos = timestampNanos;
        hasPreviousBoundary = true;
    }

    /** Resets both the rolling samples and the boundary baseline. */
    public synchronized void reset() {
        hasPreviousBoundary = false;
        previousBoundaryNanos = 0L;
        tracker.reset();
    }
}
