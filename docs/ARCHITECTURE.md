# Argon 0.1 Architecture

## Goals

Argon is a Java-first Fabric client optimization suite for Minecraft Java Edition 26.3. Optimize measured bottlenecks while preserving gameplay correctness and isolating experimental changes.

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

The client registers Fabric's LevelRenderEvents.END_MAIN event at the end of Minecraft 26.3's main level-render pass. FrameTimeMonitor measures elapsed nanoseconds between successive callbacks and passes valid intervals to the bounded rolling FrameTimeTracker.

The monitor runs only when `telemetry.enabled=true`. Its summary computes min, average, max, P50, and P95 in one snapshot and sort when the user invokes `/argon status`. This is a diagnostic estimate of world-render-pass interval, not a GPU timestamp, presentation timestamp, or definitive FPS counter. It samples only while this render event runs. Duplicate/backward timestamps and gaps longer than five seconds are ignored so pauses do not pollute the rolling window. The first callback only establishes a baseline.

## Cancelled chunk-task cleanup

Minecraft 26.3's SectionTaskDynamicQueue already removes cancelled tasks while polling, prioritizes candidates by camera distance, and enforces a quota between initial compilation and recompilation. Argon does not replace that queue or alter the poll algorithm.

When the opt-in `chunks.cancelled-task-cleanup.enabled=true` setting is enabled, a client-only Mixin checks the native queue before new tasks are appended. If at least 32 tasks are queued, it runs a cleanup pass every 16 additions, removing entries whose vanilla cancellation flag is already set. Removing from the end preserves the order of surviving tasks. The original queue methods continue to own worker scheduling, task execution, and buffer lifecycle.

This is an experimental queue-hygiene hook, not a proven FPS optimization. It is disabled by default and must be tested in an actual 26.3 client before being recommended for normal play. The diagnostic count records how many cancelled entries were removed early, and that counter is not collected when local metrics are disabled.

## OpenGL and Vulkan

Argon must work with Minecraft's supported OpenGL and Vulkan runtime paths when the target release supports them. Shared code should use Minecraft rendering abstractions rather than direct OpenGL calls.

Backend detection must use a verified API for the selected Minecraft release. Do not infer the active backend from the operating system, GPU vendor, or a guessed setting.

The initial release does not implement a custom Vulkan renderer or force a backend switch. Renderer experiments remain disabled by default until a separate design documents resource ownership, synchronization, presentation, fallback, and compatibility.

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

Target Minecraft 26.3 and Fabric Loader 0.19.5 for the bootstrap. Pin exact build tooling in the repository. Fabric API is pinned to the Minecraft 26.3 line because the frame-interval hook uses the version-specific level-rendering API.

## Performance

Record baseline results before optimization patches. Keep changes only when repeatable tests show a measurable benefit without unacceptable visual, stability, or gameplay regressions.
