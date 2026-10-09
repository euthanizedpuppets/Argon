package dev.argon;

import dev.argon.core.FeatureFlags;
import net.fabricmc.api.ClientModInitializer;

import java.util.logging.Logger;

/**
 * Client entry point. The bootstrap intentionally does not modify rendering
 * or chunk behavior; optimizations will be added after baseline profiling.
 */
public final class ArgonClient implements ClientModInitializer {
    public static final String MOD_ID = "argon";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        FeatureFlags.initializeDefaults();
        LOGGER.info("Argon 0.1 initialized; experimental renderer is disabled.");
    }
}