package dev.argon.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * In-memory feature registry. A requested flag never activates a feature until
 * a runtime-compatible implementation has explicitly registered availability.
 */
public final class FeatureFlags {
    public enum Status {
        UNAVAILABLE,
        DISABLED,
        ACTIVE
    }

    private static final Map<ArgonFeature, Boolean> REQUESTED =
            new EnumMap<>(ArgonFeature.class);
    private static final Map<ArgonFeature, Boolean> AVAILABLE =
            new EnumMap<>(ArgonFeature.class);

    private FeatureFlags() {
    }

    public static synchronized void initializeDefaults() {
        REQUESTED.clear();
        AVAILABLE.clear();
        for (ArgonFeature feature : ArgonFeature.values()) {
            REQUESTED.put(feature, false);
            AVAILABLE.put(feature, false);
        }
    }

    /** True only when a feature was requested and an implementation is available. */
    public static synchronized boolean isEnabled(ArgonFeature feature) {
        return isRequested(feature) && isAvailable(feature);
    }

    public static synchronized boolean isRequested(ArgonFeature feature) {
        return REQUESTED.getOrDefault(feature, false);
    }

    public static synchronized boolean isAvailable(ArgonFeature feature) {
        return AVAILABLE.getOrDefault(feature, false);
    }

    public static synchronized Status status(ArgonFeature feature) {
        if (!isAvailable(feature)) {
            return Status.UNAVAILABLE;
        }
        return isRequested(feature) ? Status.ACTIVE : Status.DISABLED;
    }

    /** Records user/config intent; it does not bypass availability checks. */
    public static synchronized void setEnabled(ArgonFeature feature, boolean enabled) {
        REQUESTED.put(feature, enabled);
    }

    /** Called only after a compatible implementation has initialized successfully. */
    public static synchronized void markAvailable(ArgonFeature feature, boolean available) {
        AVAILABLE.put(feature, available);
    }
}
