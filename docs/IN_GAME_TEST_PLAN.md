# Argon 0.1 — first in-game smoke test

This build is intended to verify startup compatibility and gather an initial baseline. It is not a performance-release claim.

## Before installing

1. Use a separate Minecraft 26.3 Fabric instance with Java 25, Fabric Loader 0.19.5, and Fabric API 0.162.0+26.3.
2. Back up any world you care about. Prefer a temporary test world for the first launch.
3. Download the `argon-0.1.0` artifact from the latest successful Argon CI run. Use the JAR inside the downloaded artifact ZIP.
4. Close Minecraft before adding the JAR to that instance's `mods` folder. Avoid mixing this first test with other optimization mods where possible.

## Test A — default settings

Do not create or edit the config before the first run. Argon creates `config/argon.properties` with safe defaults:

```properties
telemetry.enabled=true
chunks.scheduler.enabled=false
renderer.experimental.enabled=false
chunks.queue.capacity=256
performance.frame-window=240
chunks.cancelled-task-cleanup.enabled=false
```

Launch Minecraft and check that it reaches the title screen and can enter a test world. In the world, run:

```text
/argon status
```

Confirm the status command appears and the world-pass sample count grows while the world is rendering. The cancelled-task cleanup feature should show as disabled. The first launch also verifies that the required 26.3-specific Mixin can apply without a startup error.

## Test B — opt-in queue cleanup

Only after Test A is stable, close Minecraft and back up the generated config. Change just this setting:

```properties
chunks.cancelled-task-cleanup.enabled=true
```

Restart the client and check `/argon status` says `CANCELLED_CHUNK_TASK_CLEANUP: ACTIVE`. In a disposable test world, move quickly across chunk boundaries, rotate the camera through dense terrain, enter/leave the Nether if practical, and revisit areas that trigger chunk rebuilds. Watch for crashes, missing chunks, visual corruption, severe stutters, or console errors.

If anything unusual occurs, close the game and set the option back to `false` before another launch. This setting is deliberately opt-in because the hook has passed CI but has not yet been validated in a real client.

## What to report back

Please share whether each test reached the title screen and loaded a world; whether `/argon status` worked; any crash or Mixin error from `logs/latest.log`; and, for the opt-in test, whether you noticed a repeatable difference in chunk stutter. P50/P95 in the command are world-render-pass intervals, not GPU timings or a guaranteed FPS measurement. Try to compare the same view and movement route before interpreting a difference.

Do not test first on a valuable world or server you care about. If the client crashes, preserve `logs/latest.log` and any crash report before changing the configuration.
