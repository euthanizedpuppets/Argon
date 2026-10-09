# Argon — Minecraft 26.2 performance diagnostics

This branch targets Minecraft Java Edition **26.2** on Fabric. It remains isolated from the 26.3 development branch.

## Cloud build

GitHub Actions uses Java 25 and Gradle 9.6.0. The current artifact is `argon-mc26.2-0.1.5.jar`.

## Install requirements

- Minecraft Java Edition 26.2
- Java 25
- Fabric Loader 0.19.5 or newer
- Fabric API 0.161.0+26.2 or newer for this game version

Use a separate Fabric instance and a disposable world for diagnosis. Back up important worlds first.

## 0.1.5: correct Minecraft 26.2 noise-fill Mixin signature

Argon's latest JFR capture showed two concrete leads, so this build adds diagnostic-only instrumentation for:

- `GuiRenderState.hasIntersection`: total call and intersecting-call counts, candidate-list sizes, and rolling CPU wall time.
- `NoiseBasedChunkGenerator.doFill`: completed fill count, cumulative time, recent average/P50/P95/max, all-time maximum, and worker-thread call counts.

These probes do **not** change GUI intersections, world-generation math, chunk scheduling, rendering, or game state. Their config switches default to false. They collect data only when local telemetry is also enabled.

## Why 0.1.5 exists

Argon 0.1.4 could crash during Minecraft bootstrap because its noise-fill Mixin handler used an outdated parameter list. Minecraft 26.2's `NoiseBasedChunkGenerator.doFill` requires `Blender`, `StructureManager`, `RandomState`, `ChunkAccess`, two integers, and a `CallbackInfoReturnable`. Version 0.1.5 corrects both HEAD and RETURN hooks and strengthens the build-time signature check. The prior 0.1.4 JAR should not be used.

## Enable the probes

Close Minecraft and edit `config/argon.properties`:

```properties
telemetry.enabled=true
performance.gui-intersection.enabled=true
chunks.generation-profiling.enabled=true
chunks.cancelled-task-cleanup.enabled=false
renderer.experimental.enabled=false
```

Restart into a disposable world. Travel through newly generated terrain for a few minutes, ideally including the village where you previously saw severe tick spikes, then run `/argon status`. The GUI and noise-fill metrics have independent counts and timing windows, so one can be busy while the other is idle.

For a cleaner isolation run, first test newly generated terrain with both probes enabled; then disable one profiling setting and restart to observe the remaining path with less instrumentation overhead. These are CPU wall-time diagnostics, not direct measurements of full chunk-ready latency or village AI alone.

## Important limits

- `NoiseBasedChunkGenerator.doFill` is a central terrain-density/fill stage, not all chunk generation, decoration, lighting, biome work, disk I/O, or queue waiting.
- The GUI candidate-list size is the size of the supplied list, not the exact number of list entries the vanilla loop visits before returning.
- A probe being expensive does not by itself prove that replacing or caching it is safe. Preserve vanilla's output semantics and confirm improvements with repeatable A/B testing.
- The experimental renderer remains unavailable; this build does not replace the renderer or claim performance gains.
- A successful CI build proves compilation and unit tests, not an in-game startup or performance result.
