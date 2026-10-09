package dev.argon.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FeatureFlagsTest {
    @BeforeEach
    void resetRegistry() {
        FeatureFlags.initializeDefaults();
    }

    @Test
    void allFeaturesStartUnavailableAndDisabled() {
        for (ArgonFeature feature : ArgonFeature.values()) {
            assertFalse(FeatureFlags.isRequested(feature));
            assertFalse(FeatureFlags.isAvailable(feature));
            assertFalse(FeatureFlags.isEnabled(feature));
            assertEquals(FeatureFlags.Status.UNAVAILABLE, FeatureFlags.status(feature));
        }
    }

    @Test
    void requestedFeatureDoesNotActivateUntilImplementationIsAvailable() {
        FeatureFlags.setEnabled(ArgonFeature.EXPERIMENTAL_RENDERER, true);

        assertTrue(FeatureFlags.isRequested(ArgonFeature.EXPERIMENTAL_RENDERER));
        assertFalse(FeatureFlags.isEnabled(ArgonFeature.EXPERIMENTAL_RENDERER));
        assertEquals(FeatureFlags.Status.UNAVAILABLE,
                FeatureFlags.status(ArgonFeature.EXPERIMENTAL_RENDERER));

        FeatureFlags.markAvailable(ArgonFeature.EXPERIMENTAL_RENDERER, true);

        assertTrue(FeatureFlags.isEnabled(ArgonFeature.EXPERIMENTAL_RENDERER));
        assertEquals(FeatureFlags.Status.ACTIVE,
                FeatureFlags.status(ArgonFeature.EXPERIMENTAL_RENDERER));
    }

    @Test
    void disablingAvailableFeatureKeepsItInactive() {
        FeatureFlags.markAvailable(ArgonFeature.CHUNK_REBUILD_DEDUPLICATION, true);
        FeatureFlags.setEnabled(ArgonFeature.CHUNK_REBUILD_DEDUPLICATION, true);
        assertTrue(FeatureFlags.isEnabled(ArgonFeature.CHUNK_REBUILD_DEDUPLICATION));

        FeatureFlags.setEnabled(ArgonFeature.CHUNK_REBUILD_DEDUPLICATION, false);

        assertFalse(FeatureFlags.isEnabled(ArgonFeature.CHUNK_REBUILD_DEDUPLICATION));
        assertEquals(FeatureFlags.Status.DISABLED,
                FeatureFlags.status(ArgonFeature.CHUNK_REBUILD_DEDUPLICATION));
    }
}