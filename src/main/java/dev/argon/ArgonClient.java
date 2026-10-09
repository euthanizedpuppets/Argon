package dev.argon;

import dev.argon.chunks.BoundedPriorityTaskQueue;
import dev.argon.config.ArgonConfig;
import dev.argon.core.ArgonFeature;
import dev.argon.core.FeatureFlags;
import dev.argon.performance.FrameTimeTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client entry point. The bootstrap intentionally does not modify rendering
 * or chunk behavior; optimizations will be added after baseline profiling.
 */
public final class ArgonClient implements ClientModInitializer {
    public static final String MOD_ID = "argon";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    private static ArgonConfig config = ArgonConfig.defaults();
    private static FrameTimeTracker frameTimes = new FrameTimeTracker(
            ArgonConfig.defaults().frameSampleWindow());
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
     * It is not connected to Minecraft chunk rebuilds in this bootstrap.
     */
    public static BoundedPriorityTaskQueue<String, Runnable> chunkQueue() {
        return chunkQueue;
    }

    @Override
    public void onInitializeClient() {
        FeatureFlags.initializeDefaults();

        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("argon.properties");
        try {
            boolean existed = Files.exists(configPath);
            config = ArgonConfig.load(configPath);
            if (!existed) {
                config.save(configPath);
            }
        } catch (IOException exception) {
            config = ArgonConfig.defaults();
            LOGGER.log(Level.WARNING,
                    "Could not load Argon configuration; using safe defaults.", exception);
        }

        FeatureFlags.setEnabled(
                ArgonFeature.CHUNK_PRIORITY_SCHEDULING, config.chunkSchedulerEnabled());
        FeatureFlags.setEnabled(
                ArgonFeature.EXPERIMENTAL_RENDERER, config.experimentalRendererEnabled());

        frameTimes = new FrameTimeTracker(config.frameSampleWindow());
        chunkQueue = new BoundedPriorityTaskQueue<>(config.maxQueuedChunkTasks());

        LOGGER.info(() -> "Argon 0.1 initialized. "
                + "Experimental renderer status: "
                + FeatureFlags.status(ArgonFeature.EXPERIMENTAL_RENDERER)
                + "; chunk scheduler status: "
                + FeatureFlags.status(ArgonFeature.CHUNK_PRIORITY_SCHEDULING)
                + "; local metrics enabled: " + config.telemetryEnabled() + ".");
    }
}