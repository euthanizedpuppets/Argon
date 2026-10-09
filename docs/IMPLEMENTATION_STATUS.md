# Argon Renderer Prototype Status — Minecraft 26.2

## Branch isolation

`support/minecraft-26.2` is the compatibility/testing branch for Minecraft Java Edition 26.2. It has independent version metadata, artifact naming, and documentation. It does not change `dev/argon-core` or `main`.

## Included

- Validated configuration parsing and safe fallback for malformed properties;
- Bounded render-interval samples with min/average/max/P50/P95 summaries;
- Fabric `LevelRenderEvents.END_MAIN` integration when local metrics are enabled;
- Integrated-server tick-work durations from Fabric server tick callbacks when local metrics are enabled;
- A bounded, deduplicating priority queue for Argon's future pure-data tasks;
- An opt-in Mixin that prunes already-cancelled entries from Minecraft's native section-task queue;
- Stable linear compaction for random-access lists in the cancellation-pruning helper;
- Optional-cleanup metrics for scan counts, entries inspected/pruned, and average/maximum scan duration;
- Native section-task queue add/poll/successful-poll/clear counters and current/peak depth, guarded by `telemetry.enabled`;
- Rolling measurements for native queue wait, section mesh compile duration, and CPU wall time in terrain-buffer upload passes;
- Fabric metadata declares the packaged icon at `assets/argon/icon.png`, verified during the Gradle build;
- Guarded feature flags, `/argon status`, unit tests and a Gradle Mixin metadata/target declaration check;
- A cloud build that uploads only `argon-mc26.2-0.1.3.jar`.

The user reports that the default-settings Minecraft 26.2 client starts, `/argon status` works, and shutdown completes cleanly on the earlier candidate. Native queue cleanup remains disabled by default; its opt-in runtime behavior and performance have not yet been verified in-game. No FPS uplift is claimed.

Render intervals are elapsed times between world-render callbacks, not GPU timestamps, presentation timestamps, or a definitive FPS counter. Integrated tick-work durations are measured between server tick callbacks; they apply only to the single-player integrated server and do not measure shutdown saving. Local metrics are opt-in via `telemetry.enabled=true`; no remote reporting exists.

## Stability gates

1. GitHub Actions compiles against Minecraft 26.2 with all unit tests passing.
2. The packaged 26.2 JAR artifact is produced.
3. Mixin resource metadata and target declarations pass the Gradle verification task.
4. The game starts with cleanup disabled and `/argon status` works.
5. Integrated tick-work diagnostics show samples in a single-player world.
6. Chunk pipeline timings are diagnostic-only and never alter vanilla scheduling.
7. Cleanup scan-cost metrics appear only after the user opts in to cleanup and telemetry is enabled.
8. Do not claim FPS gains until repeatable, controlled comparisons support them.


## Experimental renderer observer branch

Branch: experiment/renderer-prototype; artifact version: 0.2.0-renderer-prototype.

Included: optional timings around vanilla ChunkSectionsToRender.renderGroup; opaque/translucent call counts; observed draw-group and draw-entry counters; /argon status output for average/P50/P95/max timing. The observer is gated by both telemetry and renderer.experimental.enabled.

Not included: an independent terrain renderer, custom render pipeline for chunk meshes, custom chunk mesh compilation, changed culling, changed batching, or any claimed performance optimization. The vanilla render method still owns drawing. This stage exists to quantify current CPU terrain submission cost on the user's hardware. Do not label this build as a custom renderer replacement.
