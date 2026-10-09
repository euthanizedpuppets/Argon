# Argon 0.2.0 Renderer Observer — in-game test plan

## Install safely

1. Use a separate Minecraft 26.2 Fabric instance with Java 25, Fabric Loader 0.19.5, and Fabric API 0.161.0+26.2.
2. Back up important worlds; use a disposable fresh world for this experiment.
3. Download only argon-mc26.2-0.2.0-renderer-prototype.jar from the successful GitHub Actions artifact for branch experiment/renderer-prototype.
4. Put the JAR in the test instance's mods folder.

## Enable observer

Close the game and update config/argon.properties:

    telemetry.enabled=true
    renderer.experimental.enabled=true
    chunks.cancelled-task-cleanup.enabled=false

Restart, enter a world, move around for 2–3 minutes, and run /argon status. Confirm the new terrain observer section shows nonzero opaque/translucent group calls and timings. If calls stay at zero, capture logs/latest.log and report the result.

## Interpret safely

This stage times vanilla's existing ChunkSectionsToRender.renderGroup calls. It still uses vanilla for the actual draw and is not an alternate renderer. Timings include CPU work performed inside that method, but they are not GPU completion times or full-frame times. Observed draw group and entry counters are per-call accumulated totals, not unique chunk counts.

For final FPS comparison, use a separate run with telemetry.enabled=false, because any profiler adds overhead. This initial milestone is meant to show whether CPU terrain submission is a worthwhile next target, not to claim that Argon renders faster.

If the screen goes black, the game crashes, or shutdown hangs, save logs/latest.log before trying again and disable renderer.experimental.enabled.
