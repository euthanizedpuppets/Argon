package dev.argon.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Small dependency-free configuration model. Parsing and validation stay
 * independent from Minecraft so they can be tested in ordinary unit tests.
 */
public final class ArgonConfig {
    public static final int MIN_QUEUE_CAPACITY = 16;
    public static final int MAX_QUEUE_CAPACITY = 4096;
    public static final int MIN_FRAME_WINDOW = 30;
    public static final int MAX_FRAME_WINDOW = 1200;

    private final boolean telemetryEnabled;
    private final boolean chunkSchedulerEnabled;
    private final boolean experimentalRendererEnabled;
    private final int maxQueuedChunkTasks;
    private final int frameSampleWindow;
    private final boolean cancelledChunkTaskCleanupEnabled;
    private final boolean guiIntersectionProfilingEnabled;
    private final boolean chunkGenerationProfilingEnabled;

    /**
     * Compatibility constructor: experimental chunk cleanup remains disabled
     * unless the caller explicitly opts in through the full constructor.
     */
    public ArgonConfig(
            boolean telemetryEnabled,
            boolean chunkSchedulerEnabled,
            boolean experimentalRendererEnabled,
            int maxQueuedChunkTasks,
            int frameSampleWindow) {
        this(telemetryEnabled, chunkSchedulerEnabled, experimentalRendererEnabled,
                maxQueuedChunkTasks, frameSampleWindow, false, false, false);
    }

    public ArgonConfig(
            boolean telemetryEnabled,
            boolean chunkSchedulerEnabled,
            boolean experimentalRendererEnabled,
            int maxQueuedChunkTasks,
            int frameSampleWindow,
            boolean cancelledChunkTaskCleanupEnabled) {
        this(telemetryEnabled, chunkSchedulerEnabled, experimentalRendererEnabled,
                maxQueuedChunkTasks, frameSampleWindow, cancelledChunkTaskCleanupEnabled,
                false, false);
    }

    public ArgonConfig(
            boolean telemetryEnabled,
            boolean chunkSchedulerEnabled,
            boolean experimentalRendererEnabled,
            int maxQueuedChunkTasks,
            int frameSampleWindow,
            boolean cancelledChunkTaskCleanupEnabled,
            boolean guiIntersectionProfilingEnabled,
            boolean chunkGenerationProfilingEnabled) {
        this.telemetryEnabled = telemetryEnabled;
        this.chunkSchedulerEnabled = chunkSchedulerEnabled;
        this.experimentalRendererEnabled = experimentalRendererEnabled;
        this.maxQueuedChunkTasks = clamp(
                maxQueuedChunkTasks, MIN_QUEUE_CAPACITY, MAX_QUEUE_CAPACITY);
        this.frameSampleWindow = clamp(
                frameSampleWindow, MIN_FRAME_WINDOW, MAX_FRAME_WINDOW);
        this.cancelledChunkTaskCleanupEnabled = cancelledChunkTaskCleanupEnabled;
        this.guiIntersectionProfilingEnabled = guiIntersectionProfilingEnabled;
        this.chunkGenerationProfilingEnabled = chunkGenerationProfilingEnabled;
    }

    public static ArgonConfig defaults() {
        return new ArgonConfig(true, false, false, 256, 240, false, false, false);
    }

    public boolean telemetryEnabled() {
        return telemetryEnabled;
    }

    public boolean chunkSchedulerEnabled() {
        return chunkSchedulerEnabled;
    }

    public boolean experimentalRendererEnabled() {
        return experimentalRendererEnabled;
    }

    public int maxQueuedChunkTasks() {
        return maxQueuedChunkTasks;
    }

    public int frameSampleWindow() {
        return frameSampleWindow;
    }

    public boolean cancelledChunkTaskCleanupEnabled() {
        return cancelledChunkTaskCleanupEnabled;
    }

    public boolean guiIntersectionProfilingEnabled() {
        return guiIntersectionProfilingEnabled;
    }

    public boolean chunkGenerationProfilingEnabled() {
        return chunkGenerationProfilingEnabled;
    }

    public static ArgonConfig load(Path file) throws IOException {
        ArgonConfig defaults = defaults();
        if (!Files.exists(file)) {
            return defaults;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IllegalArgumentException malformedProperties) {
            // Properties.load throws this for malformed Unicode escape sequences.
            return defaults;
        }

        return new ArgonConfig(
                readBoolean(properties, "telemetry.enabled", defaults.telemetryEnabled),
                readBoolean(properties, "chunks.scheduler.enabled", defaults.chunkSchedulerEnabled),
                readBoolean(properties, "renderer.experimental.enabled",
                        defaults.experimentalRendererEnabled),
                readInt(properties, "chunks.queue.capacity", defaults.maxQueuedChunkTasks),
                readInt(properties, "performance.frame-window", defaults.frameSampleWindow),
                readBoolean(properties, "chunks.cancelled-task-cleanup.enabled",
                        defaults.cancelledChunkTaskCleanupEnabled),
                readBoolean(properties, "performance.gui-intersection.enabled",
                        defaults.guiIntersectionProfilingEnabled),
                readBoolean(properties, "chunks.generation-profiling.enabled",
                        defaults.chunkGenerationProfilingEnabled));
    }

    public void save(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Properties properties = new Properties();
        properties.setProperty("telemetry.enabled", Boolean.toString(telemetryEnabled));
        properties.setProperty("chunks.scheduler.enabled", Boolean.toString(chunkSchedulerEnabled));
        properties.setProperty("renderer.experimental.enabled",
                Boolean.toString(experimentalRendererEnabled));
        properties.setProperty("chunks.queue.capacity", Integer.toString(maxQueuedChunkTasks));
        properties.setProperty("performance.frame-window", Integer.toString(frameSampleWindow));
        properties.setProperty("chunks.cancelled-task-cleanup.enabled",
                Boolean.toString(cancelledChunkTaskCleanupEnabled));
        properties.setProperty("performance.gui-intersection.enabled",
                Boolean.toString(guiIntersectionProfilingEnabled));
        properties.setProperty("chunks.generation-profiling.enabled",
                Boolean.toString(chunkGenerationProfilingEnabled));

        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output,
                    "Argon configuration. Experimental cleanup is disabled by default.");
        }
    }

    private static boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }
        return fallback;
    }

    private static int readInt(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
