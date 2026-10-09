package dev.argon;

import dev.argon.config.ArgonConfig;
import dev.argon.core.FeatureFlags;
import dev.argon.performance.FrameTimeTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
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

    public static ArgonConfig config() {
        return config;
    }

    public static FrameTimeTracker frameTimes() {
        return frameTimes;
    }

    @Override
    public void onInitializeClient() {
        FeatureFlags.initializeDefaults();

        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("argon.properties");
        try {
            boolean existed = java.nio.file.Files.exists(configPath);
            config = ArgonConfig.load(configPath);
            if (!existed) {
                config.save(configPath);
            }
        } catch (IOException exception) {
            config = ArgonConfig.defaults();
            LOGGER.log(Level.WARNING,
                    "Could not load Argon configuration; using safe defaults.", exception);
        }

        frameTimes = new FrameTimeTracker(config.frameSampleWindow());
        LOGGER.info(() -> "Argon 0.1 initialized. "
                + "Experimental renderer: disabled; "
                + "chunk scheduler integration: not active; "
                + "telemetry setting: " + config.telemetryEnabled() + ".");
    }
}