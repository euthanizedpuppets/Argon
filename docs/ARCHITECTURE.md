# Argon 0.1 Architecture — Minecraft 26.2 test branch

## Goals

This branch targets Minecraft Java Edition 26.2 on Fabric. It is a separate compatibility/testing lane from the 26.3 development branch. Optimize measured bottlenecks while preserving gameplay correctness and isolating experimental changes.

## Modules

- Core: lifecycle, feature registry, configuration and initialization.
- Rendering: visibility work, terrain mesh handling and renderer experiments.
- Chunks: task priority, cancellation, rebuild deduplication and bounded scheduling.
- Memory: allocation measurements, bounded caches and reusable buffers.
- Performance: frame-time statistics, chunk latency and adaptive optional-work budgets.
- Compatibility: runtime capability checks and feature-specific fallbacks.
- Diagnostics: reports that distinguish implemented features from placeholders.

The initial repository uses one Gradle project and package boundaries. Split into subprojects only when that materially improves isolation or build maintenance.

## Frame interval monitoring

The client registers Fabric's LevelRenderEvents.END_MAIN event at the end of the main level-render pass. FrameTimeMonitor measures elapsed nanoseconds between successive callbacks and passes valid intervals to the bounded rolling FrameTimeTracker.

The monitor runs only when `telemetry.enabled=true`. Its summary computes min, average, max, P50, and P95 in one snapshot and sort when the user invokes `/argon status`. This is a diagnostic estimate of world-render-pass interval, not a GPU timestamp, presentation timestamp, or definitive FPS counter. It samples only while this render event runs. Duplicate/backward timestamps and gaps longer than five seconds are ignored so pauses do not pollute the rolling window. The first callback only establishes a baseline.

## Cancelled chunk-task cleanup

Minecraft's SectionTaskDynamicQueue already removes cancelled tasks while polling, prioritizes candidates by camera distance, and enforces a quota between initial compilation and recompilation. Argon does not replace that queue or alter the poll algorithm.

When the opt-in `chunks.cancelled-task-cleanup.enabled=true` setting is enabled, a client-only Mixin checks the native queue before new tasks are appended. If at least 32 tasks are queued, it runs a cleanup pass every 16 additions, removing entries whose vanilla cancellation flag is already set. Removing from the end preserves the order of surviving tasks. The original queue methods continue to own worker scheduling, task execution, and buffer lifecycle.

This is an experimental queue-hygiene hook, not a proven FPS optimization. It is disabled by default and must be tested in an actual 26.2 client before being recommended for normal play. The diagnostic count records how many cancelled entries were removed early, and that counter is not collected when local metrics are disabled.

## Graphics backend considerations

This compatibility branch is intended for a 26.2 client where the graphics backend choices are available for the user's system. Argon does not force a backend or issue raw OpenGL calls. Use the game's own graphics/backend setting, and verify that the selected backend actually starts on the installed driver stack.

## Thread ownership

- Access Minecraft world and client state only from the thread required by the relevant API.
- Worker tasks must receive immutable snapshots or data explicitly safe for concurrent access.
- GPU resources must follow the lifecycle and thread rules of Minecraft's renderer.
- Queues and caches must be bounded.
- Canceled or stale chunk work must not publish outdated results.
- Never disable essential game simulation to inflate FPS.

## Feature flags

A flag is not proof that a feature is beneficial. Diagnostics distinguish unavailable, disabled, and active implementations. A user opt-in does not bypass runtime availability checks.

## Compatibility

Target Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, Java 25, and Fabric Loom 1.17.21 in this branch. The 26.3 branch remains separate.

## Performance

Record baseline results before optimization patches. Keep changes only when repeatable tests show a measurable benefit without unacceptable visual, stability, or gameplay regressions.
