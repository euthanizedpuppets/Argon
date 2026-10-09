package dev.argon;

import dev.argon.chunks.BoundedPriorityTaskQueue;
import dev.argon.config.ArgonConfig;
import dev.argon.core.ArgonFeature;
import dev.argon.core.FeatureFlags;
import dev.argon.performance.FrameTimeMonitor;
import dev.argon.performance.FrameTimeTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
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

    private static final AtomicLong CANCELLED_CHUNK_TASKS_PRUNED = new AtomicLong();

    private static ArgonConfig config = ArgonConfig.defaults();
    private static FrameTimeTracker frameTimes = new FrameTimeTracker(
            ArgonConfig.defaults().frameSampleWindow());
    private static FrameTimeMonitor frameTimeMonitor = new FrameTimeMonitor(frameTimes);
    private static BoundedPriorityTaskQueue<String, Runnable> chunkQueue =
            new BoundedPriorityTaskQueue<>(ArgonConfig.defaults().maxQueuedChunkTasks());

    public static ArgonConfig config() {
        return config;
    }

    public static FrameTimeTracker frameTimes() {
        return frameTimes;
    }

    /**
     * Exposes the bounded scheduling primitive for future integration.
     * This queue is not the native Minecraft chunk task queue.
     */
    public static BoundedPriorityTaskQueue<String, Runnable> chunkQueue() {
        return chunkQueue;
    }

    public static void recordCancelledChunkTasksPruned(int count) {
        if (count > 0) {
            CANCELLED_CHUNK_TASKS_PRUNED.addAndGet(count);
        }
    }

    public static long cancelledChunkTasksPruned() {
        return CANCELLED_CHUNK_TASKS_PRUNED.get();
    }

    @Override
    public void onInitializeClient() {
        FeatureFlags.initializeDefaults();
        CANCELLED_CHUNK_TASKS_PRUNED.set(0L);

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
        LevelRenderEvents.END_MAIN.register(
                context -> frameTimeMonitor.recordFrameBoundary(System.nanoTime()));

        chunkQueue = new BoundedPriorityTaskQueue<>(config.maxQueuedChunkTasks());

        ArgonCommands.register();

        LOGGER.info(() -> "Argon 0.1 initialized. "
                + "Cancelled chunk-task cleanup: "
                + FeatureFlags.status(ArgonFeature.CANCELLED_CHUNK_TASK_CLEANUP)
                + "; experimental renderer status: "
                + FeatureFlags.status(ArgonFeature.EXPERIMENTAL_RENDERER)
                + "; chunk scheduler status: "
                + FeatureFlags.status(ArgonFeature.CHUNK_PRIORITY_SCHEDULING)
                + "; local metrics enabled: " + config.telemetryEnabled()
                + "; world-pass interval monitoring enabled.");
    }
}
