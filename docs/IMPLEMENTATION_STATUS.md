# Argon Implementation Status

## Bootstrap branch

`bootstrap/argon-0.1` contains the initial Fabric project metadata, cloud build workflow, and architecture documents. It is the comparison baseline for the next development branch.

## Core development branch

`dev/argon-core` builds on the bootstrap branch and adds pure-Java components that can be tested without launching Minecraft:

- validated configuration parsing and persistence;
- bounded rolling frame-time samples and percentile calculations;
- a bounded, deduplicating priority queue for chunk work;
- unit tests for those components.

These are primitives, not yet active Minecraft optimizations. The queue is not connected to Minecraft chunk rebuilds, and the frame tracker is not yet hooked into a verified frame lifecycle.

## Stability gates

1. GitHub Actions resolves the pinned toolchain and compiles the mod.
2. All unit tests pass on the hosted runner.
3. The packaged JAR artifact is produced.
4. Configuration failures safely fall back to defaults.
5. Queue tests cover capacity, deduplication, priority upgrades, cancellation, and stable priority ordering.
6. No feature is described as an FPS optimization until integrated and benchmarked against a controlled baseline.

## Next integration gate

After CI is green, integrate configuration loading into the Fabric client lifecycle and add a minimal diagnostic status command. Only then start a version-specific Minecraft integration for chunk task scheduling.
