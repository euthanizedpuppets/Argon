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

This is a low-cost diagnostic estimate of world-render-pass interval, not a GPU timestamp, presentation timestamp, or definitive FPS counter. It samples only while this render event runs. Duplicate/backward timestamps and gaps longer than five seconds are ignored so pauses do not pollute the rolling window. The first callback only establishes a baseline.

## Chunk scheduling investigation

Minecraft 26.3 already uses SectionTaskDynamicQueue in SectionRenderDispatcher. Its existing selector removes cancelled tasks while polling, prioritizes tasks by camera distance, and reserves a quota for recompiles versus initial compilation. Any Argon integration must preserve those vanilla correctness and fairness properties unless repeatable measurements justify changing them.

The Argon bounded queue remains a pure-Java scheduling primitive and is not connected to the native chunk task queue. Do not enqueue vanilla SectionTask objects into an independent worker pool: they carry world/section and buffer lifecycle requirements that must remain under Minecraft's dispatch ownership. The next chunk step is a narrow, version-specific adapter or a measured queue-cleanup patch, tested against 26.3 before it is marked available.

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

A flag is not proof that a feature is implemented. Diagnostics must distinguish unavailable, experimental, enabled, and active states. If optional feature initialization fails, disable it only when a safe fallback exists; do not silently swallow failures in core rendering code.

## Compatibility

Target Minecraft 26.3 and Fabric Loader 0.19.5 for the bootstrap. Pin exact build tooling in the repository. Fabric API is pinned to the Minecraft 26.3 line because the frame-interval hook uses the version-specific level-rendering API.

## Performance

Record baseline results before optimization patches. Keep changes only when repeatable tests show a measurable benefit without unacceptable visual, stability, or gameplay regressions.
