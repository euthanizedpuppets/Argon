package dev.argon.performance;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Opt-in timing for NoiseBasedChunkGenerator.doFill on generation workers.
 * It measures only this terrain-density/fill stage, not the full chunk status
 * pipeline, decoration, lighting, disk I/O, or time spent waiting for a stage.
 */
public final class ChunkGenerationMetrics {
    private final FrameTimeTracker durationWindow;
    private final LongAdder completedFills = new LongAdder();
    private final LongAdder totalDurationNanos = new LongAdder();
    private final AtomicLong maximumDurationNanos = new AtomicLong();
    private final ConcurrentHashMap<String, LongAdder> fillsByThread = new ConcurrentHashMap<>();
    private final ThreadLocal<ArrayDeque<Long>> activeStarts =
            ThreadLocal.withInitial(ArrayDeque::new);

    public ChunkGenerationMetrics(int sampleWindow) {
        this.durationWindow = new FrameTimeTracker(sampleWindow);
    }

    public void beginFill() {
        activeStarts.get().push(System.nanoTime());
    }

    public void finishFill() {
        ArrayDeque<Long> stack = activeStarts.get();
        Long startedAt = stack.poll();
        if (stack.isEmpty()) {
            activeStarts.remove();
        }
        if (startedAt == null) {
            return;
        }

        long elapsed = Math.max(1L, System.nanoTime() - startedAt);
        completedFills.increment();
        totalDurationNanos.add(elapsed);
        maximumDurationNanos.accumulateAndGet(elapsed, Math::max);
        durationWindow.recordFrame(elapsed);
        fillsByThread.computeIfAbsent(Thread.currentThread().getName(), ignored -> new LongAdder())
                .increment();
    }

    public Snapshot snapshot() {
        Map<String, Long> threadCounts = fillsByThread.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().sum()));
        Map<String, Long> topThreads = threadCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, java.util.LinkedHashMap::new));
        return new Snapshot(
                completedFills.sum(),
                totalDurationNanos.sum(),
                maximumDurationNanos.get(),
                durationWindow.summary(),
                topThreads);
    }

    public record Snapshot(
            long completedFills,
            long cumulativeDurationNanos,
            long maximumDurationNanos,
            FrameTimeTracker.Summary recentDurations,
            Map<String, Long> topWorkerCalls) {
    }
}