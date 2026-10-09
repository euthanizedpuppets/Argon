package dev.argon.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Minimal in-memory feature flag registry for the bootstrap.
 * A flag is not proof that a feature has been implemented or is available.
 */
public final class FeatureFlags {
    private static final Map<ArgonFeature, Boolean> ENABLED =
            new EnumMap<>(ArgonFeature.class);

    private FeatureFlags() {
    }

    public static synchronized void initializeDefaults() {
        ENABLED.clear();
        for (ArgonFeature feature : ArgonFeature.values()) {
            ENABLED.put(feature, false);
        }
    }

    public static synchronized boolean isEnabled(ArgonFeature feature) {
        return ENABLED.getOrDefault(feature, false);
    }

    /**
     * Sets a requested flag. Callers must still verify that the implementation
     * is registered and safe for the current runtime.
     */
    public static synchronized void setEnabled(ArgonFeature feature, boolean enabled) {
        ENABLED.put(feature, enabled);
    }
}