package dev.argon;

import dev.argon.chunks.BoundedPriorityTaskQueue;
import dev.argon.chunks.ChunkBuildMetrics;
import dev.argon.chunks.ChunkCleanupMetrics;
import dev.argon.chunks.NativeChunkQueueMetrics;
import dev.argon.config.ArgonConfig;
import dev.argon.core.ArgonFeature;
import dev.argon.core.FeatureFlags;
import dev.argon.performance.FrameTimeMonitor;
import dev.argon.performance.FrameTimeTracker;
import dev.argon.performance.ServerTickMonitor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client entry point. The bootstrap collects render-interval diagnostics and
 * optionally performs conservative cleanup of already-cancelled native chunk
 * tasks; it does not replace Minecraft's scheduler or renderer.
 */
public final class ArgonClient implements ClientModInitializer {
    public static final String MOD_ID = "argon";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    private static final ChunkCleanupMetrics CHUNK_CLEANUP_METRICS = new ChunkCleanupMetrics();
    private static final NativeChunkQueueMetrics NATIVE_CHUNK_QUEUE_METRICS =
            new NativeChunkQueueMetrics();

    private static ChunkBuildMetrics CHUNK_BUILD_METRICS =
            new ChunkBuildMetrics(ArgonConfig.defaults().frameSampleWindow());

    private static ArgonConfig config = ArgonConfig.defaults();
    private static FrameTimeTracker frameTimes = new FrameTimeTracker(
            ArgonConfig.defaults().frameSampleWindow());
    private static FrameTimeMonitor frameTimeMonitor = new FrameTimeMonitor(frameTimes);
    private static FrameTimeTracker integratedServerTickTimes = new FrameTimeTracker(
            ArgonConfig.defaults().frameSampleWindow());
    private static ServerTickMonitor serverTickMonitor = new ServerTickMonitor(integratedServerTickTimes);
    private static BoundedPriorityTaskQueue<String, Runnable> chunkQueue =
            new BoundedPriorityTaskQueue<>(ArgonConfig.defaults().maxQueuedChunkTasks());

    public static ArgonConfig config() {
        return config;
    }

    public static FrameTimeTracker frameTimes() {
        return frameTimes;
    }

    /** Tick-work duration samples are produced by the single-player integrated server only. */
    public static FrameTimeTracker integratedServerTickTimes() {
        return integratedServerTickTimes;
    }

    public static ChunkCleanupMetrics chunkCleanupMetrics() {
        return CHUNK_CLEANUP_METRICS;
    }

    public static NativeChunkQueueMetrics nativeChunkQueueMetrics() {
        return NATIVE_CHUNK_QUEUE_METRICS;
    }

    public static ChunkBuildMetrics chunkBuildMetrics() {
        return CHUNK_BUILD_METRICS;
    }

    public static void recordNativeChunkQueueAdd(Object task, int depth) {
        if (config.telemetryEnabled()) {
            NATIVE_CHUNK_QUEUE_METRICS.recordAdd(depth);
            CHUNK_BUILD_METRICS.recordEnqueued(task, System.nanoTime());
        }
    }

    public static void recordNativeChunkQueuePoll(
            Object task, boolean taskReturned, int depth) {
        if (config.telemetryEnabled()) {
            NATIVE_CHUNK_QUEUE_METRICS.recordPoll(taskReturned, depth);
            if (taskReturned) {
                CHUNK_BUILD_METRICS.recordDequeued(task, System.nanoTime());
            }
        }
    }

    public static void recordNativeChunkQueueClear(int entriesCleared) {
        if (config.telemetryEnabled()) {
            NATIVE_CHUNK_QUEUE_METRICS.recordClear(entriesCleared);
            CHUNK_BUILD_METRICS.clearPendingTasks();
        }
    }

    public static void recordSectionCompileDuration(long durationNanos) {
        if (config.telemetryEnabled()) {
            CHUNK_BUILD_METRICS.recordCompileDuration(durationNanos);
        }
    }

    public static void recordTerrainUploadPassDuration(long durationNanos) {
        if (config.telemetryEnabled()) {
            CHUNK_BUILD_METRICS.recordUploadPassDuration(durationNanos);
        }
    }

    public static void recordChunkCleanupScan(int inspected, int removed, long durationNanos) {
        if (config.telemetryEnabled()) {
            CHUNK_CLEANUP_METRICS.recordScan(inspected, removed, Math.max(0L, durationNanos));
        }
    }

    /**
     * Exposes the bounded scheduling primitive for future integration.
     * This queue is not the native Minecraft chunk task queue.
     */
    public static BoundedPriorityTaskQueue<String, Runnable> chunkQueue() {
        return chunkQueue;
    }

    @Override
    public void onInitializeClient() {
        FeatureFlags.initializeDefaults();
        CHUNK_CLEANUP_METRICS.reset();
        NATIVE_CHUNK_QUEUE_METRICS.reset();

        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("argon.properties");
        try {
            boolean existed = Files.exists(configPath);
            config = ArgonConfig.load(configPath);
            if (!existed) {
                config.save(configPath);
            }
        } catch (IOException | IllegalArgumentException exception) {
            config = ArgonConfig.defaults();
            LOGGER.log(Level.WARNING,
                    "Could not load Argon configuration; using safe defaults.", exception);
        }

        FeatureFlags.setEnabled(
                ArgonFeature.CHUNK_PRIORITY_SCHEDULING, config.chunkSchedulerEnabled());
        FeatureFlags.setEnabled(
                ArgonFeature.EXPERIMENTAL_RENDERER, config.experimentalRendererEnabled());

        // The required, version-pinned mixin is part of this client implementation.
        FeatureFlags.markAvailable(ArgonFeature.CANCELLED_CHUNK_TASK_CLEANUP, true);
        FeatureFlags.setEnabled(ArgonFeature.CANCELLED_CHUNK_TASK_CLEANUP,
                config.cancelledChunkTaskCleanupEnabled());

        frameTimes = new FrameTimeTracker(config.frameSampleWindow());
        frameTimeMonitor = new FrameTimeMonitor(frameTimes);
        frameTimeMonitor.reset();

        integratedServerTickTimes = new FrameTimeTracker(config.frameSampleWindow());
        CHUNK_BUILD_METRICS = new ChunkBuildMetrics(config.frameSampleWindow());
        serverTickMonitor = new ServerTickMonitor(integratedServerTickTimes);
        if (config.telemetryEnabled()) {
            FrameTimeMonitor activeFrameMonitor = frameTimeMonitor;
            ServerTickMonitor activeServerTickMonitor = serverTickMonitor;
            LevelRenderEvents.END_MAIN.register(
                    context -> activeFrameMonitor.recordFrameBoundary(System.nanoTime()));
            ServerTickEvents.START_SERVER_TICK.register(
                    server -> activeServerTickMonitor.beginTick(System.nanoTime()));
            ServerTickEvents.END_SERVER_TICK.register(
                    server -> activeServerTickMonitor.finishTick(System.nanoTime()));
        }

        chunkQueue = new BoundedPriorityTaskQueue<>(config.maxQueuedChunkTasks());

        ArgonCommands.register();

        LOGGER.info(() -> "Argon 0.1.2 initialized. "
                + "Cancelled chunk-task cleanup: "
                + FeatureFlags.status(ArgonFeature.CANCELLED_CHUNK_TASK_CLEANUP)
                + "; experimental renderer status: "
                + FeatureFlags.status(ArgonFeature.EXPERIMENTAL_RENDERER)
                + "; chunk scheduler status: "
                + FeatureFlags.status(ArgonFeature.CHUNK_PRIORITY_SCHEDULING)
                + "; local metrics enabled: " + config.telemetryEnabled()
                + (config.telemetryEnabled()
                    ? "; world-pass and integrated-server tick diagnostics enabled."
                    : "; local metrics collection disabled by configuration."));
    }
}
