# Argon 0.1.5 — CPU hotspot profiling test plan

## Install safely

1. Use a separate Minecraft 26.2 Fabric instance with Java 25, Fabric Loader 0.19.5+, and Fabric API 0.161.0+26.2.
2. Back up important worlds and use a disposable test world.
3. Download only `argon-mc26.2-0.1.5.jar` from the latest successful workflow run on `support/minecraft-26.2`.
4. Put the JAR in the instance's `mods` directory.

## Enable diagnostic probes

Close Minecraft and update `config/argon.properties`:

```properties
telemetry.enabled=true
performance.gui-intersection.enabled=true
chunks.generation-profiling.enabled=true
chunks.cancelled-task-cleanup.enabled=false
renderer.experimental.enabled=false
```

Restart the client so the settings take effect.

## Test A — generation in unexplored terrain

Enter a fresh or previously unexplored area and travel for 2–3 minutes. Reproduce the usual chunk-generation bursts, then run:

```text
/argon status
```

Record:
- Completed noise fills and cumulative CPU wall time.
- Noise-fill average/P50/P95/max and the all-time maximum.
- Worker-thread call counts.
- Queue-wait P95 and mesh-compile P95 for context.
- Whether terrain arrives in bursts or steadily.

The probe measures only `NoiseBasedChunkGenerator.doFill`; it does not capture the whole chunk-status pipeline, biome selection, decoration, lighting, storage, or time waiting for other stages.

## Test B — GUI intersection work and village load

Travel to the village where you saw severe tick spikes, let the area settle briefly, then run `/argon status`. Record:
- GUI intersection call / hit counts.
- Candidate-list total and maximum size.
- Candidate list rolling average/P50/P95/max.
- `hasIntersection` rolling CPU time average/P50/P95/max.
- Integrated-server tick-work P95/max and world-pass interval P95.

GUI measurements can include HUD and screen extraction from the entire session. The candidate-list size records the list supplied to vanilla, not the exact number of list entries visited before a method returns.

## Isolate overhead

Both probes are disabled by default. For final performance comparisons use a separate run with both profiling settings disabled (or `telemetry.enabled=false`). Do not use an instrumented run as a clean FPS benchmark. If the numbers show a clear hotspot, repeat with only that probe enabled to check that it remains prominent.

If the game crashes or reports a Mixin error, save `logs/latest.log` and disable the relevant setting before trying again. Do not use a valuable world for first tests.
