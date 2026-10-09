package dev.argon.performance;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Opt-in measurements for GuiRenderState.hasIntersection.
 *
 * The candidate count records the size of the supplied ScreenArea list, not
 * the exact number of entries the vanilla loop visits before returning.
 */
public final class GuiIntersectionMetrics {
    private final FrameTimeTracker durationsNanos;
    private final FrameTimeTracker candidateListSizes;
    private final LongAdder totalCalls = new LongAdder();
    private final LongAdder intersectingCalls = new LongAdder();
    private final LongAdder totalCandidateEntries = new LongAdder();
    private final AtomicInteger maximumCandidateListSize = new AtomicInteger();
    private final ThreadLocal<ArrayDeque<Start>> activeStarts =
            ThreadLocal.withInitial(ArrayDeque::new);

    public GuiIntersectionMetrics(int sampleWindow) {
        this.durationsNanos = new FrameTimeTracker(sampleWindow);
        this.candidateListSizes = new FrameTimeTracker(sampleWindow);
    }

    /** Starts one sample; thread-local storage avoids cross-thread interference. */
    public void begin(int candidateListSize) {
        int safeSize = Math.max(0, candidateListSize);
        activeStarts.get().push(new Start(System.nanoTime(), safeSize));
    }

    /** Completes the most recent call on this thread. */
    public void finish(boolean intersects) {
        ArrayDeque<Start> stack = activeStarts.get();
        Start start = stack.poll();
        if (stack.isEmpty()) {
            activeStarts.remove();
        }
        if (start == null) {
            return;
        }

        long elapsed = Math.max(1L, System.nanoTime() - start.startedAtNanos());
        totalCalls.increment();
        if (intersects) {
            intersectingCalls.increment();
        }
        totalCandidateEntries.add(start.candidateListSize());
        maximumCandidateListSize.accumulateAndGet(start.candidateListSize(), Math::max);

        durationsNanos.recordFrame(elapsed);
        // FrameTimeTracker ignores zero; offset sizes by one and subtract one
        // when displaying summary values so empty lists remain represented.
        candidateListSizes.recordFrame((long) start.candidateListSize() + 1L);
    }

    public Snapshot snapshot() {
        return new Snapshot(
                totalCalls.sum(),
                intersectingCalls.sum(),
                totalCandidateEntries.sum(),
                maximumCandidateListSize.get(),
                durationsNanos.summary(),
                candidateListSizes.summary());
    }

    public record Snapshot(
            long totalCalls,
            long intersectingCalls,
            long totalCandidateEntries,
            int maximumCandidateListSize,
            FrameTimeTracker.Summary durationWindow,
            FrameTimeTracker.Summary candidateListSizeWindow) {
    }

    private record Start(long startedAtNanos, int candidateListSize) {
    }
}