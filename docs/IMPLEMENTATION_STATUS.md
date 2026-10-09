# Argon Implementation Status

## Bootstrap branch

`bootstrap/argon-0.1` contains the initial Fabric project metadata, cloud build workflow, and architecture documents. It is the comparison baseline for the next development branch.

## Core development branch

`dev/argon-core` builds on the bootstrap branch and adds:

- validated configuration parsing and persistence with safe fallback for malformed properties;
- bounded rolling frame-time samples and percentile calculations;
- a bounded, deduplicating priority queue for chunk work;
- guarded feature flags that distinguish requested features from implemented/available features;
- the client-side `/argon status` diagnostic command;
- unit tests for the pure-Java components.

The queue is not connected to Minecraft chunk rebuilds, and the frame tracker is not yet hooked into a verified frame lifecycle. The diagnostic command reports those capabilities as unavailable rather than implying that they are active.

## Stability gates

1. GitHub Actions resolves the pinned toolchain and compiles the mod.
2. All unit tests pass on the hosted runner.
3. The packaged JAR artifact is produced.
4. Configuration parsing failures safely fall back to defaults.
5. Queue tests cover capacity, deduplication, priority upgrades, cancellation, and stable priority ordering.
6. The diagnostic command distinguishes implemented primitives from active optimizations.
7. No feature is described as an FPS optimization until integrated and benchmarked against a controlled baseline.

## Next integration gate

With diagnostics and safe config fallback in place, the next step is a version-specific integration investigation for Minecraft chunk rebuild scheduling. First map the 26.3 client chunk rebuild lifecycle and thread ownership, then add a narrow adapter with tests before replacing any vanilla scheduling behavior. Do not enable scheduling changes until the adapter is verified against the target runtime.
