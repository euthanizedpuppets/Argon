# Argon Minecraft 26.2 — first in-game smoke test

This branch is a 26.2 compatibility test candidate so it can be tried on the user's current graphics stack. It is not a performance-release claim.

## Before installing

1. Use a separate Minecraft 26.2 Fabric instance with Java 25, Fabric Loader 0.19.5, and Fabric API 0.161.0+26.2.
2. Back up any world you care about. Prefer a temporary test world for the first launch.
3. Open the successful CI run for the `support/minecraft-26.2` branch and download artifact `argon-mc26.2-0.1.0`.
4. Extract the ZIP and place only `argon-mc26.2-0.1.0.jar` in that instance's `mods` directory. Close Minecraft before adding it.
5. Start with no other performance mods where practical, so a crash or behavior change is easier to isolate.

## Test A — default settings

Do not create or edit the config before the first run. Argon creates `config/argon.properties` with safe defaults, including:

```properties
telemetry.enabled=true
chunks.scheduler.enabled=false
renderer.experimental.enabled=false
chunks.queue.capacity=256
performance.frame-window=240
chunks.cancelled-task-cleanup.enabled=false
```

Launch the game, reach the title screen, and enter a disposable world. Run:

```text
/argon status
```

Confirm the world-pass sample count grows while the world is rendering. In single-player, confirm the integrated server tick-work sample count also grows. The cleanup feature should report `CANCELLED_CHUNK_TASK_CLEANUP: DISABLED`. Use the game's own graphics setting to test the backend your system supports; Argon does not force OpenGL or Vulkan.

## Test B — opt-in queue cleanup

Only after Test A is stable, close Minecraft and back up the generated config. Change just this setting:

```properties
chunks.cancelled-task-cleanup.enabled=true
```

Restart and confirm `/argon status` says `CANCELLED_CHUNK_TASK_CLEANUP: ACTIVE`. In a disposable world, move quickly across chunk boundaries, rotate the camera through dense terrain, and revisit areas likely to trigger chunk rebuilds. Watch for startup crashes, missing chunks, visual corruption, severe stutters, or console errors.

If anything unusual occurs, close the game and set the option back to `false` before another launch. This setting is opt-in because it has passed CI but has not yet been validated in a real 26.2 client.

## What to report back

Share whether the title screen and world loaded, whether `/argon status` worked, whether the selected graphics backend starts, and any crash or Mixin errors from `logs/latest.log`. The integrated tick-work min/average/max and P50/P95 may help show whether single-player tick processing is itself slow. Those values do not measure remote multiplayer servers, wall-clock tick scheduling delay, or world-save time during shutdown.

For the cleanup test, describe repeatable changes in chunk stutter and any new log warnings. The P50/P95 render values measure intervals between world-render callbacks; they are not GPU timings or a guaranteed FPS measurement. Compare the same view and movement route before interpreting differences.

Do not use a valuable world or server for the first test. If saving or exiting hangs, preserve `logs/latest.log` and any crash report before force-closing so we can diagnose it.
