package dev.argon.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ArgonConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void defaultsKeepExperimentalFeaturesDisabled() {
        ArgonConfig config = ArgonConfig.defaults();

        assertTrue(config.telemetryEnabled());
        assertFalse(config.chunkSchedulerEnabled());
        assertFalse(config.experimentalRendererEnabled());
        assertFalse(config.cancelledChunkTaskCleanupEnabled());
        assertFalse(config.guiIntersectionProfilingEnabled());
        assertFalse(config.chunkGenerationProfilingEnabled());
        assertEquals(256, config.maxQueuedChunkTasks());
        assertEquals(240, config.frameSampleWindow());
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        Path file = temporaryDirectory.resolve("nested/argon.properties");
        ArgonConfig expected = new ArgonConfig(true, true, false, 512, 300, true, true, true);

        expected.save(file);
        ArgonConfig actual = ArgonConfig.load(file);

        assertTrue(actual.telemetryEnabled());
        assertTrue(actual.chunkSchedulerEnabled());
        assertFalse(actual.experimentalRendererEnabled());
        assertTrue(actual.cancelledChunkTaskCleanupEnabled());
        assertTrue(actual.guiIntersectionProfilingEnabled());
        assertTrue(actual.chunkGenerationProfilingEnabled());
        assertEquals(512, actual.maxQueuedChunkTasks());
        assertEquals(300, actual.frameSampleWindow());
    }

    @Test
    void invalidValuesFallBackAndNumbersAreClamped() throws IOException {
        Path file = temporaryDirectory.resolve("argon.properties");
        Files.writeString(file, """
                telemetry.enabled=maybe
                chunks.scheduler.enabled=true
                renderer.experimental.enabled=false
                chunks.queue.capacity=999999
                performance.frame-window=not-a-number
                chunks.cancelled-task-cleanup.enabled=maybe
                performance.gui-intersection.enabled=invalid
                chunks.generation-profiling.enabled=invalid
                """);

        ArgonConfig config = ArgonConfig.load(file);

        assertTrue(config.telemetryEnabled());
        assertTrue(config.chunkSchedulerEnabled());
        assertFalse(config.cancelledChunkTaskCleanupEnabled());
        assertFalse(config.guiIntersectionProfilingEnabled());
        assertFalse(config.chunkGenerationProfilingEnabled());
        assertEquals(ArgonConfig.MAX_QUEUE_CAPACITY, config.maxQueuedChunkTasks());
        assertEquals(ArgonConfig.defaults().frameSampleWindow(), config.frameSampleWindow());
    }

    @Test
    void malformedUnicodeEscapeFallsBackToDefaults() throws IOException {
        Path file = temporaryDirectory.resolve("malformed.properties");
        Files.writeString(file, "telemetry.enabled=\\u12G4\nchunks.scheduler.enabled=true\n");

        ArgonConfig config = ArgonConfig.load(file);

        assertEquals(ArgonConfig.defaults().telemetryEnabled(), config.telemetryEnabled());
        assertEquals(ArgonConfig.defaults().chunkSchedulerEnabled(), config.chunkSchedulerEnabled());
        assertEquals(ArgonConfig.defaults().maxQueuedChunkTasks(), config.maxQueuedChunkTasks());
        assertFalse(config.cancelledChunkTaskCleanupEnabled());
        assertFalse(config.guiIntersectionProfilingEnabled());
        assertFalse(config.chunkGenerationProfilingEnabled());
    }

    @Test
    void missingFileReturnsDefaults() throws IOException {
        ArgonConfig config = ArgonConfig.load(temporaryDirectory.resolve("missing.properties"));
        assertFalse(config.experimentalRendererEnabled());
        assertFalse(config.cancelledChunkTaskCleanupEnabled());
    }
}
