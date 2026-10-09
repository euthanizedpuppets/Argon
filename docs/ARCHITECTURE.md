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

Target Minecraft 26.3 and Fabric Loader 0.19.5 for the bootstrap. Pin exact build tooling in the repository. Add Fabric API only after a compatible release is verified and a concrete API is required.

## Performance

Record baseline results before optimization patches. Keep changes only when repeatable tests show a measurable benefit without unacceptable visual, stability, or gameplay regressions.