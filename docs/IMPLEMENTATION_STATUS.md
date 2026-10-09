# Argon Implementation Status

## Bootstrap branch

`bootstrap/argon-0.1` contains the initial Fabric project metadata, cloud build workflow, and architecture documents. It is the comparison baseline for the next development branch.

## Core development branch

`dev/argon-core` builds on the bootstrap branch and adds:

- validated configuration parsing and persistence with safe fallback for malformed properties;
- bounded rolling render-interval samples, with min/average/max/P50/P95 summary;
- integration with Minecraft 26.3's Fabric `LevelRenderEvents.END_MAIN` callback when local metrics are enabled;
- a bounded, deduplicating priority queue for Argon's future pure-data tasks;
- an opt-in Mixin that prunes already-cancelled entries from Minecraft 26.3's native section-task queue;
- guarded feature flags and the client-side `/argon status` diagnostic command;
- unit tests for configuration, frame monitoring, summary statistics, and the standalone queue-pruning helper.

Native queue cleanup is disabled by default. It leaves vanilla's distance-based selection and compile/recompile quota unchanged, but actual in-game compatibility and performance have not yet been verified. The feature does not claim an FPS uplift.

When `telemetry.enabled=false`, Argon does not register the world-render timing callback and does not retain the native queue-pruning counter. This setting is local-only telemetry; no remote reporting service is implemented.

World-pass samples are elapsed intervals between main level-render callbacks. They are not GPU timestamps, presentation timestamps, or a definitive FPS counter.

## Stability gates

1. GitHub Actions resolves the pinned toolchain and compiles the mod.
2. All unit tests pass on the hosted runner.
3. The packaged JAR artifact is produced.
4. Configuration parsing failures safely fall back to defaults.
5. Queue tests cover capacity, deduplication, priority upgrades, cancellation, stable priority ordering, and queue-cleanup order preservation.
6. Diagnostics distinguish available implementation from user opt-in and measured benefit.
7. No feature is described as an FPS optimization until integrated and benchmarked against a controlled baseline.

## Next integration gate

Compile and load the new opt-in native queue-cleanup Mixin in a Minecraft 26.3 client. Test rapid travel, chunk-heavy camera movement, recompile-heavy scenes, world transitions, and mixed mod environments. Compare queue sizes and frame-time samples with the setting off and on. Keep it disabled by default until that validation is complete.
