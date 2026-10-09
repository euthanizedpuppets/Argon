# Argon Implementation Status

## Bootstrap branch

bootstrap/argon-0.1 contains the initial Fabric project metadata, cloud build workflow, and architecture documents. It is the comparison baseline for the next development branch.

## Core development branch

dev/argon-core builds on the bootstrap branch and adds:

- validated configuration parsing and persistence with safe fallback for malformed properties;
- bounded rolling frame-time samples and percentile calculations;
- integration with Minecraft 26.3's Fabric LevelRenderEvents.END_MAIN callback to collect world-pass interval samples;
- a bounded, deduplicating priority queue for chunk work;
- guarded feature flags that distinguish requested features from implemented/available features;
- the client-side /argon status diagnostic command;
- unit tests for pure-Java components, including render interval sampling.

The render samples are elapsed intervals between main level-render callbacks. They are not GPU timestamps, presentation timestamps, or a definitive FPS counter. They are collected only while world rendering is active. The queue is not connected to Minecraft chunk rebuilds, and no chunk scheduling optimization is marked available.

## Stability gates

1. GitHub Actions resolves the pinned toolchain and compiles the mod.
2. All unit tests pass on the hosted runner.
3. The packaged JAR artifact is produced.
4. Configuration parsing failures safely fall back to defaults.
5. Queue tests cover capacity, deduplication, priority upgrades, cancellation, and stable priority ordering.
6. The diagnostic command distinguishes implemented primitives from active optimizations.
7. No feature is described as an FPS optimization until integrated and benchmarked against a controlled baseline.

## Next integration gate

Investigate Minecraft 26.3's native SectionTaskDynamicQueue, which already handles camera-distance ordering, cancellation cleanup during poll, and a quota between initial compile and recompile tasks. Any future Argon patch must preserve section-task lifecycle and worker/buffer ownership. Prefer a narrow, testable cleanup or prioritization change over replacing the native scheduler wholesale.
